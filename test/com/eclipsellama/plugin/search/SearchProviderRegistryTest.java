package com.eclipsellama.plugin.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Test;

/**
 * Unit tests for {@link SearchProviderRegistry}. Runs as a plain JUnit test.
 */
public class SearchProviderRegistryTest {

	private static WebSearchProvider provider(String id) {
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
				return true;
			}

			@Override
			public List<SearchResult> search(String query) {
				return List.of();
			}
		};
	}

	@Test
	public void testGetById() {
		WebSearchProvider builtin = provider("builtin");
		WebSearchProvider searxng = provider("searxng");
		SearchProviderRegistry registry = new SearchProviderRegistry(List.of(builtin, searxng));
		assertSame(searxng, registry.get("searxng"));
		assertSame(builtin, registry.builtin());
	}

	@Test
	public void testUnknownIdFallsBackToBuiltin() {
		WebSearchProvider builtin = provider("builtin");
		SearchProviderRegistry registry = new SearchProviderRegistry(List.of(builtin));
		assertSame(builtin, registry.get("nonexistent"));
	}

	@Test
	public void testAllReturnsProviders() {
		WebSearchProvider builtin = provider("builtin");
		WebSearchProvider brave = provider("brave");
		SearchProviderRegistry registry = new SearchProviderRegistry(List.of(builtin, brave));
		assertEquals(2, registry.all().size());
	}
}
