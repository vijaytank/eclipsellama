package com.eclipsellama.plugin.search;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Brave Search API web search provider. Requires an API key supplied via the
 * constructor (the plugin stores it in Eclipse Secure Storage at runtime).
 */
public class BraveSearchProvider implements WebSearchProvider {

	private static final String DEFAULT_ENDPOINT = "https://api.search.brave.com/res/v1/web/search";

	private final String endpoint;
	private final String apiKey;
	private final int maxResults;
	private final HttpClient client;

	public BraveSearchProvider(String apiKey) {
		this(DEFAULT_ENDPOINT, apiKey, 5, HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build());
	}

	public BraveSearchProvider(String apiKey, int maxResults, HttpClient client) {
		this(DEFAULT_ENDPOINT, apiKey, maxResults, client);
	}

	public BraveSearchProvider(String endpoint, String apiKey, int maxResults, HttpClient client) {
		this.endpoint = endpoint == null ? DEFAULT_ENDPOINT : endpoint.trim();
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.maxResults = Math.max(1, maxResults);
		this.client = client;
	}

	@Override
	public String getId() {
		return "brave";
	}

	@Override
	public String getDisplayName() {
		return "Brave Search";
	}

	@Override
	public boolean isConfigured() {
		return apiKey != null && !apiKey.isEmpty();
	}

	@Override
	public List<SearchResult> search(String query) {
		if (query == null || query.isBlank() || apiKey.isEmpty()) {
			return Collections.emptyList();
		}
		try {
			String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
			URI uri = URI.create(endpoint + "?q=" + encoded + "&count=" + maxResults);
			HttpRequest req = HttpRequest.newBuilder(uri).header("X-Subscription-Token", apiKey)
					.header("Accept", "application/json").GET().build();
			HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
			if (resp.statusCode() != 200) {
				return Collections.emptyList();
			}
			return parse(resp.body());
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	private List<SearchResult> parse(String body) {
		List<SearchResult> results = new ArrayList<>();
		try {
			JSONObject json = new JSONObject(body);
			JSONObject web = json.optJSONObject("web");
			JSONArray arr = web == null ? null : web.optJSONArray("results");
			if (arr == null) {
				return results;
			}
			for (int i = 0; i < arr.length() && results.size() < maxResults; i++) {
				JSONObject hit = arr.optJSONObject(i);
				if (hit == null) {
					continue;
				}
				String url = hit.optString("url", "");
				if (url.isEmpty()) {
					continue;
				}
				results.add(new SearchResult(hit.optString("title", url), url, hit.optString("description", "")));
			}
		} catch (Exception e) {
			// ignore malformed responses
		}
		return results;
	}
}
