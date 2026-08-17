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
	 * Gets a set of all currently registered provider names.
	 *
	 * @return An unmodifiable map view of all registered providers.
	 */
	public Map<String, LlmProvider> getAllProviders() {
		return new HashMap<>(providers);
	}
}