package com.eclipsellama.plugin.mcp;

import java.io.IOException;

/**
 * Represents a generic MCP connection that can read and write messages.
 * Implementations must provide methods to read a message, write a message, and
 * close the connection.
 */
public interface McpConnection {
	/**
	 * Reads the next message from the connection.
	 *
	 * @return The message as a string, or {@code null} if the stream ends.
	 * @throws IOException if an I/O error occurs.
	 */
	String readMessage() throws IOException;

	/**
	 * Writes a message to the connection.
	 *
	 * @param message The message to write.
	 * @throws IOException if an I/O error occurs.
	 */
	void writeMessage(String message) throws IOException;

	/**
	 * Closes the connection.
	 *
	 * @throws IOException if closing fails.
	 */
	void close() throws IOException;
}