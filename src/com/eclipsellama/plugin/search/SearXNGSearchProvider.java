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
 * SearXNG web search provider backed by a user-configured SearXNG instance. The
 * instance must expose the JSON format (append {@code &format=json}).
 */
public class SearXNGSearchProvider implements WebSearchProvider {

	private final String endpoint;
	private final int maxResults;
	private final HttpClient client;

	public SearXNGSearchProvider(String endpoint) {
		this(endpoint, 5, HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build());
	}

	public SearXNGSearchProvider(String endpoint, int maxResults, HttpClient client) {
		this.endpoint = (endpoint == null) ? "" : endpoint.trim();
		this.maxResults = Math.max(1, maxResults);
		this.client = client;
	}

	@Override
	public String getId() {
		return "searxng";
	}

	@Override
	public String getDisplayName() {
		return "SearXNG";
	}

	@Override
	public boolean isConfigured() {
		return endpoint != null && !endpoint.isBlank();
	}

	@Override
	public List<SearchResult> search(String query) {
		if (query == null || query.isBlank()) {
			return Collections.emptyList();
		}
		try {
			String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
			String separator = endpoint.contains("?") ? "&" : "?";
			URI uri = URI.create(endpoint + separator + "q=" + encoded + "&format=json");
			HttpRequest req = HttpRequest.newBuilder(uri).GET().build();
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
			JSONArray arr = json.optJSONArray("results");
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
				results.add(new SearchResult(hit.optString("title", url), url, hit.optString("content", "")));
			}
		} catch (Exception e) {
			// ignore malformed responses
		}
		return results;
	}
}
