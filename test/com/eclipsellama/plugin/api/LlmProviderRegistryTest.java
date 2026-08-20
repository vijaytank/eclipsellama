package com.eclipsellama.plugin.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.Test;

/**
 * Unit tests for {@link LlmProviderRegistry}. Runs as a plain JUnit test.
 */
public class LlmProviderRegistryTest {

	private static LlmProvider stub(String name) {
		return new LlmProvider() {
			@Override
			public CompletableFuture<String> generate(String p, String c) {
				return CompletableFuture.completedFuture("");
			}

			@Override
			public Stream<String> stream(String p, String c) {
				return Stream.empty();
			}

			@Override
			public String getProviderName() {
				return name;
			}
		};
	}

	@Test
	public void testRegisterAndGet() {
		LlmProviderRegistry reg = LlmProviderRegistry.getInstance();
		// Use a unique name to avoid conflicts with the singleton across test runs.
		String name = "reg-a-" + System.nanoTime();
		reg.registerProvider(stub(name));
		assertTrue(reg.getProvider(name).isPresent());
	}

	@Test
	public void testDuplicateRejected() {
		LlmProviderRegistry reg = LlmProviderRegistry.getInstance();
		String name = "dup-" + System.nanoTime();
		reg.registerProvider(stub(name));
		try {
			reg.registerProvider(stub(name));
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testGetMissingReturnsEmpty() {
		LlmProviderRegistry reg = LlmProviderRegistry.getInstance();
		assertFalse(reg.getProvider("missing-" + System.nanoTime()).isPresent());
	}

	@Test
	public void testGetAllIsCopy() {
		LlmProviderRegistry reg = LlmProviderRegistry.getInstance();
		String name = "copy-" + System.nanoTime();
		reg.registerProvider(stub(name));
		int before = reg.getAllProviders().size();
		reg.getAllProviders().clear();
		assertEquals(before, reg.getAllProviders().size());
	}

	@Test
	public void testUnregisterProvider() {
		LlmProviderRegistry reg = LlmProviderRegistry.getInstance();
		String name = "unreg-" + System.nanoTime();
		reg.registerProvider(stub(name));
		assertTrue(reg.getProvider(name).isPresent());

		reg.unregisterProvider(name);
		assertFalse(reg.getProvider(name).isPresent());
	}

	@Test
	public void testActiveProviderSelection() {
		LlmProviderRegistry reg = LlmProviderRegistry.getInstance();
		String nameA = "active-a-" + System.nanoTime();
		String nameB = "active-b-" + System.nanoTime();
		reg.registerProvider(stub(nameA));
		reg.registerProvider(stub(nameB));

		reg.setActiveProvider(nameB);
		assertTrue(reg.hasExplicitActiveProvider());
		assertEquals(nameB, reg.getActiveProvider().get().getProviderName());

		reg.setActiveProvider(null);
		assertFalse(reg.hasExplicitActiveProvider());
	}
}
