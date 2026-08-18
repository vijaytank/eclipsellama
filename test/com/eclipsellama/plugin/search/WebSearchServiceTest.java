package com.eclipsellama.plugin.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;

/**
 * Unit tests for {@link WebSearchService}, covering provider selection and
 * fallback. Resets the static preference cache so each test starts clean.
 */
public class WebSearchServiceTest {

	@Before
	public void setUp() throws Exception {
		Path temp = Files.createTempDirectory("eclipsellama-search-test");
		System.setProperty("user.home", temp.toString());
		resetSingletonState();
	}

	private static void resetSingletonState() throws Exception {
		java.lang.reflect.Field loaded = EclipseLlamaPreferences.class.getDeclaredField("loaded");
		loaded.setAccessible(true);
		loaded.setBoolean(null, false);
		java.lang.reflect.Field props = EclipseLlamaPreferences.class.getDeclaredField("properties");
		props.setAccessible(true);
		props.set(null, null);
	}

	private static WebSearchProvider provider(String id, boolean configured, List<SearchResult> results) {
		return new WebSearchProvider() {
			@Override
			public String getId() {
				return id;
			}

			@Override
			public String getDisplayName() {
				return id;
			}

			@Override
			public boolean isConfigured() {
				return configured;
			}

			@Override
			public List<SearchResult> search(String query) {
				return results;
			}
		};
	}

	@Test
	public void testDefaultSelectionIsBuiltin() {
		WebSearchProvider builtin = provider("builtin", true, List.of(new SearchResult("T", "https://x.com", "S")));
		SearchProviderRegistry registry = new SearchProviderRegistry(List.of(builtin));
		WebSearchService service = new WebSearchService(registry);
		assertEquals("builtin", service.selectedProvider().getId());
	}

	@Test
	public void testDisabledReturnsNullProvider() {
		EclipseLlamaPreferences.setSearchProvider("disabled");
		WebSearchProvider builtin = provider("builtin", true, List.of());
		WebSearchService service = new WebSearchService(new SearchProviderRegistry(List.of(builtin)));
		assertNull(service.selectedProvider());
		assertTrue(service.search("q").isEmpty());
	}

	@Test
	public void testUnconfiguredExternalFallsBackToBuiltin() {
		EclipseLlamaPreferences.setSearchProvider("searxng");
		WebSearchProvider builtin = provider("builtin", true, List.of(new SearchResult("T", "https://x.com", "S")));
		WebSearchProvider searxng = provider("searxng", false, List.of());
		WebSearchService service = new WebSearchService(new SearchProviderRegistry(List.of(builtin, searxng)));
		assertEquals("builtin", service.selectedProvider().getId());
	}

	@Test
	public void testConfiguredExternalIsSelected() {
		EclipseLlamaPreferences.setSearchProvider("searxng");
		WebSearchProvider builtin = provider("builtin", true, List.of());
		WebSearchProvider searxng = provider("searxng", true, List.of(new SearchResult("S", "https://s.com", "sn")));
		WebSearchService service = new WebSearchService(new SearchProviderRegistry(List.of(builtin, searxng)));
		assertEquals("searxng", service.selectedProvider().getId());
		assertEquals(1, service.search("q").size());
	}

	@Test
	public void testEmptyExternalFallsBackWhenEnabled() {
		EclipseLlamaPreferences.setSearchProvider("brave");
		EclipseLlamaPreferences.setSearchFallbackToBuiltin(true);
		WebSearchProvider builtin = provider("builtin", true, List.of(new SearchResult("T", "https://x.com", "S")));
		WebSearchProvider brave = provider("brave", true, List.of());
		WebSearchService service = new WebSearchService(new SearchProviderRegistry(List.of(builtin, brave)));
		assertEquals(1, service.search("q").size());
		assertEquals("https://x.com", service.search("q").get(0).getUrl());
	}

	@Test
	public void testNoFallbackWhenDisabled() {
		EclipseLlamaPreferences.setSearchProvider("brave");
		EclipseLlamaPreferences.setSearchFallbackToBuiltin(false);
		WebSearchProvider builtin = provider("builtin", true, List.of(new SearchResult("T", "https://x.com", "S")));
		WebSearchProvider brave = provider("brave", true, List.of());
		WebSearchService service = new WebSearchService(new SearchProviderRegistry(List.of(builtin, brave)));
		assertTrue(service.search("q").isEmpty());
	}

	@Test
	public void testBuildPromptDelegates() {
		WebSearchProvider builtin = provider("builtin", true, List.of());
		WebSearchService service = new WebSearchService(new SearchProviderRegistry(List.of(builtin)));
		String prompt = service.buildPrompt("hello", List.of(new SearchResult("T", "https://x.com", "S")));
		assertTrue(prompt.contains("<WebSearchContext>"));
		assertTrue(prompt.contains("hello"));
	}
}
