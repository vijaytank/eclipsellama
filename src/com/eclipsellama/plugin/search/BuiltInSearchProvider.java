package com.eclipsellama.plugin.search;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import com.eclipsellama.plugin.search.parser.DuckDuckGoHtmlParser;

/**
 * Zero-configuration built-in web search provider backed by DuckDuckGo's
 * lightweight HTML results page. Requires no Docker, Python, Node, MCP server
 * or API key. Parsing is delegated to {@link DuckDuckGoHtmlParser} so
 * page-layout changes only touch the parser.
 */
public class BuiltInSearchProvider implements WebSearchProvider {

	private static final String ENDPOINT = "https://html.duckduckgo.com/html/";

	private final String endpoint;
	private final int maxResults;
	private final HttpClient client;
	private final DuckDuckGoHtmlParser parser;

	public BuiltInSearchProvider() {
		this(5, HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build(),
				new DuckDuckGoHtmlParser());
	}

	public BuiltInSearchProvider(int maxResults, HttpClient client, DuckDuckGoHtmlParser parser) {
		this(ENDPOINT, maxResults, client, parser);
	}

	BuiltInSearchProvider(String endpoint, int maxResults, HttpClient client, DuckDuckGoHtmlParser parser) {
		this.endpoint = endpoint;
		this.maxResults = Math.max(1, Math.min(10, maxResults));
		this.client = client;
		this.parser = parser;
	}

	@Override
	public String getId() {
		return "builtin";
	}

	@Override
	public String getDisplayName() {
		return "Built-in search";
	}

	@Override
	public boolean isConfigured() {
		return true;
	}

	@Override
	public List<SearchResult> search(String query) {
		if (query == null || query.isBlank()) {
			return Collections.emptyList();
		}
		try {
			URI uri = buildSearchUri(endpoint, query);
			HttpRequest request = HttpRequest.newBuilder(uri).header("User-Agent", "EclipseLlama/2.0.1")
					.timeout(Duration.ofSeconds(15)).GET().build();
			HttpResponse<String> response = client.send(request,
					HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() != 200) {
				return Collections.emptyList();
			}
			String body = response.body();
			if (isHomepage(body)) {
				return Collections.emptyList();
			}
			return parser.parse(body, maxResults);
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	/**
	 * Normalizes the configured endpoint to a base URL (no query string) and
	 * appends exactly one {@code q} parameter. Prevents the {@code ?q=&q=...}
	 * duplicate that made DuckDuckGo return its homepage.
	 */
	static URI buildSearchUri(String endpoint, String query) {
		String base = endpoint == null ? "" : endpoint.trim();
		int queryStart = base.indexOf('?');
		if (queryStart >= 0) {
			base = base.substring(0, queryStart);
		}
		if (!base.endsWith("/")) {
			base += "/";
		}
		String encoded = URLEncoder.encode(normalizeSearchQuery(query), StandardCharsets.UTF_8);
		return URI.create(base + "?q=" + encoded);
	}

	private static boolean isHomepage(String html) {
		return html != null && (html.contains("content_wrapper_homepage") || html.contains("search_form_homepage"));
	}

	/**
	 * Trims common instruction suffixes so the query is a focused search. This is a
	 * temporary fallback; the final architecture sends a focused query from the
	 * model's tool call.
	 */
	static String normalizeSearchQuery(String query) {
		String trimmed = query == null ? "" : query;
		trimmed = trimmed.replaceAll("(?i)\\bsearch the web\\b", "");
		trimmed = trimmed.replaceAll("(?i)\\bcite (the )?source title and url\\b", "");
		trimmed = trimmed.replaceAll("(?i)\\bsearch for\\b", "");
		// Repeatedly drop trailing connector words and punctuation (e.g. "? and .")
		// left behind when an instruction suffix was removed.
		String prev;
		do {
			prev = trimmed;
			trimmed = trimmed.replaceAll("\\s+(and|or|the|to|of|a|an)\\s*$", "");
			trimmed = trimmed.replaceAll("[\\?\\.,;:!\\s]+$", "");
		} while (!trimmed.equals(prev));
		return trimmed.replaceAll("\\s+", " ").trim();
	}
}
