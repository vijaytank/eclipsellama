package com.eclipsellama.plugin.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * MCP {@link McpConnection} for the SSE transport.
 * <p>
 * MCP SSE transport uses two separate HTTP channels:
 * <ul>
 * <li><b>Read channel</b>: a long-lived {@code GET} request to the SSE endpoint
 * that emits {@code data: <json>} events (RFC 8895 format).</li>
 * <li><b>Write channel</b>: a fresh {@code POST} request per outgoing message;
 * the POST response body is ignored — replies arrive via the GET stream.</li>
 * </ul>
 * </p>
 * <p>
 * The previous implementation used a single POST and binary length-prefix
 * framing, which caused {@code NumberFormatException} on any real SSE server.
 * </p>
 */
public class SseMcpConnection implements McpConnection, AutoCloseable {

	private final URL serverUrl;
	/** Normalised bearer token, {@code null} when not configured. */
	private final String bearerToken;

	/** The GET SSE read stream; opened lazily on first {@link #readMessage()}. */
	private HttpURLConnection sseConnection;
	private BufferedReader sseReader;

	/**
	 * Constructs an SSE MCP connection. The GET/SSE stream is opened lazily on the
	 * first {@link #readMessage()} call so construction is always non-blocking.
	 *
	 * @param serverUrl   the SSE endpoint URL; must not be {@code null}.
	 * @param bearerToken optional bearer token; {@code null} or blank = no auth.
	 */
	public SseMcpConnection(URL serverUrl, String bearerToken) {
		this.serverUrl = java.util.Objects.requireNonNull(serverUrl, "Server URL cannot be null");
		this.bearerToken = (bearerToken != null && !bearerToken.isBlank()) ? bearerToken.trim() : null;
	}

	/**
	 * Convenience constructor with no authentication.
	 *
	 * @param serverUrl the SSE endpoint URL.
	 * @throws IOException declared for API compatibility; not thrown here.
	 */
	public SseMcpConnection(URL serverUrl) throws IOException {
		this(serverUrl, null);
	}

	/**
	 * Reads the next JSON-RPC message from the SSE stream.
	 * <p>
	 * SSE framing: lines starting with {@code data:} carry the payload; a blank
	 * line terminates one event. Lines starting with {@code :} are comments (skip).
	 * Other prefixes ({@code event:}, {@code id:}, {@code retry:}) are also
	 * skipped.
	 * </p>
	 * <p>
	 * Opens the GET SSE connection on first call (lazy init).
	 * </p>
	 *
	 * @return the JSON payload string, or {@code null} at end-of-stream.
	 * @throws IOException if the SSE connection cannot be opened or I/O fails.
	 */
	@Override
	public String readMessage() throws IOException {
		if (sseReader == null) {
			openSseStream();
		}
		StringBuilder eventData = new StringBuilder();
		String line;
		while ((line = sseReader.readLine()) != null) {
			if (line.startsWith("data:")) {
				String data = line.substring(5).trim();
				if (!data.isEmpty()) {
					if (eventData.length() > 0) {
						eventData.append('\n');
					}
					eventData.append(data);
				}
			} else if (line.isEmpty()) {
				// A blank line marks the end of one SSE event.
				if (eventData.length() > 0) {
					return eventData.toString();
				}
				// Empty event (double blank or keepalive blank) — keep reading.
			}
			// All other prefixes (event:, id:, retry:, :comment) are silently ignored.
		}
		return null; // end-of-stream
	}

	/**
	 * Sends a JSON-RPC message to the server via a fresh HTTP POST.
	 * <p>
	 * Each call opens and immediately closes its own connection; the read-only SSE
	 * GET stream is untouched. This prevents socket leaks and allows the server to
	 * correlate the request with the ongoing SSE session via cookies or session
	 * headers if required.
	 * </p>
	 *
	 * @param message the JSON-RPC message to POST.
	 * @throws IOException if the POST fails, times out, or returns an error code.
	 */
	@Override
	public void writeMessage(String message) throws IOException {
		HttpURLConnection postConn = (HttpURLConnection) serverUrl.openConnection();
		try {
			postConn.setRequestMethod("POST");
			postConn.setDoOutput(true);
			postConn.setRequestProperty("Content-Type", "application/json");
			if (bearerToken != null) {
				postConn.setRequestProperty("Authorization", "Bearer " + bearerToken);
			}
			try (OutputStream os = postConn.getOutputStream()) {
				os.write(message.getBytes(StandardCharsets.UTF_8));
			}
			int status = postConn.getResponseCode();
			if (status == 401 || status == 403) {
				throw new IOException("Authentication required or invalid token (HTTP " + status + ")."
						+ " Provide a valid bearer token for this MCP server.");
			}
			if (status >= 400) {
				throw new IOException("MCP server rejected POST with HTTP " + status + ".");
			}
			// 2xx — success; the actual reply will arrive via the SSE stream.
		} finally {
			// Always release the socket, regardless of success or error.
			postConn.disconnect();
		}
	}

	/**
	 * Closes the SSE read stream and the underlying HTTP connection. Idempotent:
	 * safe to call multiple times.
	 */
	@Override
	public void close() throws IOException {
		try {
			if (sseReader != null) {
				sseReader.close();
			}
		} finally {
			if (sseConnection != null) {
				sseConnection.disconnect();
			}
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Opens the long-lived GET SSE connection and initialises {@link #sseReader}.
	 * Validates authentication status before returning.
	 */
	private void openSseStream() throws IOException {
		sseConnection = (HttpURLConnection) serverUrl.openConnection();
		sseConnection.setRequestMethod("GET");
		sseConnection.setRequestProperty("Accept", "text/event-stream");
		sseConnection.setRequestProperty("Cache-Control", "no-cache");
		if (bearerToken != null) {
			sseConnection.setRequestProperty("Authorization", "Bearer " + bearerToken);
		}
		// getResponseCode() triggers the actual HTTP handshake.
		int status = sseConnection.getResponseCode();
		if (status == 401 || status == 403) {
			sseConnection.disconnect();
			throw new IOException("Authentication required or invalid token (HTTP " + status
					+ "). Provide a valid bearer token for this MCP server.");
		}
		if (status >= 400) {
			sseConnection.disconnect();
			throw new IOException("SSE endpoint returned HTTP " + status + ".");
		}
		sseReader = new BufferedReader(new InputStreamReader(sseConnection.getInputStream(), StandardCharsets.UTF_8));
	}
}