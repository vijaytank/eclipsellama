package com.eclipsellama.plugin.context;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.eclipsellama.plugin.context.ModelRouter.Task;

/**
 * Unit tests for {@link ModelRouter}. Runs as a plain JUnit test.
 */
public class ModelRouterTest {

	private Map<String, String> prefs = new HashMap<>();

	@Test
	public void testResolvesConfiguredModel() {
		prefs.put(Task.EXPLAIN.preferenceKey(), "deepseek-coder");
		ModelRouter router = new ModelRouter(k -> prefs.get(k), "codellama");
		assertEquals("deepseek-coder", router.resolve(Task.EXPLAIN));
	}

	@Test
	public void testFallsBackToDefault() {
		ModelRouter router = new ModelRouter(k -> prefs.get(k), "codellama");
		assertEquals("codellama", router.resolve(Task.FIX));
	}

	@Test
	public void testBlankConfiguredFallsBack() {
		prefs.put(Task.FIX.preferenceKey(), "  ");
		ModelRouter router = new ModelRouter(k -> prefs.get(k), "codellama");
		assertEquals("codellama", router.resolve(Task.FIX));
	}
}
