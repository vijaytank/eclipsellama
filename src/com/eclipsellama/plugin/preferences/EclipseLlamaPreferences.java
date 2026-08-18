package com.eclipsellama.plugin.preferences;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Preferences manager for EclipseLlama. Stores settings in
 * ~/.eclipsellama/config.properties
 */
public final class EclipseLlamaPreferences {

	private static final String CONFIG_DIR = ".eclipsellama";
	private static final String CONFIG_FILE = "config.properties";

	// Keys
	private static final String KEY_ENDPOINT = "ollama.endpoint";
	private static final String KEY_MODEL = "ollama.model";
	private static final String KEY_SETUP_COMPLETE = "setup.complete";
	private static final String KEY_COMMIT_PROMPT = "commit.prompt";
	private static final String KEY_API_KEY = "api.key";
	private static final String KEY_TIMEOUT_SECONDS = "eclipsellama.timeout.seconds";
	private static final String KEY_RETRY_ATTEMPTS = "eclipsellama.retry.attempts";
	private static final String KEY_SEARCH_PROVIDER = "search.provider";
	private static final String KEY_SEARCH_MAX_RESULTS = "search.maxResults";
	private static final String KEY_SEARCH_API_KEY = "search.apiKey";
	private static final String KEY_SEARCH_SEARXNG_ENDPOINT = "search.searxngEndpoint";
	private static final String KEY_SEARCH_FALLBACK = "search.fallbackToBuiltin";
	private static final String KEY_SEARCH_MODE = "search.mode";

	// Defaults
	private static final String DEFAULT_ENDPOINT = "http://localhost:11434";
	private static final String DEFAULT_MODEL = "codellama";
	private static final int DEFAULT_TIMEOUT_SECONDS = 60;
	private static final int DEFAULT_RETRY_ATTEMPTS = 3;
	private static final int MIN_TIMEOUT_SECONDS = 10;
	private static final int MAX_TIMEOUT_SECONDS = 300;
	private static final int MIN_RETRY_ATTEMPTS = 1;
	private static final int MAX_RETRY_ATTEMPTS = 5;
	private static final String DEFAULT_SEARCH_PROVIDER = "builtin";
	private static final int DEFAULT_SEARCH_MAX_RESULTS = 5;
	private static final boolean DEFAULT_SEARCH_FALLBACK_TO_BUILTIN = true;
	private static final String DEFAULT_SEARCH_MODE = "smart";
	private static final String DEFAULT_SEARXNG_ENDPOINT = "";
	private static final String DEFAULT_COMMIT_PROMPT = "Generate a concise Conventional Commit message for the following Git diff. "
			+ "Format: type(scope): description\\n\\n[optional body]";

	private static Properties properties;
	private static boolean loaded = false;

	public enum BackendType {
		OLLAMA, OPENAI
	}

	private EclipseLlamaPreferences() {
		// Utility class
	}

	/**
	 * Get the config directory path.
	 */
	private static Path getConfigDir() {
		return Paths.get(System.getProperty("user.home"), CONFIG_DIR);
	}

	/**
	 * Get the config file path.
	 */
	private static Path getConfigFile() {
		return getConfigDir().resolve(CONFIG_FILE);
	}

	/**
	 * Load preferences from disk.
	 */
	private static synchronized void load() {
		if (loaded) {
			return;
		}

		properties = new Properties();

		// Set defaults
		properties.setProperty(KEY_ENDPOINT, DEFAULT_ENDPOINT);
		properties.setProperty(KEY_MODEL, DEFAULT_MODEL);
		properties.setProperty(KEY_SETUP_COMPLETE, "false");
		properties.setProperty(KEY_COMMIT_PROMPT, DEFAULT_COMMIT_PROMPT);
		properties.setProperty(KEY_API_KEY, "");
		properties.setProperty(KEY_TIMEOUT_SECONDS, String.valueOf(DEFAULT_TIMEOUT_SECONDS));
		properties.setProperty(KEY_RETRY_ATTEMPTS, String.valueOf(DEFAULT_RETRY_ATTEMPTS));
		properties.setProperty(KEY_SEARCH_PROVIDER, DEFAULT_SEARCH_PROVIDER);
		properties.setProperty(KEY_SEARCH_MAX_RESULTS, String.valueOf(DEFAULT_SEARCH_MAX_RESULTS));
		properties.setProperty(KEY_SEARCH_API_KEY, "");
		properties.setProperty(KEY_SEARCH_SEARXNG_ENDPOINT, DEFAULT_SEARXNG_ENDPOINT);
		properties.setProperty(KEY_SEARCH_FALLBACK, String.valueOf(DEFAULT_SEARCH_FALLBACK_TO_BUILTIN));
		properties.setProperty(KEY_SEARCH_MODE, DEFAULT_SEARCH_MODE);

		// Load from file if exists
		Path configFile = getConfigFile();
		if (Files.exists(configFile)) {
			try (FileInputStream fis = new FileInputStream(configFile.toFile())) {
				properties.load(fis);
			} catch (IOException e) {
				System.err.println("EclipseLlama: Failed to load config: " + e.getMessage());
			}
		}

		loaded = true;
	}

	/**
	 * Save preferences to disk.
	 */
	public static synchronized void save() {
		if (properties == null) {
			return;
		}

		Path configDir = getConfigDir();
		Path configFile = getConfigFile();

		try {
			Files.createDirectories(configDir);
			try (FileOutputStream fos = new FileOutputStream(configFile.toFile())) {
				properties.store(fos, "EclipseLlama Configuration");
			}
		} catch (IOException e) {
			System.err.println("EclipseLlama: Failed to save config: " + e.getMessage());
		}
	}

	/**
	 * Get endpoint URL.
	 */
	public static String getEndpoint() {
		load();
		return properties.getProperty(KEY_ENDPOINT, DEFAULT_ENDPOINT);
	}

	/**
	 * Set endpoint URL.
	 */
	public static void setEndpoint(String endpoint) {
		load();
		properties.setProperty(KEY_ENDPOINT,
				(endpoint == null || endpoint.isBlank()) ? DEFAULT_ENDPOINT : endpoint.trim());
	}

	/**
	 * Get selected model name.
	 */
	public static String getModel() {
		load();
		return properties.getProperty(KEY_MODEL, DEFAULT_MODEL);
	}

	/**
	 * Set selected model name.
	 */
	public static void setModel(String model) {
		load();
		properties.setProperty(KEY_MODEL, (model == null || model.isBlank()) ? DEFAULT_MODEL : model.trim());
	}

	/**
	 * Get api key.
	 */
	public static String getApiKey() {
		load();
		return properties.getProperty(KEY_API_KEY, "");
	}

	/**
	 * Set api key.
	 */
	public static void setApiKey(String key) {
		load();
		properties.setProperty(KEY_API_KEY, (key == null || key.isBlank()) ? "" : key.trim());
	}

	/**
	 * Check if initial setup is complete.
	 */
	public static boolean isSetupComplete() {
		load();
		return Boolean.parseBoolean(properties.getProperty(KEY_SETUP_COMPLETE, "false"));
	}

	/**
	 * Mark setup as complete.
	 */
	public static void setSetupComplete(boolean complete) {
		load();
		properties.setProperty(KEY_SETUP_COMPLETE, String.valueOf(complete));
	}

	/**
	 * Get commit message prompt template.
	 */
	public static String getCommitPrompt() {
		load();
		return properties.getProperty(KEY_COMMIT_PROMPT, DEFAULT_COMMIT_PROMPT);
	}

	/**
	 * Set commit message prompt template.
	 */
	public static void setCommitPrompt(String prompt) {
		load();
		properties.setProperty(KEY_COMMIT_PROMPT,
				(prompt == null || prompt.isBlank()) ? DEFAULT_COMMIT_PROMPT : prompt);
	}

	/**
	 * Get recommended code models.
	 */
	public static String[] getRecommendedCodeModels() {
		return new String[] { "codellama", "codellama:7b", "codellama:13b", "deepseek-coder", "deepseek-coder:6.7b",
				"qwen2.5-coder", "starcoder2" };
	}

	/**
	 * Check if config file exists (for first-run detection).
	 */
	public static boolean configExists() {
		return Files.exists(getConfigFile());
	}

	/**
	 * Get configured timeout in seconds, clamped to the range 10-300.
	 */
	public static int getTimeoutSeconds() {
		load();
		return clamp(parseInt(properties.getProperty(KEY_TIMEOUT_SECONDS, String.valueOf(DEFAULT_TIMEOUT_SECONDS)),
				DEFAULT_TIMEOUT_SECONDS), MIN_TIMEOUT_SECONDS, MAX_TIMEOUT_SECONDS);
	}

	/**
	 * Set the configured timeout in seconds (clamped to the range 10-300).
	 */
	public static void setTimeoutSeconds(int seconds) {
		load();
		properties.setProperty(KEY_TIMEOUT_SECONDS,
				String.valueOf(clamp(seconds, MIN_TIMEOUT_SECONDS, MAX_TIMEOUT_SECONDS)));
	}

	/**
	 * Get the configured retry attempt count, clamped to the range 1-5.
	 */
	public static int getRetryAttempts() {
		load();
		return clamp(parseInt(properties.getProperty(KEY_RETRY_ATTEMPTS, String.valueOf(DEFAULT_RETRY_ATTEMPTS)),
				DEFAULT_RETRY_ATTEMPTS), MIN_RETRY_ATTEMPTS, MAX_RETRY_ATTEMPTS);
	}

	/**
	 * Set the configured retry attempt count (clamped to the range 1-5).
	 */
	public static void setRetryAttempts(int attempts) {
		load();
		properties.setProperty(KEY_RETRY_ATTEMPTS,
				String.valueOf(clamp(attempts, MIN_RETRY_ATTEMPTS, MAX_RETRY_ATTEMPTS)));
	}

	private static int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (Exception e) {
			return fallback;
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	/**
	 * Get the selected web search provider id (duckduckgo, searxng, brave).
	 */
	public static String getSearchProvider() {
		load();
		return properties.getProperty(KEY_SEARCH_PROVIDER, DEFAULT_SEARCH_PROVIDER);
	}

	/**
	 * Set the selected web search provider id.
	 */
	public static void setSearchProvider(String provider) {
		load();
		properties.setProperty(KEY_SEARCH_PROVIDER,
				(provider == null || provider.isBlank()) ? DEFAULT_SEARCH_PROVIDER : provider.trim());
	}

	/**
	 * Get the max number of search results to use, clamped to 1-10.
	 */
	public static int getSearchMaxResults() {
		load();
		return clamp(
				parseInt(properties.getProperty(KEY_SEARCH_MAX_RESULTS, String.valueOf(DEFAULT_SEARCH_MAX_RESULTS)),
						DEFAULT_SEARCH_MAX_RESULTS),
				1, 10);
	}

	/**
	 * Set the max number of search results (clamped to 1-10).
	 */
	public static void setSearchMaxResults(int max) {
		load();
		properties.setProperty(KEY_SEARCH_MAX_RESULTS, String.valueOf(clamp(max, 1, 10)));
	}

	/**
	 * Get the web search API key (Brave, etc.).
	 */
	public static String getSearchApiKey() {
		load();
		return properties.getProperty(KEY_SEARCH_API_KEY, "");
	}

	/**
	 * Set the web search API key.
	 */
	public static void setSearchApiKey(String key) {
		load();
		properties.setProperty(KEY_SEARCH_API_KEY, (key == null || key.isBlank()) ? "" : key.trim());
	}

	/**
	 * Get the SearXNG instance endpoint URL.
	 */
	public static String getSearchSearxngEndpoint() {
		load();
		return properties.getProperty(KEY_SEARCH_SEARXNG_ENDPOINT, "");
	}

	/**
	 * Set the SearXNG instance endpoint URL.
	 */
	public static void setSearchSearxngEndpoint(String endpoint) {
		load();
		properties.setProperty(KEY_SEARCH_SEARXNG_ENDPOINT,
				(endpoint == null || endpoint.isBlank()) ? "" : endpoint.trim());
	}

	/**
	 * Whether to fall back to the built-in provider when the selected provider is
	 * unavailable or returns no results.
	 */
	public static boolean getSearchFallbackToBuiltin() {
		load();
		return Boolean.parseBoolean(
				properties.getProperty(KEY_SEARCH_FALLBACK, String.valueOf(DEFAULT_SEARCH_FALLBACK_TO_BUILTIN)));
	}

	/**
	 * Set whether to fall back to the built-in provider.
	 */
	public static void setSearchFallbackToBuiltin(boolean enabled) {
		load();
		properties.setProperty(KEY_SEARCH_FALLBACK, String.valueOf(enabled));
	}

	/**
	 * Get the web search mode: off, smart, ask, or always.
	 */
	public static String getSearchMode() {
		load();
		return properties.getProperty(KEY_SEARCH_MODE, DEFAULT_SEARCH_MODE);
	}

	/**
	 * Set the web search mode (off, smart, ask, always).
	 */
	public static void setSearchMode(String mode) {
		load();
		String normalized = (mode == null || mode.isBlank()) ? DEFAULT_SEARCH_MODE : mode.trim().toLowerCase();
		properties.setProperty(KEY_SEARCH_MODE, normalized);
	}

	/**
	 * Get backend type
	 */
	public static BackendType getBackendType() {
		return (getEndpoint() != null && getEndpoint().contains("v1")) ? BackendType.OPENAI : BackendType.OLLAMA;
	}
}
