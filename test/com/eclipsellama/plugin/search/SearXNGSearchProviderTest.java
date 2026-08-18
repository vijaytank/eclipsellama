package com.eclipsellama.plugin.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.List;

import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

/**
 * Unit tests for {@link SearXNGSearchProvider} against a local stub server.
 */
public class SearXNGSearchProviderTest {

	@Test
	public void testParsesResults() throws Exception {
		String body = "{\"results\":[" + "{\"title\":\"First\",\"url\":\"http://x/1\",\"content\":\"snippet one\"},"
				+ "{\"title\":\"Second\",\"url\":\"http://x/2\",\"content\":\"snippet two\"}]}";

		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/search", ex -> {
			byte[] bytes = body.getBytes();
			ex.sendResponseHeaders(200, bytes.length);
			ex.getResponseBody().write(bytes);
			ex.close();
		});
		server.start();
		try {
			String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/search";
			SearXNGSearchProvider provider = new SearXNGSearchProvider(endpoint, 5, HttpClient.newHttpClient());
			List<SearchResult> results = provider.search("hello");
			assertEquals(2, results.size());
			assertEquals("First", results.get(0).getTitle());
			assertEquals("http://x/1", results.get(0).getUrl());
			assertEquals("snippet one", results.get(0).getSnippet());
		} finally {
			server.stop(0);
		}
	}

	@Test
	public void testEmptyOnBlankQuery() {
		SearXNGSearchProvider provider = new SearXNGSearchProvider("http://localhost:8080/search");
		assertTrue(provider.search("   ").isEmpty());
	}

	@Test
	public void testConfiguredOnlyWhenEndpointPresent() {
		SearXNGSearchProvider unconfigured = new SearXNGSearchProvider("", 5, HttpClient.newHttpClient());
		assertTrue(!unconfigured.isConfigured());
		SearXNGSearchProvider configured = new SearXNGSearchProvider("http://localhost:8080/search", 5,
				HttpClient.newHttpClient());
		assertTrue(configured.isConfigured());
	}

	@Test
	public void testMetadata() {
		SearXNGSearchProvider provider = new SearXNGSearchProvider("http://localhost:8080/search");
		assertEquals("searxng", provider.getId());
		assertEquals("SearXNG", provider.getDisplayName());
	}
}
