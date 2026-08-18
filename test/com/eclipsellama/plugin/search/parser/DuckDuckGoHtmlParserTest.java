package com.eclipsellama.plugin.search.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.eclipsellama.plugin.search.SearchResult;

/**
 * Unit tests for {@link DuckDuckGoHtmlParser}. Runs as a plain JUnit test.
 */
public class DuckDuckGoHtmlParserTest {

	private final DuckDuckGoHtmlParser parser = new DuckDuckGoHtmlParser();

	private static final String HTML = ""
			+ "<a rel=\"nofollow\" class=\"result__a\" href=\"https://example.com/1\">First &amp; Title</a>"
			+ "<a class=\"result__snippet\">Snippet one</a>"
			+ "<a rel=\"nofollow\" class=\"result__a\" href=\"https://example.com/2\">Second</a>"
			+ "<a class=\"result__snippet\">Snippet <b>two</b></a>";

	@Test
	public void testParsesTitleUrlSnippet() {
		List<SearchResult> results = parser.parse(HTML, 10);
		assertEquals(2, results.size());
		SearchResult first = results.get(0);
		assertEquals("First & Title", first.getTitle());
		assertEquals("https://example.com/1", first.getUrl());
		assertEquals("Snippet one", first.getSnippet());
		SearchResult second = results.get(1);
		assertEquals("Second", second.getTitle());
		assertEquals("Snippet two", second.getSnippet());
	}

	@Test
	public void testRespectsMaxResults() {
		List<SearchResult> results = parser.parse(HTML, 1);
		assertEquals(1, results.size());
	}

	@Test
	public void testDeduplicatesUrls() {
		String html = "<a class=\"result__a\" href=\"https://x.com/1\">A</a>"
				+ "<a class=\"result__a\" href=\"https://x.com/1\">A dup</a>";
		List<SearchResult> results = parser.parse(html, 10);
		assertEquals(1, results.size());
	}

	@Test
	public void testDropsNonHttpAndBlank() {
		String html = "<a class=\"result__a\" href=\"/relative\">Rel</a>"
				+ "<a class=\"result__a\" href=\"ftp://x\">Ftp</a>"
				+ "<a class=\"result__a\" href=\"https://ok.com\">Ok</a>";
		List<SearchResult> results = parser.parse(html, 10);
		assertEquals(1, results.size());
		assertEquals("https://ok.com", results.get(0).getUrl());
	}

	@Test
	public void testDecodesDuckDuckGoRedirectUrl() {
		String html = "<a class=\"result__a\" href=\"//duckduckgo.com/l/?uddg=https%3A%2F%2Fwww.cnbc.com%2F2026%2F08%2F17%2Falibaba%2Dmeta%2Dqwen%2Dopen%2Dweight%2Dai%2Dlaptop%2Dmodels.html&amp;rut=73383b61381af6f1e2dfa2574b43b18b1a7a2d66c0e9c3261d7363e502ef7725\">Alibaba Qwen</a>"
				+ "<a class=\"result__snippet\">Snippet</a>";
		List<SearchResult> results = parser.parse(html, 5);
		assertEquals(1, results.size());
		assertEquals("https://www.cnbc.com/2026/08/17/alibaba-meta-qwen-open-weight-ai-laptop-models.html",
				results.get(0).getUrl());
	}

	@Test
	public void testNullAndBlankReturnEmpty() {
		assertTrue(parser.parse(null, 10).isEmpty());
		assertTrue(parser.parse("   ", 10).isEmpty());
	}
}
