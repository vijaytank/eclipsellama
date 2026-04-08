package com.eclipsellama.plugin.core;

import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;
import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences.BackendType;

/**
 * Singleton helper to provide the correct LLMClient instance.
 */
public class ClientProvider {

    private static LLMClient ollamaClient;
    private static LLMClient openAIClient;

    private ClientProvider() {
        // Prevent instantiation
    }

    /**
     * Initialize or refresh the client based on preferences.
     */
    public static void initClient() {
        openAIClient = new OpenAIClient();
        ollamaClient = new OllamaClient();
    }

    /**
     * Get the current client instance.
     */
    public static LLMClient getClient() {
        if (ollamaClient == null || openAIClient == null) {
            initClient();
        }
        BackendType backendType = EclipseLlamaPreferences.getBackendType();
        return (backendType==BackendType.OPENAI)?openAIClient:ollamaClient; 
    }
}