package com.eclipsellama.plugin.preferences;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for timeout/retry preference keys in
 * {@link EclipseLlamaPreferences}. Redirects user.home to a temp dir so the
 * real user config is never touched. Runs as a plain JUnit test.
 */
public class EclipseLlamaPreferencesTest {

	@Before
	public void setUp() throws Exception {
		Path temp = Files.createTempDirectory("eclipsellama-test");
		System.setProperty("user.home", temp.toString());
		// EclipseLlamaPreferences caches its properties in static fields behind a
		// "loaded" flag, so tests that mutate values would leak state across methods.
		// Reset the static cache so each test reloads from the fresh temp user.home.
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

	@Test
	public void testDefaults() {
		assertEquals(60, EclipseLlamaPreferences.getTimeoutSeconds());
		assertEquals(3, EclipseLlamaPreferences.getRetryAttempts());
	}

	@Test
	public void testClampTimeout() {
		EclipseLlamaPreferences.setTimeoutSeconds(5);
		assertEquals(10, EclipseLlamaPreferences.getTimeoutSeconds());
		EclipseLlamaPreferences.setTimeoutSeconds(999);
		assertEquals(300, EclipseLlamaPreferences.getTimeoutSeconds());
	}

	@Test
	public void testClampRetry() {
		EclipseLlamaPreferences.setRetryAttempts(0);
		assertEquals(1, EclipseLlamaPreferences.getRetryAttempts());
		EclipseLlamaPreferences.setRetryAttempts(99);
		assertEquals(5, EclipseLlamaPreferences.getRetryAttempts());
	}
}
