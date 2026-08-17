package com.eclipsellama.plugin.mcp;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.eclipsellama.plugin.api.LlmException;
import com.eclipsellama.plugin.api.LlmProvider;

/**
 * MCP Provider implementation of {@link LlmProvider} that routes LLM calls
 * through a configured MCP server. It sends a real JSON-RPC {@code tools/call}
 * request over the underlying {@link McpConnection} and returns the server's
 * actual response.
 */
public class McpProvider implements LlmProvider {

	private static final String RPC_VERSION = "2.0";
	private final McpServerConfig config;
	private final McpConnectionManager connectionManager;

	/**
	 * Constructs a new McpProvider for the given server configuration using a
	 * shared connection manager.
	 *
	 * @param config the MCP server configuration.
	 */
	public McpProvider(McpServerConfig config) {
		this(config, McpConnectionManager.getInstance());
	}

	/**
	 * Constructs a new McpProvider with an explicit connection manager (for tests).
	 */
	public McpProvider(McpServerConfig config, McpConnectionManager connectionManager) {
		this.config = config;
		this.connectionManager = connectionManager;
	}

	@Override
	public CompletableFuture<String> generate(String prompt, String contextCode) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return callTool("prompt", prompt);
			} catch (LlmException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public Stream<String> stream(String prompt, String contextCode) {
		try {
			return Stream.of(callTool("prompt", prompt));
		} catch (LlmException e) {
			return Stream.of("Error: " + e.getMessage());
		}
	}

	/**
	 * Sends a JSON-RPC {@code tools/call} request to the MCP server and returns the
	 * server's raw response.
	 */
	private String callTool(String toolName, String argument) throws LlmException {
		McpConnection conn;
		try {
			conn = connectionManager.getOrCreate(config);
		} catch (IOException e) {
			throw new LlmException("Failed to establish MCP connection for " + config.getTransport(), e);
		}

		String request = "{\"jsonrpc\":\"" + RPC_VERSION + "\",\"id\":\"" + UUID.randomUUID()
				+ "\",\"method\":\"tools/call\",\"params\":{\"name\":\"" + toolName + "\",\"arguments\":{\"prompt\":\""
				+ escape(argument) + "\"}}}";
		try {
			conn.writeMessage(request);
			String response = conn.readMessage();
			if (response == null) {
				throw new LlmException("MCP server closed the connection before responding.");
			}
			return response;
		} catch (IOException e) {
			throw new LlmException("Error communicating with MCP server", e);
		}
	}

	private String escape(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	@Override
	public String getProviderName() {
		return "MCP (" + config.getTransport() + ")";
	}

	/**
	 * Closes the underlying MCP connection if open.
	 */
	public void close() {
		connectionManager.close(config.getId());
	}
}