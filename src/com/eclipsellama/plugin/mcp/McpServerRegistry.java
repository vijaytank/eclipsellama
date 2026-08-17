package com.eclipsellama.plugin.mcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * In-memory registry of configured {@link McpServerConfig} instances, keyed by
 * config id. The registry is populated from the preference-backed store at
 * startup and can be queried by the rest of the plugin.
 */
public class McpServerRegistry {

	private final List<McpServerConfig> servers = new ArrayList<>();

	/**
	 * Registers (or replaces, by id) a server configuration.
	 *
	 * @param config the configuration to register.
	 */
	public void register(McpServerConfig config) {
		if (config == null) {
			throw new IllegalArgumentException("config must not be null");
		}
		servers.removeIf(existing -> existing.getId().equals(config.getId()));
		servers.add(config);
	}

	/**
	 * Removes a server configuration by id.
	 *
	 * @param configId the id of the configuration to remove.
	 */
	public void unregister(String configId) {
		servers.removeIf(existing -> existing.getId().equals(configId));
	}

	/**
	 * Returns the configuration with the given id, if present.
	 */
	public Optional<McpServerConfig> get(String configId) {
		return servers.stream().filter(s -> s.getId().equals(configId)).findFirst();
	}

	/**
	 * Returns an unmodifiable view of all registered configurations.
	 */
	public List<McpServerConfig> getAll() {
		return Collections.unmodifiableList(new ArrayList<>(servers));
	}

	/**
	 * Returns the number of registered configurations.
	 */
	public int size() {
		return servers.size();
	}

	/**
	 * Removes all registered configurations.
	 */
	public void clear() {
		servers.clear();
	}
}
