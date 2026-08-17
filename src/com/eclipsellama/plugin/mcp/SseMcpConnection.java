package com.eclipsellama.plugin.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Input/Output handling for SSE based MCP servers. This class wraps a
 * Server-Sent Events (SSE) endpoint that follows the MCP specification for
 * streaming responses from a remote server.
 * <p>
 * Each incoming SSE event is parsed and exposed as a raw JSON message to the
 * caller. Outgoing messages are posted to the SSE endpoint via HTTP POST with a
 * JSON payload that conforms to the MCP message format.
 * </p>
 *
 * <p>
 * Future enhancements may include automatic reconnection logic, heartbeat
 * monitoring, and per‑event deserialization into typed DTOs.
 * </p>
 */
public class SseMcpConnection implements McpConnection, AutoCloseable {

	private final URL serverUrl;
	private final HttpURLConnection connection;
	private final BufferedReader reader;
	private final PrintWriter writer;
	private final ExecutorService executor;

	/**
	 * Constructs a new SSE MCP connection for the given server URL. The URL must
	 * point to an SSE endpoint that complies with the MCP message format
	 * (length-prefixed JSON messages). No authentication is used.
	 *
	 * @param serverUrl The absolute URL of the SSE MCP server.
	 * @throws IOException if the connection cannot be established.
	 */
	public SseMcpConnection(URL serverUrl) throws IOException {
		this(serverUrl, null);
	}

	/**
	 * Constructs a new SSE MCP connection for the given server URL and an optional
	 * bearer token. When a non-blank token is supplied an
	 * {@code Authorization: Bearer <token>} header is attached; otherwise no
	 * authentication header is sent. If the server responds 401/403 an
	 * authentication error is reported so the caller can prompt the user for a
	 * token.
	 *
	 * @param serverUrl   The absolute URL of the SSE MCP server.
	 * @param bearerToken Optional bearer token; may be null or blank.
	 * @throws IOException if the connection cannot be established or is
	 *                     unauthorized.
	 */
	public SseMcpConnection(URL serverUrl, String bearerToken) throws IOException {
		this.serverUrl = java.util.Objects.requireNonNull(serverUrl, "Server URL cannot be null");
		this.connection = (HttpURLConnection) this.serverUrl.openConnection();
		this.connection.setRequestMethod("POST");
		this.connection.setDoOutput(true);
		this.connection.setRequestProperty("Content-Type", "application/json");
		if (bearerToken != null && !bearerToken.isBlank()) {
			this.connection.setRequestProperty("Authorization", "Bearer " + bearerToken.trim());
		}

		// Fail fast on authentication / authorization failures so the caller can
		// inform the user that a token is required or invalid.
		int status = this.connection.getResponseCode();
		if (status == 401 || status == 403) {
			this.connection.disconnect();
			throw new IOException("Authentication required or invalid token (HTTP " + status
					+ "). Provide a valid bearer token for this MCP server.");
		}

		// Initialise streams for reading responses and writing requests
		this.reader = new BufferedReader(new InputStreamReader(this.connection.getInputStream()));
		this.writer = new PrintWriter(new OutputStreamWriter(this.connection.getOutputStream()));

		// Executor for background tasks such as reconnection retries
		this.executor = Executors.newSingleThreadExecutor();

		// Ensure the initial write flushes immediately
		this.writer.flush();
	}

	/**
	 * Reads the next MCP message from the SSE stream. This method blocks until a
	 * complete message is received.
	 *
	 * @return The received message as a JSON {@code String}.
	 * @throws IOException if an I/O error occurs or the stream ends.
	 */
	@Override
	public String readMessage() throws IOException {
		String lengthLine = reader.readLine();
		if (lengthLine == null) {
			return null; // End of stream
		}

		int length = Integer.parseInt(lengthLine.trim());
		char[] buffer = new char[length];
		int read = 0;
		while (read < length) {
			int r = reader.read(buffer, read, length - read);
			if (r == -1) {
				throw new IOException("Unexpected end of stream");
			}
			read += r;
		}

		return new String(buffer);
	}

	/**
	 * Writes a message to the SSE endpoint.
	 *
	 * @param message The message to send.
	 * @throws IOException if an I/O error occurs during the POST.
	 */
	@Override
	public void writeMessage(String message) throws IOException {
		writer.write(message);
		writer.flush();
	}

	/**
	 * Closes the underlying HTTP connection and shuts down the executor.
	 *
	 * @throws IOException if closing the connection fails.
	 */
	@Override
	public void close() throws IOException {
		connection.disconnect();
		reader.close();
		writer.close();
		executor.shutdownNow();
	}
}