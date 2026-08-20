package com.eclipsellama.plugin.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry responsible for collecting, storing, and providing access to all
 * initialized LlmProvider implementations. This acts as a centralized
 * dependency injection mechanism for the plugin core.
 */
public class LlmProviderRegistry {

	private static final LlmProviderRegistry INSTANCE = new LlmProviderRegistry();
	private final Map<String, LlmProvider> providers = new HashMap<>();
	private String activeProviderName;

	private LlmProviderRegistry() {
		// Private constructor to enforce Singleton pattern
	}

	/**
	 * Gets the singleton instance of the registry.
	 *
	 * @return The LlmProviderRegistry instance.
	 */
	public static LlmProviderRegistry getInstance() {
		return INSTANCE;
	}

	/**
	 * Registers a new LlmProvider implementation.
	 *
	 * @param provider The provider instance to register.
	 * @throws IllegalArgumentException if a provider with the same name is already
	 *                                  registered.
	 */
	public void registerProvider(LlmProvider provider) {
		String name = provider.getProviderName();
		if (providers.containsKey(name)) {
			throw new IllegalArgumentException("Provider already registered: " + name);
		}
		providers.put(name, provider);
		System.out.println("LlmProviderRegistry: Registered provider: " + name);
	}

	/**
	 * Retrieves an LlmProvider by its unique name.
	 *
	 * @param name The name of the provider.
	 * @return An Optional containing the provider, or empty if not found.
	 */
	public Optional<LlmProvider> getProvider(String name) {
		return Optional.ofNullable(providers.get(name));
	}

	/**
	 * Unregisters an LlmProvider by name, clearing the active provider if it
	 * matched.
	 *
	 * @param name The name of the provider to unregister.
	 */
	public synchronized void unregisterProvider(String name) {
		if (name != null) {
			providers.remove(name);
			if (name.equals(activeProviderName)) {
				activeProviderName = null;
			}
		}
	}

	/**
	 * Gets a set of all currently registered provider names.
	 *
	 * @return An unmodifiable map view of all registered providers.
	 */
	public Map<String, LlmProvider> getAllProviders() {
		return new HashMap<>(providers);
	}

	/**
	 * Sets the currently active provider by name. Falls back to the default
	 * (first-registered) provider on retrieval if no name has been set.
	 *
	 * @param name the name of the provider to make active, or {@code null} to clear
	 *             the selection.
	 */
	public void setActiveProvider(String name) {
		this.activeProviderName = name;
	}

	/**
	 * Resolves the active {@link LlmProvider}. If no explicit active provider was
	 * set, the first registered provider is used as the default.
	 *
	 * @return the active provider, or empty if none are registered.
	 */
	public Optional<LlmProvider> getActiveProvider() {
		if (activeProviderName != null && providers.containsKey(activeProviderName)) {
			return Optional.of(providers.get(activeProviderName));
		}
		return providers.values().stream().findFirst();
	}

	/**
	 * @return {@code true} if an explicit active provider has been set via
	 *         {@link #setActiveProvider(String)}.
	 */
	public boolean hasExplicitActiveProvider() {
		return activeProviderName != null && providers.containsKey(activeProviderName);
	}
}