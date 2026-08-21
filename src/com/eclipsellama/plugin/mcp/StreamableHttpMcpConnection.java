package com.eclipsellama.plugin.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;
import com.eclipsellama.plugin.security.TlsPolicy;

/**
 * MCP {@link McpConnection} for the modern "Streamable HTTP" transport.
 * <p>
 * Unlike the legacy SSE transport, Streamable HTTP uses standard HTTP POST/GET
 * round-trips carrying a single JSON-RPC message per request. The server
 * negotiates content with an {@code Accept: application/json,
 * text/event-stream} header and issues a {@code Mcp-Session-Id} header that
 * must be echoed on subsequent requests.
 * <p>
 * The {@code writeMessage}/{@code readMessage} pair models one HTTP exchange:
 * {@link #writeMessage} performs the POST and buffers the parsed response, and
 * {@link #readMessage} returns that buffered response.
 */
public class StreamableHttpMcpConnection implements McpConnection, AutoCloseable {

	private final URL endpoint;
	private final String bearerToken;
	private String sessionId;
	private String bufferedResponse;

	/**
	 * Constructs a new Streamable HTTP MCP connection for the given endpoint.
	 *
	 * @param endpoint    the MCP Streamable HTTP endpoint URL.
	 * @param bearerToken optional bearer token; may be null or blank.
	 */
	public StreamableHttpMcpConnection(URL endpoint, String bearerToken) {
		this.endpoint = java.util.Objects.requireNonNull(endpoint, "Endpoint cannot be null");
		this.bearerToken = bearerToken;
		enforceTlsIfEnabled();
	}

	/**
	 * Sends the given JSON-RPC message to the server over an HTTP POST, buffering
	 * the server's response for the next {@link #readMessage()} call.
	 */
	@Override
	public void writeMessage(String message) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Accept", "application/json, text/event-stream");
		if (bearerToken != null && !bearerToken.isBlank()) {
			conn.setRequestProperty("Authorization", "Bearer " + bearerToken.trim());
		}
		if (sessionId != null) {
			conn.setRequestProperty("Mcp-Session-Id", sessionId);
		}

		int status;
		try {
			try (OutputStream os = conn.getOutputStream()) {
				os.write(message.getBytes(StandardCharsets.UTF_8));
				os.flush();
			}
			status = conn.getResponseCode();
			if (status == 401 || status == 403) {
				throw new IOException("Authentication required or invalid token (HTTP " + status
						+ "). Provide a valid bearer token for this MCP server.");
			}

			String newSessionId = conn.getHeaderField("Mcp-Session-Id");
			if (newSessionId != null && !newSessionId.isBlank()) {
				this.sessionId = newSessionId;
			}

			InputStream body = (status >= 400) ? conn.getErrorStream() : conn.getInputStream();
			this.bufferedResponse = readBody(body, conn.getContentType());
			// Some servers embed the session id in the response JSON rather than (or in
			// addition to) the header. Fall back to that if no header was captured.
			if ((this.sessionId == null || this.sessionId.isBlank()) && this.bufferedResponse != null) {
				String fromBody = extractSessionId(this.bufferedResponse);
				if (fromBody != null) {
					this.sessionId = fromBody;
				}
			}
		} finally {
			conn.disconnect();
		}
	}

	/**
	 * Returns the response buffered by the most recent {@link #writeMessage} call,
	 * then clears it.
	 */
	@Override
	public String readMessage() throws IOException {
		String response = this.bufferedResponse;
		this.bufferedResponse = null;
		return response;
	}

	/**
	 * Reads the HTTP body, parsing SSE {@code data:} lines when the content type is
	 * {@code text/event-stream}, otherwise reading the raw body text.
	 */
	private String readBody(InputStream body, String contentType) throws IOException {
		if (body == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
			boolean sse = contentType != null && contentType.startsWith("text/event-stream");
			String line;
			while ((line = reader.readLine()) != null) {
				if (sse) {
					if (line.startsWith("data:")) {
						String data = line.substring(5).trim();
						if (!data.isEmpty()) {
							if (sb.length() > 0) {
								sb.append('\n');
							}
							sb.append(data);
						}
					}
				} else {
					sb.append(line);
				}
			}
		}
		return sb.toString();
	}

	@Override
	public void close() throws IOException {
		// Nothing to release beyond the per-request connections, which are closed in
		// writeMessage. Kept for AutoCloseable symmetry.
	}

	/**
	 * Rejects the configured endpoint when the TLS-enforcement preference is on and
	 * the endpoint is a non-local plain-HTTP URL. Throws
	 * {@link IllegalArgumentException} so the failure is loud and immediate at
	 * construction time.
	 */
	private void enforceTlsIfEnabled() {
		if (EclipseLlamaPreferences.getTlsEnforce() && !TlsPolicy.isPermitted(endpoint, true)) {
			throw new IllegalArgumentException("TLS enforcement is enabled and endpoint is not remote-safe ("
					+ endpoint.toExternalForm() + "). Use an https:// URL or add a localhost exception.");
		}
	}

	/**
	 * Returns the Mcp-Session-Id currently bound to this connection, or
	 * {@code null} if the server has not issued one yet.
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * Best-effort extraction of a session id from a JSON-RPC response body. Looks
	 * for {@code "sessionId"} / {@code "Mcp-Session-Id"} keys in the payload.
	 * Returns {@code null} when nothing sensible is found.
	 */
	private String extractSessionId(String body) {
		String value = matchJsonField(body, "sessionId");
		if (value == null) {
			value = matchJsonField(body, "Mcp-Session-Id");
		}
		return value;
	}

	private String matchJsonField(String body, String field) {
		int idx = body.indexOf("\"" + field + "\"");
		if (idx < 0) {
			return null;
		}
		int colon = body.indexOf(':', idx);
		if (colon < 0) {
			return null;
		}
		int start = colon + 1;
		while (start < body.length() && Character.isWhitespace(body.charAt(start))) {
			start++;
		}
		if (start < body.length() && body.charAt(start) == '\"') {
			int end = body.indexOf('\"', start + 1);
			if (end > start) {
				String v = body.substring(start + 1, end);
				return v.isBlank() ? null : v;
			}
		}
		return null;
	}
}
