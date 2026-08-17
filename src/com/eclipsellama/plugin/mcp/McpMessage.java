package com.eclipsellama.plugin.mcp;

/**
 * Represents a generic MCP message exchanged between the client and server.
 * This class captures the essential structure of messages defined by the Model
 * Context Protocol (MCP) specification.
 * <p>
 * MCP defines three primary message types that are reflected here:
 * <ul>
 * <li>{@code Request} – messages that request a computation or tool use.</li>
 * <li>{@code Response} – messages that return a result or streamed data.</li>
 * <li>{@code Notification} – asynchronous messages that do not expect a
 * reply.</li>
 * </ul>
 * </p>
 * <p>
 * In practice, the EclipseLlama plugin will use these message types to
 * communicate with MCP servers (e.g., file‑system, terminal, or custom
 * domain‑specific servers) while building context for LLM interactions.
 * </p>
 */
public abstract class McpMessage {

	/**
	 * Unique identifier for the message. This ID is used for correlating responses
	 * and notifications to their corresponding requests.
	 */
	private final String id;

	/**
	 * The JSON‑RPC method name that indicates the type of operation. Typical values
	 * include {@code "initialize"}, {@code "compute"}, or custom method names
	 * defined by specific MCP servers.
	 */
	private final String method;

	/**
	 * The payload associated with the message. The exact schema of this object
	 * depends on the chosen method and is typically defined by the server
	 * implementation.
	 */
	private final Object params;

	/**
	 * Constructor that initializes a new {@link McpMessage} with the given
	 * identifier, method, and parameters.
	 *
	 * @param id     A unique identifier for the message; must not be {@code null}
	 *               or empty.
	 * @param method The RPC method name that designates the operation to perform.
	 * @param params The parameters accompanying the method; may be {@code null} if
	 *               the method does not require additional data.
	 */
	protected McpMessage(String id, String method, Object params) {
		this.id = java.util.Objects.requireNonNull(id, "Message id cannot be null");
		this.method = java.util.Objects.requireNonNull(method, "Method cannot be null");
		this.params = params;
	}

	/**
	 * Retrieves the unique identifier of this message.
	 *
	 * @return The message ID as a {@code String}.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Retrieves the RPC method name associated with this message.
	 *
	 * @return The method name as a {@code String}.
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Retrieves the parameters encapsulated within this message.
	 *
	 * @return An {@code Object} representing the message's parameters.
	 */
	public Object getParams() {
		return params;
	}
}