package com.eclipsellama.plugin.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * JSON-Hilfsklasse für OpenAI API.
 */
public final class OpenAIJsonHelper {

    private OpenAIJsonHelper() {
        // Utility class
    }

    /**
     * Escapet einen String für die sichere JSON-Einbindung.
     */
    public static String escape(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Parse das "content" Feld aus OpenAI /v1/chat/completions Streaming Antwort.
     * Verarbeitet SSE-Format (data: ...) und [DONE] Marker.
     */
    public static String parseOpenAIChatChunk(String json) {
        try {
            // Entferne SSE Prefix falls vorhanden
            String data = json;
            if (data.startsWith("data: ")) {
                data = data.substring(6);
            }

            // Handle EOF Marker
            if ("[DONE]".equals(data.trim())) {
                return null; // Signal für Ende
            }

            JSONObject obj = new JSONObject(data);
            JSONArray choices = obj.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject delta = choice.optJSONObject("delta");
                if (delta != null) {
                    return delta.optString("content", "");
                }
            }
            return "";
        } catch (JSONException e) {
            return "";
        }
    }

    /**
     * Prüft, ob das Streaming bei OpenAI abgeschlossen ist.
     */
    public static boolean isOpenAIDone(String json) {
        return "[DONE]".equals(json.trim());
    }

    /**
     * Parse die vollständige Antwort aus OpenAI /v1/chat/completions.
     */
    public static String parseOpenAIChatResponse(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray choices = obj.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.optJSONObject("message");
                if (message != null) {
                    return message.optString("content", "");
                }
            }
            return "";
        } catch (JSONException e) {
            return "";
        }
    }

    /**
     * Parse Modellnamen aus OpenAI /v1/models Antwort.
     */
    public static String[] parseOpenAIModelList(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray models = obj.optJSONArray("data");
            if (models == null) {
                return new String[0];
            }
            String[] result = new String[models.length()];
            for (int i = 0; i < models.length(); i++) {
                JSONObject model = models.getJSONObject(i);
                result[i] = model.optString("id", "unknown");
            }
            return result;
        } catch (JSONException e) {
            return new String[0];
        }
    }

    /**
     * Baut ein Chat-Message JSON Objekt.
     */
    public static JSONObject buildChatMessage(String role, String content) {
        JSONObject msg = new JSONObject();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }

    /**
     * Baut ein Chat-Request Payload für OpenAI.
     */
    public static String buildOpenAIChatRequest(String model, JSONArray messages, boolean stream) {
        JSONObject request = new JSONObject();
        request.put("model", model);
        request.put("messages", messages);
        request.put("stream", stream);
        return request.toString();
    }

    /**
     * Baut ein Chat-Request Payload für OpenAI mit Temperatur.
     */
    public static String buildOpenAIChatRequest(String model, JSONArray messages, boolean stream, float temperature) {
        JSONObject request = new JSONObject();
        request.put("model", model);
        request.put("messages", messages);
        request.put("stream", stream);
        request.put("temperature", temperature);
        return request.toString();
    }

    /**
     * Baut ein Chat-Request Payload für OpenAI mit Temperatur und Max Tokens.
     */
    public static String buildOpenAIChatRequest(String model, JSONArray messages, boolean stream, float temperature, int maxTokens) {
        JSONObject request = new JSONObject();
        request.put("model", model);
        request.put("messages", messages);
        request.put("stream", stream);
        request.put("temperature", temperature);
        request.put("max_tokens", maxTokens);
        return request.toString();
    }

    /**
     * Baut ein Completion-Request Payload für OpenAI.
     */
    public static String buildOpenAICompletionRequest(String model, String prompt, boolean stream) {
        JSONObject request = new JSONObject();
        request.put("model", model);
        request.put("prompt", prompt);
        request.put("stream", stream);
        return request.toString();
    }

    /**
     * Baut ein Completion-Request Payload für OpenAI mit Temperatur.
     */
    public static String buildOpenAICompletionRequest(String model, String prompt, boolean stream, float temperature) {
        JSONObject request = new JSONObject();
        request.put("model", model);
        request.put("prompt", prompt);
        request.put("stream", stream);
        request.put("temperature", temperature);
        return request.toString();
    }
}