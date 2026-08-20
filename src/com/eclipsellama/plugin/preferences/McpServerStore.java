package com.eclipsellama.plugin.preferences;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.eclipsellama.plugin.mcp.McpServerConfig;

/**
 * Persists configured MCP servers to {@code ~/.eclipsellama/mcp_servers.json}.
 * Using standard JSON file persistence eliminates OSGi early-startup preference
 * dependency issues while ensuring clean serialization of all server fields and
 * discovered tools.
 */
public final class McpServerStore {

	private static final String CONFIG_DIR = ".eclipsellama";
	private static final String FILE_NAME = "mcp_servers.json";

	private McpServerStore() {
	}

	private static Path getConfigFile() {
		return Paths.get(System.getProperty("user.home"), CONFIG_DIR, FILE_NAME);
	}

	/**
	 * Loads configured MCP servers from disk.
	 *
	 * @return list of configured MCP servers, never null.
	 */
	public static synchronized List<McpServerConfig> load() {
		List<McpServerConfig> servers = new ArrayList<>();
		Path file = getConfigFile();
		if (!Files.exists(file)) {
			return servers;
		}

		try {
			String content = Files.readString(file, StandardCharsets.UTF_8);
			if (content == null || content.isBlank()) {
				return servers;
			}
			JSONArray arr = new JSONArray(content);
			for (int i = 0; i < arr.length(); i++) {
				JSONObject obj = arr.optJSONObject(i);
				if (obj == null) {
					continue;
				}
				McpServerConfig cfg = new McpServerConfig();
				cfg.setId(obj.optString("id", null));
				cfg.setName(obj.optString("name", null));
				cfg.setTransport(obj.optString("transport", null));
				cfg.setEndpoint(obj.optString("endpoint", null));
				cfg.setBearerToken(obj.optString("bearerToken", null));
				cfg.setCommand(obj.optString("command", null));
				cfg.setArguments(obj.optString("arguments", null));

				JSONArray toolsArr = obj.optJSONArray("tools");
				if (toolsArr != null) {
					List<String> tools = new ArrayList<>();
					for (int j = 0; j < toolsArr.length(); j++) {
						String tool = toolsArr.optString(j, null);
						if (tool != null && !tool.isBlank()) {
							tools.add(tool);
						}
					}
					cfg.setTools(tools);
				}
				servers.add(cfg);
			}
		} catch (Exception e) {
			System.err.println("EclipseLlama: Failed to load MCP servers from " + file + ": " + e.getMessage());
		}
		return servers;
	}

	/**
	 * Saves configured MCP servers to disk.
	 *
	 * @param servers the list of MCP servers to persist.
	 */
	public static synchronized void save(List<McpServerConfig> servers) {
		Path file = getConfigFile();
		JSONArray arr = new JSONArray();

		if (servers != null) {
			for (McpServerConfig s : servers) {
				if (s == null) {
					continue;
				}
				JSONObject obj = new JSONObject();
				obj.put("id", s.getId());
				obj.put("name", s.getName() != null ? s.getName() : "");
				obj.put("transport", s.getTransport() != null ? s.getTransport() : "");
				obj.put("endpoint", s.getEndpoint() != null ? s.getEndpoint() : "");
				obj.put("bearerToken", s.getBearerToken() != null ? s.getBearerToken() : "");
				obj.put("command", s.getCommand() != null ? s.getCommand() : "");
				obj.put("arguments", s.getArguments() != null ? s.getArguments() : "");

				JSONArray toolsArr = new JSONArray();
				if (s.getTools() != null) {
					for (String t : s.getTools()) {
						if (t != null && !t.isBlank()) {
							toolsArr.put(t);
						}
					}
				}
				obj.put("tools", toolsArr);
				arr.put(obj);
			}
		}

		try {
			if (file.getParent() != null) {
				Files.createDirectories(file.getParent());
			}
			Files.writeString(file, arr.toString(2), StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.err.println("EclipseLlama: Failed to save MCP servers to " + file + ": " + e.getMessage());
		}
	}
}
