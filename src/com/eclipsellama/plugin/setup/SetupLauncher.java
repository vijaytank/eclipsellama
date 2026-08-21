package com.eclipsellama.plugin.setup;

import org.eclipse.ui.IStartup;

import com.eclipsellama.plugin.api.LlmProviderRegistry;
import com.eclipsellama.plugin.api.OllamaProvider;
import com.eclipsellama.plugin.api.OpenAiProvider;
import com.eclipsellama.plugin.mcp.McpProvider;
import com.eclipsellama.plugin.mcp.McpServerConfig;
import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;
import com.eclipsellama.plugin.preferences.McpServerStore;
import com.eclipsellama.plugin.security.SecurePrefsStore;

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

		// Guard: skip registration if provider is already present (e.g., on plugin
		// restart or when initializePlugin() is called more than once in tests).
		if (registry.getProvider("Ollama").isEmpty()) {
			registry.registerProvider(new OllamaProvider(EclipseLlamaPreferences.getModel()));
		}

		// OpenAI is registered only when an API key is present; the key is read from
		// preferences (Phase 5 migrates it to secure storage).
		String apiKey = EclipseLlamaPreferences.getApiKey();
		if (apiKey != null && !apiKey.isBlank() && registry.getProvider("OpenAI").isEmpty()) {
			registry.registerProvider(new OpenAiProvider());
		}

		// Register each configured MCP server so AI handlers can route through it.
		// Every server is registered; the connection is opened lazily on first use.
		// Guard by provider name to prevent duplicate registration on plugin restart.
		for (McpServerConfig config : McpServerStore.load()) {
			McpProvider mcpProvider = new McpProvider(config);
			if (registry.getProvider(mcpProvider.getProviderName()).isEmpty()) {
				registry.registerProvider(mcpProvider);
			}
		}
	}

	/**
	 * Migrates legacy plain-text keys from config.properties and in-memory MCP
	 * configs into the SecurePrefsStore. This process is idempotent: re-running it
	 * must not duplicate keys or touch already-migrated secrets. Missing keys are
	 * handled gracefully (no-op).
	 */
	private void migratePreferences() {
		if (!SecurePrefsStore.isAvailable()) {
			System.out.println("SecurePrefsStore unavailable; skipping secret migration.");
			return;
		}

		int migrated = 0;

		// 1. OpenAI API key
		String openAiKey = EclipseLlamaPreferences.getApiKey();
		if (openAiKey != null && !openAiKey.isBlank() && SecurePrefsStore.secureGet("api/openai", "apiKey").isEmpty()) {
			SecurePrefsStore.securePutEncrypted("api/openai", "apiKey", openAiKey.trim());
			EclipseLlamaPreferences.setApiKey(""); // clear plaintext legacy
			migrated++;
		}

		// 2. Brave Search API key
		String searchKey = EclipseLlamaPreferences.getSearchApiKey();
		if (searchKey != null && !searchKey.isBlank()
				&& SecurePrefsStore.secureGet("search/brave", "apiKey").isEmpty()) {
			SecurePrefsStore.securePutEncrypted("search/brave", "apiKey", searchKey.trim());
			EclipseLlamaPreferences.setSearchApiKey(""); // clear plaintext legacy
			migrated++;
		}

		// 3. MCP bearer tokens
		for (McpServerConfig config : McpServerStore.load()) {
			String token = config.getBearerToken();
			String node = "mcp/" + config.getName();
			if (token != null && !token.isBlank() && SecurePrefsStore.secureGet(node, "bearerToken").isEmpty()) {
				SecurePrefsStore.securePutEncrypted(node, "bearerToken", token.trim());
				config.setBearerToken(null); // clear plaintext legacy in-memory
				migrated++;
			}
		}

		System.out.println("Legacy preference migration complete. Migrated " + migrated + " secret(s).");
	}

	public static void main(String[] args) {
		new SetupLauncher().initializePlugin();
	}
}