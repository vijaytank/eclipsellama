package com.eclipsellama.plugin.search;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Basic DuckDuckGo web search provider using the Instant Answer API. Uses
 * {@link HttpClient} (Java 11+, matching JavaSE-21 target).
 */
public class DuckDuckGoSearchProvider implements WebSearchProvider {

	@Override
	public List<SearchResult> search(String query) {
		if (query == null || query.isBlank()) {
			return Collections.emptyList();
		}
		try {
			String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
			String url = "https://api.duckduckgo.com/?q=" + encoded + "&format=json&no_html=1";
			HttpRequest req = HttpRequest.newBuilder(java.net.URI.create(url)).GET().build();
			HttpResponse<String> resp = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
					.build().send(req, HttpResponse.BodyHandlers.ofString());
			if (resp.statusCode() != 200) {
				return Collections.emptyList();
			}
			return parse(resp.body());
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	private List<SearchResult> parse(String body) {
		java.util.List<SearchResult> results = new java.util.ArrayList<>();
		try {
			JSONObject json = new JSONObject(body);
			// Abstract / instant answer
			if (json.has("AbstractText") && !json.optString("AbstractText").isEmpty()) {
				results.add(new SearchResult(json.optString("Heading", "Result"),
						json.optString("AbstractURL", ""), json.optString("AbstractText", "")));
			}
			// Related topics
			JSONArray topics = json.optJSONArray("RelatedTopics");
			if (topics != null) {
				for (int i = 0; i < topics.length(); i++) {
					JSONObject topic = topics.optJSONObject(i);
					if (topic == null) {
						continue;
					}
					String text = topic.optString("Text", "");
					String url = topic.optString("FirstURL", "");
					if (!text.isEmpty()) {
						results.add(new SearchResult(text, url, text));
					}
				}
			}
		} catch (Exception e) {
			// ignore malformed responses
		}
		return results;
	}
}
