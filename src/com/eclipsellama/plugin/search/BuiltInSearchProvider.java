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
 * or API key. Uses standard browser headers and dual GET/POST transport.
 */
public class BuiltInSearchProvider implements WebSearchProvider {

	private static final String ENDPOINT = "https://html.duckduckgo.com/html/";
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

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
		String cleanQuery = normalizeSearchQuery(query);
		if (cleanQuery.isEmpty()) {
			return Collections.emptyList();
		}

		// 1. Primary: GET https://html.duckduckgo.com/html/?q=... with standard browser
		// headers
		try {
			URI uri = buildSearchUri(endpoint, cleanQuery);
			HttpRequest getReq = HttpRequest.newBuilder(uri).header("User-Agent", USER_AGENT)
					.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
					.header("Accept-Language", "en-US,en;q=0.9").timeout(Duration.ofSeconds(15)).GET().build();
			HttpResponse<String> response = client.send(getReq,
					HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() == 200) {
				String body = response.body();
				if (!isHomepage(body)) {
					List<SearchResult> results = parser.parse(body, maxResults);
					if (!results.isEmpty()) {
						return results;
					}
				}
			}
		} catch (Exception ignored) {
		}

		// 2. Fallback: POST form body to DuckDuckGo HTML endpoint
		try {
			String formBody = "q=" + URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8);
			HttpRequest postReq = HttpRequest.newBuilder(URI.create(endpoint)).header("User-Agent", USER_AGENT)
					.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
					.header("Accept-Language", "en-US,en;q=0.9")
					.header("Content-Type", "application/x-www-form-urlencoded").timeout(Duration.ofSeconds(15))
					.POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8)).build();
			HttpResponse<String> response = client.send(postReq,
					HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() == 200) {
				String body = response.body();
				if (!isHomepage(body)) {
					List<SearchResult> results = parser.parse(body, maxResults);
					if (!results.isEmpty()) {
						return results;
					}
				}
			}
		} catch (Exception ignored) {
		}

		// 3. Fallback: DuckDuckGo Lite endpoint
		try {
			String liteEndpoint = "https://lite.duckduckgo.com/lite/";
			String formBody = "q=" + URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8);
			HttpRequest liteReq = HttpRequest.newBuilder(URI.create(liteEndpoint)).header("User-Agent", USER_AGENT)
					.header("Content-Type", "application/x-www-form-urlencoded").timeout(Duration.ofSeconds(15))
					.POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8)).build();
			HttpResponse<String> response = client.send(liteReq,
					HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() == 200) {
				String body = response.body();
				List<SearchResult> results = parser.parse(body, maxResults);
				if (!results.isEmpty()) {
					return results;
				}
			}
		} catch (Exception ignored) {
		}

		return Collections.emptyList();
	}

	/**
	 * Normalizes the configured endpoint to a base URL (no query string) and
	 * appends exactly one {@code q} parameter.
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
	 * Trims common instruction suffixes so the query is a focused search.
	 */
	static String normalizeSearchQuery(String query) {
		String trimmed = query == null ? "" : query;
		trimmed = trimmed.replaceAll("(?i)\\bsearch the web\\b", "");
		trimmed = trimmed.replaceAll("(?i)\\bcite (the )?source title and url\\b", "");
		trimmed = trimmed.replaceAll("(?i)\\bsearch for\\b", "");
		String prev;
		do {
			prev = trimmed;
			trimmed = trimmed.replaceAll("\\s+(and|or|the|to|of|a|an)\\s*$", "");
			trimmed = trimmed.replaceAll("[\\?\\.,;:!\\s]+$", "");
		} while (!trimmed.equals(prev));
		return trimmed.replaceAll("\\s+", " ").trim();
	}
}
