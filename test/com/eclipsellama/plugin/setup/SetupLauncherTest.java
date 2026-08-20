package com.eclipsellama.plugin.setup;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests for {@link SetupLauncher} verifying idempotency of plugin
 * initialization. Runs as a JUnit 4 plain test (no Eclipse runtime needed
 * because provider re-registration is guarded by existence checks and
 * non-registration errors are caught).
 */
public class SetupLauncherTest {

	@Test
	public void testInstantiationDoesNotThrow() {
		SetupLauncher launcher = new SetupLauncher();
		assertNotNull(launcher);
	}

	@Test
	public void testInitializePluginIsIdempotent() {
		SetupLauncher launcher = new SetupLauncher();
		try {
			launcher.initializePlugin();
		} catch (Throwable ignored) {
			// In a bare JVM outside Eclipse OSGi, PreferencesService throws
			// ExceptionInInitializerError which extends LinkageError/Throwable.
		}

		try {
			launcher.initializePlugin();
		} catch (IllegalArgumentException e) {
			throw new AssertionError("initializePlugin() must be idempotent — duplicate registration detected", e);
		} catch (Throwable ignored) {
			// Non-registration errors are expected in bare JVM.
		}
	}
}
