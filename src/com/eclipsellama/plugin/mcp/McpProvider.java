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
		// All characters that must be escaped in a JSON string value (RFC 8259 §7).
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
				.replace("\t", "\\t").replace("\b", "\\b").replace("\f", "\\f");
	}

	@Override
	public String getProviderName() {
		if (config.getName() != null && !config.getName().isBlank()) {
			return config.getName();
		}
		return "MCP (" + config.getTransport() + ":" + config.getId() + ")";
	}

	/**
	 * @return the underlying server configuration (transport, endpoint, discovered
	 *         tools).
	 */
	public McpServerConfig getConfig() {
		return config;
	}

	/**
	 * Live status snapshot for a configured MCP server.
	 */
	public static final class ProbeResult {
		public final boolean connected;
		public final String detail;
		public final java.util.List<String> tools;

		ProbeResult(boolean connected, String detail, java.util.List<String> tools) {
			this.connected = connected;
			this.detail = detail;
			this.tools = tools == null ? new java.util.ArrayList<>() : tools;
		}
	}

	/**
	 * Probes the server for real, current status: opens (or reuses) the connection,
	 * performs the MCP {@code initialize} handshake, and discovers tools via
	 * {@code tools/list}. This reflects actual reachability. If probe fails, falls
	 * back to configured tools. Never throws.
	 */
	public ProbeResult probeStatus() {
		try {
			McpConnection conn = connectionManager.getOrCreate(config);
			// MCP requires initialize as the first request; it binds the session.
			String initReq = "{\"jsonrpc\":\"" + RPC_VERSION + "\",\"id\":\"" + UUID.randomUUID()
					+ "\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},"
					+ "\"clientInfo\":{\"name\":\"eclipsellama\",\"version\":\"1.0.0\"}}}";
			conn.writeMessage(initReq);
			String initResp = conn.readMessage();
			if (initResp == null) {
				return new ProbeResult(false, "connection closed before initialize response", config.getTools());
			}
			String initErr = extractError(initResp);
			if (initErr != null) {
				return new ProbeResult(false, initErr, config.getTools());
			}
			// Fire-and-forget initialized notification.
			conn.writeMessage("{\"jsonrpc\":\"" + RPC_VERSION + "\",\"method\":\"notifications/initialized\"}");

			// Discover tools.
			String toolsReq = "{\"jsonrpc\":\"" + RPC_VERSION + "\",\"id\":\"" + UUID.randomUUID()
					+ "\",\"method\":\"tools/list\"}";
			conn.writeMessage(toolsReq);
			String toolsResp = conn.readMessage();
			java.util.List<String> tools = toolsResp == null ? config.getTools() : parseToolNames(toolsResp);
			if (!tools.isEmpty()) {
				config.setTools(tools);
			}
			return new ProbeResult(true, "connected", config.getTools());
		} catch (Exception e) {
			connectionManager.close(config.getId());
			String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
			return new ProbeResult(false, msg, config.getTools());
		}
	}

	private String extractError(String response) {
		int err = response.indexOf("\"error\"");
		if (err < 0) {
			return null;
		}
		int msg = response.indexOf("\"message\"", err);
		if (msg < 0) {
			return "server returned an error";
		}
		int start = response.indexOf('"', msg + "\"message\":".length());
		if (start < 0) {
			return "server returned an error";
		}
		int end = response.indexOf('"', start + 1);
		if (end < 0) {
			return "server returned an error";
		}
		return response.substring(start + 1, end);
	}

	private java.util.List<String> parseToolNames(String response) {
		java.util.List<String> names = new java.util.ArrayList<>();
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(response);
		while (m.find()) {
			names.add(m.group(1));
		}
		return names;
	}

	/**
	 * Closes the underlying MCP connection if open.
	 */
	public void close() {
		connectionManager.close(config.getId());
	}
}