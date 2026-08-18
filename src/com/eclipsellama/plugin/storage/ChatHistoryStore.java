package com.eclipsellama.plugin.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.eclipsellama.plugin.core.ChatMessage;

/**
 * Persists the chat history to a JSON file under {@code ~/.eclipsellama/}. The
 * system prompt is excluded on save and re-added on load so it is never
 * duplicated.
 */
public class ChatHistoryStore {

	private static final String HISTORY_FILE = "history.json";
	private static final int MAX_MESSAGES = 200;

	private final Path file;

	public ChatHistoryStore() {
		this(Paths.get(System.getProperty("user.home"), ".eclipsellama").resolve(HISTORY_FILE));
	}

	ChatHistoryStore(Path file) {
		this.file = file;
	}

	/**
	 * Loads persisted messages (system messages excluded).
	 *
	 * @return never null, possibly empty.
	 */
	public List<ChatMessage> load() {
		if (!Files.exists(file)) {
			return new ArrayList<>();
		}
		try {
			String content = Files.readString(file, StandardCharsets.UTF_8);
			JSONArray arr = new JSONArray(content);
			List<ChatMessage> messages = new ArrayList<>();
			for (int i = 0; i < arr.length(); i++) {
				JSONObject obj = arr.optJSONObject(i);
				if (obj == null) {
					continue;
				}
				String role = obj.optString("role", "");
				String text = obj.optString("content", "");
				if (role.isEmpty() || role.equals(ChatMessage.ROLE_SYSTEM)) {
					continue;
				}
				messages.add(new ChatMessage(role, text));
			}
			return messages;
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	/**
	 * Saves the given messages, dropping any system messages and capping the total
	 * size.
	 */
	public void save(List<ChatMessage> messages) {
		JSONArray arr = new JSONArray();
		int count = 0;
		for (ChatMessage m : messages) {
			if (m.isSystem()) {
				continue;
			}
			JSONObject obj = new JSONObject();
			obj.put("role", m.getRole());
			obj.put("content", m.getContent());
			arr.put(obj);
			count++;
			if (count >= MAX_MESSAGES) {
				break;
			}
		}
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, arr.toString(2), StandardCharsets.UTF_8);
		} catch (IOException e) {
			// persistence is best-effort
		}
	}
}
