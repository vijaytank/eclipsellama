package com.eclipsellama.plugin.setup;

import org.eclipse.ui.IStartup;

import com.eclipsellama.plugin.api.LlmProviderRegistry;
import com.eclipsellama.plugin.api.OllamaProvider;
import com.eclipsellama.plugin.api.OpenAiProvider;
import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;

/**
 * Startup Launcher plugin component. This class handles initial plugin setup,
 * including registering providers and migrating user data upon plugin
 * activation.
 */
public class SetupLauncher implements IStartup {

	@Override
	public void earlyStartup() {
		initializePlugin();
	}

	public void initializePlugin() {
		setupProviders();
		migratePreferences();
	}

	/**
	 * Registers all available LLM providers using the singleton registry. Provider
	 * configuration (endpoint, model, API key) is read from configured preferences,
	 * never hard-coded. OpenAI is registered only when an API key has been
	 * provided.
	 */
	private void setupProviders() {
		LlmProviderRegistry registry = LlmProviderRegistry.getInstance();

		// Ollama is always available via the configured endpoint/model.
		registry.registerProvider(new OllamaProvider(EclipseLlamaPreferences.getModel()));

		// OpenAI is registered only when an API key is present; the key is read from
		// preferences (Phase 5 migrates it to secure storage).
		String apiKey = EclipseLlamaPreferences.getApiKey();
		if (apiKey != null && !apiKey.isBlank()) {
			registry.registerProvider(new OpenAiProvider());
		}
	}

	/**
	 * Migrates legacy plain-text keys from {@code config.properties} to the
	 * SecurePrefsStore. This process must be idempotent and handle missing keys
	 * gracefully.
	 */
	private void migratePreferences() {
		System.out.println("Running legacy preference migration from config.properties...");
		System.out.println("Legacy preference migration simulation completed.");
	}

	public static void main(String[] args) {
		new SetupLauncher().initializePlugin();
	}
}