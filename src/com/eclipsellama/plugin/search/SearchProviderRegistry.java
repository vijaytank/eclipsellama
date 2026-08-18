package com.eclipsellama.plugin.search;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of all known {@link WebSearchProvider}s. Providers are keyed by id
 * so the UI can enumerate options and the service can route by preference.
 */
public final class SearchProviderRegistry {

	private final Map<String, WebSearchProvider> byId = new LinkedHashMap<>();

	public SearchProviderRegistry(List<WebSearchProvider> providers) {
		for (WebSearchProvider p : providers) {
			byId.put(p.getId(), p);
		}
	}

	/**
	 * Looks up a provider by id, or the built-in provider if absent/unknown.
	 */
	public WebSearchProvider get(String id) {
		WebSearchProvider p = byId.get(id);
		return p != null ? p : builtin();
	}

	public WebSearchProvider builtin() {
		return byId.get("builtin");
	}

	public List<WebSearchProvider> all() {
		return List.copyOf(byId.values());
	}
}
