package com.eclipsellama.plugin.search;

import java.util.List;

/**
 * Provides web search results for the ChatView "Web Search" toggle. Concrete
 * providers (DuckDuckGo, SearXNG, Brave) are implemented in Phase 3.
 */
public interface WebSearchProvider {

	/**
	 * Runs a web search for the given query.
	 *
	 * @param query the search query text.
	 * @return a list of {@link SearchResult}s; never null, possibly empty.
	 */
	List<SearchResult> search(String query);
}
