package com.eclipsellama.plugin.search;

import java.util.List;

/**
 * Provides web search results for the ChatView "Web Search" toggle. The
 * {@link WebSearchService} owns fallback between the built-in provider and
 * optional external providers (SearXNG, Brave).
 */
public interface WebSearchProvider {

	/**
	 * A stable, machine-readable id used for preference selection and routing (e.g.
	 * "builtin", "searxng", "brave").
	 */
	String getId();

	/**
	 * Human-readable name shown in the preferences UI.
	 */
	String getDisplayName();

	/**
	 * Whether this provider can be used, i.e. it requires no external setup that is
	 * currently missing (an endpoint, an API key). Built-in is always true.
	 */
	boolean isConfigured();

	/**
	 * Runs a web search for the given query.
	 *
	 * @param query the search query text.
	 * @return a list of {@link SearchResult}s; never null, possibly empty.
	 */
	List<SearchResult> search(String query);
}
