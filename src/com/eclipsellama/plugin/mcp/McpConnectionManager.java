package com.eclipsellama.plugin.mcp;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages the lifecycle of open {@link McpConnection}s keyed by server
 * configuration id. Connections are created on demand and closed on demand or
 * when the plugin shuts down.
 */
public class McpConnectionManager {

	private static final McpConnectionManager INSTANCE = new McpConnectionManager();

	private final ConcurrentMap<String, McpConnection> connections = new ConcurrentHashMap<>();

	/**
	 * Protected constructor allows subclasses and test doubles to be created
	 * directly with {@code new McpConnectionManager()} while keeping the singleton
	 * pattern intact for production use via {@link #getInstance()}.
	 */
	protected McpConnectionManager() {
	}

	/** Returns the process-wide shared connection manager. */
	public static McpConnectionManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Returns the open connection for the given config, creating one if absent.
	 *
	 * @param config the server configuration.
	 * @return the open {@link McpConnection}.
	 * @throws IOException if the connection cannot be established.
	 */
	public McpConnection getOrCreate(McpServerConfig config) throws IOException {
		McpConnection existing = connections.get(config.getId());
		if (existing != null) {
			return existing;
		}
		McpConnection created = create(config);
		McpConnection raced = connections.putIfAbsent(config.getId(), created);
		if (raced != null) {
			try {
				created.close();
			} catch (IOException e) {
				System.err.println("McpConnectionManager: error closing duplicate connection: " + e.getMessage());
			}
			return raced;
		}
		return created;
	}

	private McpConnection create(McpServerConfig config) throws IOException {
		if (config.getTransport() == null) {
			throw new IOException("Transport mode must be set.");
		}
		if ("stdio".equals(config.getTransport())) {
			return new StdioMcpConnection(commandLine(config));
		} else if ("sse".equals(config.getTransport())) {
			if (config.getEndpoint() == null || config.getEndpoint().isEmpty()) {
				throw new IOException("Endpoint must be provided for " + config.getTransport() + " transport.");
			}
			return new SseMcpConnection(java.net.URI.create(config.getEndpoint()).toURL(), config.getBearerToken());
		} else if ("http-streamable".equals(config.getTransport())) {
			if (config.getEndpoint() == null || config.getEndpoint().isEmpty()) {
				throw new IOException("Endpoint must be provided for " + config.getTransport() + " transport.");
			}
			return new StreamableHttpMcpConnection(java.net.URI.create(config.getEndpoint()).toURL(),
					config.getBearerToken());
		}
		throw new IOException("Unsupported transport: " + config.getTransport());
	}

	private String[] commandLine(McpServerConfig config) {
		if (config.getCommand() == null || config.getCommand().trim().isEmpty()) {
			throw new IllegalArgumentException("For stdio transport, a command must be provided.");
		}
		String args = config.getArguments();
		if (args == null || args.trim().isEmpty()) {
			return new String[] { config.getCommand().trim() };
		}
		String[] parts = args.trim().split("\\s+");
		String[] cmd = new String[parts.length + 1];
		cmd[0] = config.getCommand().trim();
		System.arraycopy(parts, 0, cmd, 1, parts.length);
		return cmd;
	}

	/**
	 * Closes and removes the connection for the given config id.
	 */
	public void close(String configId) {
		McpConnection conn = connections.remove(configId);
		if (conn != null) {
			try {
				conn.close();
			} catch (IOException e) {
				System.err
						.println("McpConnectionManager: error closing connection " + configId + ": " + e.getMessage());
			}
		}
	}

	/**
	 * Closes and clears all managed connections.
	 */
	public void closeAll() {
		for (String id : connections.keySet()) {
			close(id);
		}
	}

	/**
	 * Returns the number of currently open connections.
	 */
	public int size() {
		return connections.size();
	}

	/**
	 * Returns true if a live connection is currently held for the given config id.
	 */
	public boolean isConnected(String configId) {
		return connections.containsKey(configId);
	}
}
