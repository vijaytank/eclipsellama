package com.eclipsellama.plugin.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;

import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;

public class OpenAIClient implements LLMClient {

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 120000;

    public OpenAIClient() {
        // Constructor
    }

    private String getEndpoint() {
        return EclipseLlamaPreferences.getEndpoint();
    }

    private String getApiKey() {
        return EclipseLlamaPreferences.getApiKey();
    }

    @Override
    public boolean isServerReachable() {
        try {
            URL url = new URL(getEndpoint() + "/models");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + getApiKey());
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            try {
                conn.connect();
                return conn.getResponseCode() == 200;
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String[] getAvailableModels() {
        try {
            URL url = new URL(getEndpoint() + "/models");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + getApiKey());
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            try {
                conn.connect();
                if (conn.getResponseCode() != 200) {
                    return new String[0];
                }
                String response = readResponse(conn.getInputStream());
                return OpenAIJsonHelper.parseOpenAIModelList(response);
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    @Override
    public String chat(String prompt, String model) {
        return chat(prompt, model, 0.7f, 2048);
    }

    @Override
    public String chat(String prompt, String model, float temperature, int maxTokens) {
        try {
            JSONArray messages = new JSONArray();
            messages.put(OpenAIJsonHelper.buildChatMessage("user", prompt));
            String payload = OpenAIJsonHelper.buildOpenAIChatRequest(model, messages, false, temperature, maxTokens);
            String response = sendPost(getEndpoint() + "/chat/completions", payload);
            return OpenAIJsonHelper.parseOpenAIChatResponse(response);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public void streamChat(List<ChatMessage> messages, String model,
                           Consumer<String> onChunk, Consumer<String> onComplete, Consumer<String> onError) {
        streamChat(messages, model, 0.7f, 2048, onChunk, onComplete, onError);
    }

    @Override
    public void streamChat(List<ChatMessage> messages, String model, float temperature, int maxTokens,
                           Consumer<String> onChunk, Consumer<String> onComplete, Consumer<String> onError) {
        CompletableFuture.runAsync(() -> {
            StringBuilder fullResponse = new StringBuilder();
            HttpURLConnection conn = null;

            try {
                JSONArray jsonMessages = new JSONArray();
                for (ChatMessage msg : messages) {
                    jsonMessages.put(OpenAIJsonHelper.buildChatMessage(msg.getRole(), msg.getContent()));
                }

                String payload = OpenAIJsonHelper.buildOpenAIChatRequest(model, jsonMessages, true, temperature, maxTokens);

                URL url = new URL(getEndpoint() + "/chat/completions");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + getApiKey());
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                InputStream stream = (conn.getResponseCode() >= 400)
                        ? conn.getErrorStream()
                        : conn.getInputStream();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String chunk = OpenAIJsonHelper.parseOpenAIChatChunk(line);
                        if (chunk != null) {
                            fullResponse.append(chunk);
                            final String c = chunk;
                            Display.getDefault().asyncExec(() -> onChunk.accept(c));
                        } else if (OpenAIJsonHelper.isOpenAIDone(line)) {
                            break;
                        }
                    }
                }

                final String result = fullResponse.toString();
                Display.getDefault().asyncExec(() -> onComplete.accept(result));

            } catch (Exception e) {
                final String error = e.getMessage();
                Display.getDefault().asyncExec(() -> onError.accept(error));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

  
    @Override
    public String generate(String prompt, String model) {
        // Map generate to chat with single user message for OpenAI
        return chat(prompt, model, 0.7f, 2048);
    }

    @Override
    public CompletableFuture<String> generateAsync(String prompt, String model) {
        return CompletableFuture.supplyAsync(() -> generate(prompt, model));
    }

    // --- Helper Methods ---

    private String sendPost(String endpoint, String payload) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + getApiKey());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setDoOutput(true);

        try {
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            InputStream stream = (conn.getResponseCode() >= 400)
                    ? conn.getErrorStream()
                    : conn.getInputStream();
            return readResponse(stream);
        } finally {
            conn.disconnect();
        }
    }

    private String readResponse(InputStream stream) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[8192];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                response.append(buffer, 0, len);
            }
        }
        return response.toString();
    }
}