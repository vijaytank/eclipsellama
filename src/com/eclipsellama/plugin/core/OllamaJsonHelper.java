package com.eclipsellama.plugin.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * JSON-Hilfsklasse für Ollama API.
 */
public final class OllamaJsonHelper {

	private OllamaJsonHelper() {
		// Utility class
	}

	/**
	 * Escapet einen String für die sichere JSON-Einbindung.
	 */
	public static String escape(String input) {
		if (input == null) {
			return "";
		}
		return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t",
				"\\t");
	}

	/**
	 * Parse die "response" Feld aus Ollama /api/generate Antwort.
	 */
	public static String parseGenerateResponse(String json) {
		try {
			JSONObject obj = new JSONObject(json);
			return obj.optString("response", "");
		} catch (JSONException e) {
			return "";
		}
	}

	/**
	 * Parse das "content" Feld aus Ollama /api/chat Streaming Antwort.
	 */
	public static String parseChatChunk(String json) {
		try {
			JSONObject obj = new JSONObject(json);
			JSONObject message = obj.optJSONObject("message");
			if (message != null) {
				return message.optString("content", "");
			}
			return "";
		} catch (JSONException e) {
			return "";
		}
	}

	/**
	 * Prüft, ob das Streaming bei Ollama abgeschlossen ist.
	 */
	public static boolean isDone(String json) {
		try {
			JSONObject obj = new JSONObject(json);
			return obj.optBoolean("done", false);
		} catch (JSONException e) {
			return false;
		}
	}

	/**
	 * Parse Modellnamen aus Ollama /api/tags Antwort.
	 */
	public static String[] parseModelList(String json) {
		try {
			JSONObject obj = new JSONObject(json);
			JSONArray models = obj.optJSONArray("models");
			if (models == null) {
				return new String[0];
			}
			String[] result = new String[models.length()];
			for (int i = 0; i < models.length(); i++) {
				JSONObject model = models.getJSONObject(i);
				result[i] = model.optString("name", "unknown");
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
	 * Baut ein Chat-Request Payload für Ollama.
	 */
	public static String buildChatRequest(String model, JSONArray messages, boolean stream) {
		JSONObject request = new JSONObject();
		request.put("model", model);
		request.put("messages", messages);
		request.put("stream", stream);
		return request.toString();
	}

	/**
	 * Baut ein Generate-Request Payload für Ollama.
	 */
	public static String buildGenerateRequest(String model, String prompt, boolean stream) {
		JSONObject request = new JSONObject();
		request.put("model", model);
		request.put("prompt", prompt);
		request.put("stream", stream);
		return request.toString();
	}
}