package com.eclipsellama.plugin.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.List;

import org.junit.Test;

import com.eclipsellama.plugin.search.parser.DuckDuckGoHtmlParser;
import com.sun.net.httpserver.HttpServer;

/**
 * Unit tests for {@link BuiltInSearchProvider} against a local stub server.
 * Runs as a plain JUnit test.
 */
public class BuiltInSearchProviderTest {

	private static final String HTML = ""
			+ "<a rel=\"nofollow\" class=\"result__a\" href=\"https://example.com/1\">First</a>"
			+ "<a class=\"result__snippet\">Snippet one</a>"
			+ "<a rel=\"nofollow\" class=\"result__a\" href=\"https://example.com/2\">Second</a>"
			+ "<a class=\"result__snippet\">Snippet two</a>";

	@Test
	public void testParsesResultsFromServer() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/", ex -> {
			byte[] bytes = HTML.getBytes();
			ex.sendResponseHeaders(200, bytes.length);
			ex.getResponseBody().write(bytes);
			ex.close();
		});
		server.start();
		try {
			String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/html";
			BuiltInSearchProvider provider = new BuiltInSearchProvider(endpoint, 10, HttpClient.newHttpClient(),
					new DuckDuckGoHtmlParser());
			List<SearchResult> results = provider.search("hello");
			assertEquals(2, results.size());
			assertEquals("First", results.get(0).getTitle());
			assertEquals("https://example.com/1", results.get(0).getUrl());
			assertEquals("Snippet one", results.get(0).getSnippet());
		} finally {
			server.stop(0);
		}
	}

	@Test
	public void testEmptyOnBlankQuery() {
		BuiltInSearchProvider provider = new BuiltInSearchProvider();
		assertTrue(provider.search("   ").isEmpty());
	}

	@Test
	public void testMetadata() {
		BuiltInSearchProvider provider = new BuiltInSearchProvider();
		assertEquals("builtin", provider.getId());
		assertEquals("Built-in search", provider.getDisplayName());
		assertTrue(provider.isConfigured());
	}

	@Test
	public void testBuildsOnlyOneQueryParameter() {
		java.net.URI uri = BuiltInSearchProvider.buildSearchUri("https://html.duckduckgo.com/html/",
				"What is the latest Qwen model?");
		String rawQuery = uri.getRawQuery();
		assertTrue(rawQuery.startsWith("q="));
		assertTrue(!rawQuery.contains("q=&"));
		assertTrue(!rawQuery.contains("&q="));
	}

	@Test
	public void testRemovesExistingQueryFromEndpoint() {
		java.net.URI uri = BuiltInSearchProvider.buildSearchUri("https://html.duckduckgo.com/html/?q=",
				"Qwen latest model");
		assertEquals("https://html.duckduckgo.com/html/?q=Qwen+latest+model", uri.toString());
	}

	@Test
	public void testNormalizesSearchQuerySuffixes() {
		assertEquals("latest Qwen model Alibaba", BuiltInSearchProvider
				.normalizeSearchQuery("latest Qwen model Alibaba? Search the web and cite the source title and URL"));
	}
}
