package com.eclipsellama.plugin.preferences;

/**
 * Unit tests for McpPreferencePage (structure only, without UI dependencies).
 * Tests the logical structure and data model without requiring SWT/Eclipse
 * runtime.
 */
public class McpPreferencePageTests {

	private static int totalPassed = 0;
	private static int totalFailed = 0;

	public static void main(String[] args) {
		System.out.println("╔════════════════════════════════════════╗");
		System.out.println("║     McpPreferencePage Tests            ║");
		System.out.println("╚════════════════════════════════════════╝\n");

		testMcpPreferencePageStructure();

		System.out.println("\n╔════════════════════════════════════════╗");
		System.out.println("║  Results: " + totalPassed + " passed, " + totalFailed + " failed");
		System.out.println("╚════════════════════════════════════════╝");

		if (totalFailed == 0) {
			System.out.println("\n✅ All McpPreferencePage tests passed!");
		} else {
			System.out.println("\n❌ Some McpPreferencePage tests failed");
			System.exit(1);
		}
	}

	private static void testMcpPreferencePageStructure() {
		System.out.println("━━━ McpPreferencePage Structure Tests ━━━");

		test("McpPreferencePage class exists", () -> {
			try {
				Class.forName("com.eclipsellama.plugin.preferences.McpPreferencePage");
				return true;
			} catch (ClassNotFoundException e) {
				return false;
			}
		});

		test("McpPreferencePage extends PreferencePage", () -> {
			try {
				Class<?> clazz = Class.forName("com.eclipsellama.plugin.preferences.McpPreferencePage");
				Class<?> superClass = clazz.getSuperclass();
				return superClass != null && superClass.getSimpleName().equals("PreferencePage");
			} catch (Exception e) {
				return false;
			}
		});

		test("McpPreferencePage has required methods", () -> {
			try {
				Class<?> clazz = Class.forName("com.eclipsellama.plugin.preferences.McpPreferencePage");
				// Check for createContents method
				boolean hasCreateContents = false;
				for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
					if (m.getName().equals("createContents")) {
						hasCreateContents = true;
						break;
					}
				}
				return hasCreateContents;
			} catch (Exception e) {
				return false;
			}
		});
	}

	private static void test(String name, java.util.function.BooleanSupplier test) {
		try {
			if (test.getAsBoolean()) {
				System.out.println("  ✅ " + name);
				totalPassed++;
			} else {
				System.out.println("  ❌ " + name);
				totalFailed++;
			}
		} catch (Exception e) {
			System.out.println("  ❌ " + name + " - " + e.getMessage());
			totalFailed++;
		}
	}
}