package com.eclipsellama.plugin.search;

import java.util.Collections;
import java.util.List;

import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;

/**
 * Central search service that owns provider selection and fallback. The default
 * is the built-in provider; SearXNG and Brave are used only when selected,
 * configured, and (for fallback) allowed.
 */
public final class WebSearchService {

	private final SearchProviderRegistry registry;
	private final SearchPromptBuilder promptBuilder;

	public WebSearchService(SearchProviderRegistry registry) {
		this(registry, new SearchPromptBuilder());
	}

	public WebSearchService(SearchProviderRegistry registry, SearchPromptBuilder promptBuilder) {
		this.registry = registry;
		this.promptBuilder = promptBuilder;
	}

	/**
	 * Runs a search for the given query using the selected provider, falling back
	 * to the built-in provider when the selected one is not configured or, if the
	 * user opted in, when it returns no results. A "disabled" selection returns no
	 * results.
	 *
	 * @return never null, possibly empty.
	 */
	public List<SearchResult> search(String query) {
		WebSearchProvider selected = selectedProvider();
		if (selected == null) {
			return Collections.emptyList();
		}

		if (selected.getId().equals("builtin")) {
			return selected.search(query);
		}

		List<SearchResult> results = selected.search(query);
		if ((results == null || results.isEmpty()) && fallbackEnabled()) {
			WebSearchProvider builtin = registry.builtin();
			if (builtin != null && !builtin.getId().equals(selected.getId())) {
				return builtin.search(query);
			}
		}
		return results == null ? Collections.emptyList() : results;
	}

	/**
	 * The provider that should be used for a request, based on preferences.
	 */
	public WebSearchProvider selectedProvider() {
		String id = EclipseLlamaPreferences.getSearchProvider();
		if ("disabled".equals(id)) {
			return null;
		}
		WebSearchProvider p = registry.get(id);
		// Never use an external provider that is missing its required configuration.
		if (!p.isConfigured()) {
			WebSearchProvider builtin = registry.builtin();
			return builtin != null ? builtin : p;
		}
		return p;
	}

	/**
	 * Builds a safe prompt that embeds the results plus the original question.
	 */
	public String buildPrompt(String userMessage, List<SearchResult> results) {
		return promptBuilder.build(userMessage, results);
	}

	private boolean fallbackEnabled() {
		return EclipseLlamaPreferences.getSearchFallbackToBuiltin();
	}
}
