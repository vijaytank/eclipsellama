package com.eclipsellama.plugin.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import com.eclipsellama.plugin.mcp.McpServerConfig;

public final class McpServerStore {
	private static final String NODE = "com.eclipsellama.plugin.preferences.mcp";
	private static final String KEY_SERVERS = "servers_list";

	private McpServerStore() {
	}

	/**
	 * Loads servers from Eclipse preferences using a simple CSV-like format to
	 * avoid external Gson dependency. Format:
	 * transport|endpoint|token|command|arguments;...
	 */
	public static List<McpServerConfig> load() {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(NODE);
		String data = prefs.get(KEY_SERVERS, "");
		List<McpServerConfig> servers = new ArrayList<>();

		if (data.isEmpty()) {
			return servers;
		}

		String[] entries = data.split(";");
		for (String entry : entries) {
			String[] parts = entry.split("\\|", -1);
			if (parts.length >= 2) {
				McpServerConfig config = new McpServerConfig();
				config.setTransport(parts[0]);
				config.setEndpoint(parts[1]);
				if (parts.length >= 3) {
					config.setBearerToken(parts[2]);
				}
				if (parts.length >= 4) {
					config.setCommand(parts[3]);
				}
				if (parts.length >= 5) {
					config.setArguments(parts[4]);
				}
				servers.add(config);
			}
		}
		return servers;
	}

	public static void save(List<McpServerConfig> servers) {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(NODE);

		String data = servers.stream().map(s -> String.format("%s|%s|%s|%s|%s", s.getTransport(),
				s.getEndpoint() == null ? "" : s.getEndpoint(), s.getBearerToken() == null ? "" : s.getBearerToken(),
				s.getCommand() == null ? "" : s.getCommand(), s.getArguments() == null ? "" : s.getArguments()))
				.collect(Collectors.joining(";"));

		prefs.put(KEY_SERVERS, data);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}
}
