package com.eclipsellama.plugin.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class StdioMcpConnection implements McpConnection, AutoCloseable {

	/** Shutdown hook thread, held to allow removal if close() is called cleanly. */
	private final Thread shutdownHook;

	private final Process process;
	private final BufferedReader reader;
	private final PrintWriter writer;

	/**
	 * Constructs a new STDIO MCP connection for the given command. The command must
	 * be an executable that follows the MCP STDIO protocol.
	 *
	 * @param command The command line to launch the MCP server (e.g., ["npx", "-y",
	 *                "@modelcontextprotocol/server-standard-input"])
	 * @throws IOException if the process cannot be started
	 */
	public StdioMcpConnection(String... command) throws IOException {
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectErrorStream(true);
		this.process = builder.start();

		// Set up streams for reading and writing JSON messages
		this.reader = new BufferedReader(new InputStreamReader(this.process.getInputStream()));
		this.writer = new PrintWriter(new OutputStreamWriter(this.process.getOutputStream()));

		// Ensure the writer flushes content immediately
		this.writer.flush();

		// Register shutdown hook to forcibly kill the subprocess if the JVM exits
		// abnormally (e.g., Eclipse crash). Prevents orphaned MCP server processes.
		this.shutdownHook = new Thread(() -> process.destroyForcibly(), "eclipsellama-mcp-shutdown-" + process.pid());
		Runtime.getRuntime().addShutdownHook(this.shutdownHook);
	}

	/**
	 * Reads the next JSON-RPC message from the STDIO stream. MCP stdio servers use
	 * newline-delimited JSON: each message is a single line of JSON. Servers may
	 * also emit log lines on stdout (e.g. "2026-08-18 ... INFO ..."), which are
	 * skipped until a line that parses as a JSON object is found.
	 *
	 * @return The received message as a {@link String}
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public String readMessage() throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			String trimmed = line.trim();
			// Skip blank lines and non-JSON (log) output. A JSON-RPC message always
			// starts with '{' once trimmed.
			if (trimmed.isEmpty() || !trimmed.startsWith("{")) {
				continue;
			}
			return trimmed;
		}
		return null; // End of stream
	}

	/**
	 * Writes a message to the STDIO stream as a single newline-terminated line of
	 * JSON (MCP stdio framing).
	 *
	 * @param message The message to write
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public void writeMessage(String message) throws IOException {
		writer.write(message);
		writer.write('\n');
		writer.flush();
	}

	@Override
	public void close() throws IOException {
		// Remove shutdown hook first to prevent it firing redundantly after a clean
		// close.
		try {
			Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
		} catch (IllegalStateException ignored) {
			// JVM is already shutting down; hook removal is not possible.
		}
		// Close streams before destroying process to flush any pending data.
		try {
			writer.close();
		} catch (Exception ignored) {
		}
		try {
			reader.close();
		} catch (Exception ignored) {
		}
		process.destroy();
	}
}