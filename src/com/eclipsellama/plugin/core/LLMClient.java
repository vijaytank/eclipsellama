package com.eclipsellama.plugin.core;

import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;

public interface LLMClient {
    boolean isServerReachable();
    String[] getAvailableModels();
    String chat(String prompt, String model);
    String chat(String prompt, String model, float temperature, int maxTokens);
    void streamChat(List<ChatMessage> messages, String model,
                    Consumer<String> onChunk, Consumer<String> onComplete, Consumer<String> onError);
    void streamChat(List<ChatMessage> messages, String model, float temperature, int maxTokens,
                    Consumer<String> onChunk, Consumer<String> onComplete, Consumer<String> onError);
    String generate(String prompt, String model);
    CompletableFuture<String> generateAsync(String prompt, String model);
}