package com.eclipsellama.plugin.search.parser;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.eclipsellama.plugin.search.SearchResult;

/**
 * Parses the lightweight DuckDuckGo HTML search results page. Kept separate
 * from the HTTP transport so a change to DuckDuckGo's page layout only requires
 * editing this parser.
 */
public final class DuckDuckGoHtmlParser {

	private static final Pattern RESULT_A = Pattern
			.compile("<a[^>]*class=\"result__a\"[^>]*href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>");
	private static final Pattern SNIPPET = Pattern.compile("<a[^>]*class=\"result__snippet\"[^>]*>([\\s\\S]*?)</a>");
	private static final Pattern RESULT_LITE_A = Pattern
			.compile("<a[^>]*class=\"result-link\"[^>]*href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>");
	private static final Pattern LITE_SNIPPET = Pattern
			.compile("<td[^>]*class=\"result-snippet\"[^>]*>([\\s\\S]*?)</td>");
	private static final Pattern TAG = Pattern.compile("<[^>]+>");

	/**
	 * Parses result blocks (title, url, snippet) from the DuckDuckGo HTML page.
	 * URLs are deduplicated and malformed ones dropped.
	 */
	public List<SearchResult> parse(String html, int maxResults) {
		List<SearchResult> results = new ArrayList<>();
		if (html == null || html.isBlank()) {
			return results;
		}

		Set<String> seen = new LinkedHashSet<>();
		Matcher titleMatcher = RESULT_A.matcher(html);
		while (titleMatcher.find() && results.size() < maxResults) {
			String rawUrl = decodeDuckDuckGoUrl(unescape(titleMatcher.group(1)));
			String title = stripTags(unescape(titleMatcher.group(2))).trim();
			if (rawUrl.isEmpty() || !isHttpUrl(rawUrl) || title.isEmpty() || !seen.add(rawUrl)) {
				continue;
			}
			String snippet = findNextSnippet(html, titleMatcher.end(), SNIPPET);
			results.add(new SearchResult(title, rawUrl, snippet));
		}

		// Fallback: parse DuckDuckGo Lite layout if standard layout returned no results
		if (results.isEmpty()) {
			Matcher liteMatcher = RESULT_LITE_A.matcher(html);
			while (liteMatcher.find() && results.size() < maxResults) {
				String rawUrl = decodeDuckDuckGoUrl(unescape(liteMatcher.group(1)));
				String title = stripTags(unescape(liteMatcher.group(2))).trim();
				if (rawUrl.isEmpty() || !isHttpUrl(rawUrl) || title.isEmpty() || !seen.add(rawUrl)) {
					continue;
				}
				String snippet = findNextSnippet(html, liteMatcher.end(), LITE_SNIPPET);
				results.add(new SearchResult(title, rawUrl, snippet));
			}
		}

		return results;
	}

	/**
	 * DuckDuckGo result links are protocol-relative redirect URLs of the form
	 * {@code //duckduckgo.com/l/?uddg=<encoded-target>&rut=<hash>}. Extract and
	 * decode the {@code uddg} parameter so the panel and model receive the real
	 * source URL rather than the tracking redirect.
	 */
	private static String decodeDuckDuckGoUrl(String rawHref) {
		if (rawHref == null || rawHref.isBlank()) {
			return "";
		}
		String href = rawHref.trim();
		int uddgStart = href.indexOf("uddg=");
		if (uddgStart >= 0) {
			String encoded = href.substring(uddgStart + "uddg=".length());
			int nextParam = encoded.indexOf('&');
			if (nextParam >= 0) {
				encoded = encoded.substring(0, nextParam);
			}
			try {
				return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
			} catch (IllegalArgumentException ignored) {
				// Malformed encoding; fall through to the original href.
			}
		}
		// Accept protocol-relative links by adding the default scheme.
		if (href.startsWith("//")) {
			return "https:" + href;
		}
		return href;
	}

	private static String findNextSnippet(String html, int fromIndex, Pattern pattern) {
		Matcher m = pattern.matcher(html);
		if (m.find(fromIndex)) {
			return stripTags(unescape(m.group(1))).trim();
		}
		return "";
	}

	private static String stripTags(String s) {
		String stripped = TAG.matcher(s == null ? "" : s).replaceAll(" ").trim();
		return stripped.replaceAll("\\s+", " ").trim();
	}

	private static boolean isHttpUrl(String url) {
		return url.startsWith("http://") || url.startsWith("https://");
	}

	private static String unescape(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
				.replace("&#x27;", "'").replace("&#39;", "'");
	}
}
