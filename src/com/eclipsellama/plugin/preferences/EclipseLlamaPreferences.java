package com.eclipsellama.plugin.preferences;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Preferences manager for EclipseLlama.
 * Stores settings in ~/.eclipsellama/config.properties
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

    // Defaults
    private static final String DEFAULT_ENDPOINT = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "codellama";
    private static final String DEFAULT_COMMIT_PROMPT = "Generate a concise Conventional Commit message for the following Git diff. "
            + "Format: type(scope): description\\n\\n[optional body]";

    private static Properties properties;
    private static boolean loaded = false;
    
    public enum BackendType {
    	OLLAMA,
    	OPENAI
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
        properties.setProperty(KEY_MODEL,
                (model == null || model.isBlank()) ? DEFAULT_MODEL : model.trim());
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
        properties.setProperty(KEY_API_KEY,
                (key == null || key.isBlank()) ? "" : key.trim());
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
        return new String[] {
                "codellama",
                "codellama:7b",
                "codellama:13b",
                "deepseek-coder",
                "deepseek-coder:6.7b",
                "qwen2.5-coder",
                "starcoder2"
        };
    }

    /**
     * Check if config file exists (for first-run detection).
     */
    public static boolean configExists() {
        return Files.exists(getConfigFile());
    }
    
    /**
     * Get backend type
     */
    public static BackendType getBackendType() {
    	return (getEndpoint()!=null && getEndpoint().contains("v1"))?BackendType.OPENAI:BackendType.OLLAMA;
    }
}
