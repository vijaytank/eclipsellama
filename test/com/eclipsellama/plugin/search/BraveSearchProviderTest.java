package com.eclipsellama.plugin.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.List;

import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

/**
 * Unit tests for {@link BraveSearchProvider} against a local stub server.
 */
public class BraveSearchProviderTest {

	@Test
	public void testParsesResults() throws Exception {
		String body = "{\"web\":{\"results\":["
				+ "{\"title\":\"Alpha\",\"url\":\"http://b/1\",\"description\":\"desc one\"}]}}";

		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/search", ex -> {
			assertEquals("tok-123", ex.getRequestHeaders().getFirst("X-Subscription-Token"));
			byte[] bytes = body.getBytes();
			ex.sendResponseHeaders(200, bytes.length);
			ex.getResponseBody().write(bytes);
			ex.close();
		});
		server.start();
		try {
			String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/search";
			BraveSearchProvider provider = new BraveSearchProvider(endpoint, "tok-123", 5, HttpClient.newHttpClient());
			List<SearchResult> results = provider.search("query");
			assertEquals(1, results.size());
			assertEquals("Alpha", results.get(0).getTitle());
			assertEquals("http://b/1", results.get(0).getUrl());
			assertEquals("desc one", results.get(0).getSnippet());
		} finally {
			server.stop(0);
		}
	}

	@Test
	public void testEmptyWithoutApiKey() {
		BraveSearchProvider provider = new BraveSearchProvider("");
		assertTrue(provider.search("query").isEmpty());
	}

	@Test
	public void testConfiguredOnlyWithApiKey() {
		BraveSearchProvider unconfigured = new BraveSearchProvider("");
		assertTrue(!unconfigured.isConfigured());
		BraveSearchProvider configured = new BraveSearchProvider("tok-123");
		assertTrue(configured.isConfigured());
	}

	@Test
	public void testMetadata() {
		BraveSearchProvider provider = new BraveSearchProvider("tok-123");
		assertEquals("brave", provider.getId());
		assertEquals("Brave Search", provider.getDisplayName());
	}
}
