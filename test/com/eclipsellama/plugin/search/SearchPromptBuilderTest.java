package com.eclipsellama.plugin.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests for {@link SearchPromptBuilder}. Runs as a plain JUnit test.
 */
public class SearchPromptBuilderTest {

	private final SearchPromptBuilder builder = new SearchPromptBuilder();

	@Test
	public void testWrapsResultsInDelimiters() {
		List<SearchResult> results = List.of(new SearchResult("T", "https://x.com", "S"));
		String prompt = builder.build("user question", results);
		assertTrue(prompt.contains("<WebSearchContext>"));
		assertTrue(prompt.contains("</WebSearchContext>"));
		assertTrue(prompt.contains("<UserQuestion>"));
		assertTrue(prompt.contains("user question"));
		assertTrue(prompt.contains("untrusted web context"));
		assertTrue(prompt.contains("https://x.com"));
	}

	@Test
	public void testNullResultsYieldsEmptyContext() {
		String prompt = builder.build("q", null);
		assertTrue(prompt.contains("<WebSearchContext>\n</WebSearchContext>"));
		assertTrue(prompt.contains("<UserQuestion>\nq\n</UserQuestion>"));
	}

	@Test
	public void testCapsSnippetLength() {
		String longSnippet = "a".repeat(SearchPromptBuilder.MAX_SNIPPET_LENGTH + 50);
		List<SearchResult> results = List.of(new SearchResult("T", "https://x.com", longSnippet));
		String prompt = builder.build("q", results);
		assertFalse(prompt.contains(longSnippet));
		assertTrue(prompt.contains("a".repeat(SearchPromptBuilder.MAX_SNIPPET_LENGTH) + "..."));
	}

	@Test
	public void testDeduplicatesAndRejectsMalformedUrls() {
		List<SearchResult> results = new ArrayList<>();
		results.add(new SearchResult("A", "https://x.com", "s1"));
		results.add(new SearchResult("A dup", "https://x.com", "s2"));
		results.add(new SearchResult("bad", "javascript:alert(1)", "s3"));
		String prompt = builder.build("q", results);
		assertEquals(1, countOccurrences(prompt, "https://x.com"));
		assertFalse(prompt.contains("javascript:"));
	}

	@Test
	public void testStripsControlCharacters() {
		List<SearchResult> results = List.of(new SearchResult("T", "https://x.com", "bad\u0007control"));
		String prompt = builder.build("q", results);
		assertFalse(prompt.contains("\u0007"));
	}

	private static int countOccurrences(String haystack, String needle) {
		int count = 0;
		int idx = 0;
		while ((idx = haystack.indexOf(needle, idx)) != -1) {
			count++;
			idx += needle.length();
		}
		return count;
	}
}
