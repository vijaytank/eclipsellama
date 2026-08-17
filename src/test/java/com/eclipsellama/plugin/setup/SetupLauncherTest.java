package test.java.com.eclipsellama.plugin.setup;

import com.eclipsellama.plugin.setup.SetupLauncher;

/**
 * Simple self‑contained test that verifies the {@link SetupLauncher} can be
 * instantiated and that its {@code initializePlugin()} method completes without
 * throwing any unchecked exceptions.
 * <p>
 * This test does not depend on JUnit or any testing framework; it uses a
 * straightforward {@code main} method that prints the outcome and fails fast
 * with a {@link RuntimeException} if initialization does not succeed.
 * </p>
 */
public class SetupLauncherTest {

	/**
	 * Executes the plugin initialization logic and prints the outcome. If any
	 * unchecked exception is thrown the method will terminate the program with a
	 * clear error message.
	 *
	 * @param args command‑line arguments (unused)
	 */
	public static void main(String[] args) {
		System.out.println("\n==================================================");
		System.out.println("Running SetupLauncherTest: Initialization Flow");
		System.out.println("==================================================");

		try {
			new SetupLauncher().initializePlugin();
			System.out.println("✅ Test Passed: initializePlugin completed without throwing an exception.");
		} catch (Throwable ex) {
			System.out.println("❌ Test Failed: initializePlugin threw an exception: " + ex.getMessage());
			// Fail fast with an unchecked exception that does not depend on testing
			// frameworks
			throw new RuntimeException("Unexpected exception during plugin initialization", ex);
		}
	}
}