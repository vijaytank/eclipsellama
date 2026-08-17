package com.eclipsellama.plugin.mcp;

import java.io.Serializable;
import java.util.UUID;

/**
 * Configuration details for an MCP server.
 */
public class McpServerConfig implements Serializable {
	private static final long serialVersionUID = 1L;
	private String id = UUID.randomUUID().toString();

	/**
	 * Transport mode for the MCP server. Valid values are {@code "stdio"} and
	 * {@code "sse"} and {@code "http-streamable"}.
	 */
	private String transport;

	/**
	 * The endpoint URL when {@code transport} is {@code "sse"} or
	 * {@code "http-streamable"}. Must be a fully-qualified HTTPS/HTTP URL.
	 */
	private String endpoint;

	/**
	 * Optional bearer token for {@code sse} or {@code http-streamable} transports.
	 */
	private String bearerToken;

	/**
	 * Command to execute when {@code transport} is {@code "stdio"}. Ignored for
	 * other transports.
	 */
	private String command;

	/**
	 * Arguments to pass to the command when {@code transport} is {@code "stdio"}.
	 * Ignored for other transports.
	 */
	private String arguments;

	/**
	 * Names of tools advertised by the server, populated by a live
	 * {@code tools/list} query. Transient: not persisted, fetched on connection.
	 */
	private transient java.util.List<String> tools = new java.util.ArrayList<>();

	/**
	 * Creates a new configuration object with default values.
	 */
	public McpServerConfig() {
	}

	/**
	 * @return the unique identifier for this configuration.
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the transport mode for this configuration.
	 */
	public String getTransport() {
		return transport;
	}

	/**
	 * @param transport the transport mode to set; must be one of {@code "stdio"},
	 *                  {@code "sse"}, or {@code "http-streamable"}.
	 */
	public void setTransport(String transport) {
		this.transport = transport;
	}

	/**
	 * @return the endpoint URL for this configuration.
	 */
	public String getEndpoint() {
		return endpoint;
	}

	/**
	 * @param endpoint the endpoint URL to set.
	 */
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	/**
	 * @return the bearer token for this configuration.
	 */
	public String getBearerToken() {
		return bearerToken;
	}

	/**
	 * @param bearerToken the bearer token to set.
	 */
	public void setBearerToken(String bearerToken) {
		this.bearerToken = bearerToken;
	}

	/**
	 * @return the command for stdio transport.
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * @param command the command to set (only used when transport is "stdio").
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	/**
	 * @return the arguments for stdio transport.
	 */
	public String getArguments() {
		return arguments;
	}

	/**
	 * @param arguments the arguments to set (only used when transport is "stdio").
	 */
	public void setArguments(String arguments) {
		this.arguments = arguments;
	}

	/**
	 * @return the tool names discovered from the server (may be empty).
	 */
	public java.util.List<String> getTools() {
		return tools;
	}

	/**
	 * @param tools the tool names to associate with this server.
	 */
	public void setTools(java.util.List<String> tools) {
		this.tools = (tools == null) ? new java.util.ArrayList<>() : tools;
	}

	/**
	 * Validates the configuration.
	 * <p>
	 * The rules are:
	 * <ul>
	 * <li>Transport must be set.</li>
	 * <li>If transport is {@code "stdio"}, a non‑empty command is required.</li>
	 * <li>If transport is {@code "sse"} or {@code "http-streamable"}, an endpoint
	 * must be provided.</li>
	 * </ul>
	 *
	 * @throws IllegalStateException with a descriptive message if validation fails.
	 */
	public void validate() {
		if (transport == null) {
			throw new IllegalStateException("Transport mode must be set.");
		}
		if (transport.equals("stdio")) {
			if (command == null || command.trim().isEmpty()) {
				throw new IllegalStateException("For stdio transport, a command must be provided.");
			}
		} else if (transport.equals("sse") || transport.equals("http-streamable")) {
			if (endpoint == null || endpoint.trim().isEmpty()) {
				throw new IllegalStateException("Endpoint must be provided for " + transport + " transport.");
			}
		}
		// No further validation – callers can still decide to actually open a
		// connection.
	}
}