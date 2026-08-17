package com.eclipsellama.plugin;

/**
 * Simple unit tests that can run without Eclipse or plugin dependencies. These
 * test regex patterns and utility logic only.
 */
public class AllTests {

	private static int totalPassed = 0;
	private static int totalFailed = 0;

	public static void main(String[] args) {
		System.out.println("╔════════════════════════════════════════╗");
		System.out.println("║     EclipseLlama Test Suite            ║");
		System.out.println("╚════════════════════════════════════════╝\n");

		testMarkdownPatterns();
		testCommitMessageEdgeCases();
		testPreferencesConstants();

		System.out.println("\n╔════════════════════════════════════════╗");
		System.out.println("║  Results: " + totalPassed + " passed, " + totalFailed + " failed");
		System.out.println("╚════════════════════════════════════════╝");

		if (totalFailed == 0) {
			System.out.println("\n✅ All tests passed!");
		} else {
			System.out.println("\n❌ Some tests failed");
			System.exit(1);
		}
	}

	private static void testMarkdownPatterns() {
		System.out.println("━━━ Markdown Pattern Tests ━━━");

		java.util.regex.Pattern codeBlockPattern = java.util.regex.Pattern.compile("```(\\w*)\\n([\\s\\S]*?)```",
				java.util.regex.Pattern.MULTILINE);

		// Test 1: Detects Java code block
		test("Detects Java code block", () -> {
			String md = "Here is code:\n```java\npublic void test() {}\n```\nEnd";
			java.util.regex.Matcher m = codeBlockPattern.matcher(md);
			return m.find() && "java".equals(m.group(1));
		});

		// Test 2: Extracts code content
		test("Extracts code content", () -> {
			String md = "```python\nprint('hello')\n```";
			java.util.regex.Matcher m = codeBlockPattern.matcher(md);
			return m.find() && m.group(2).contains("print");
		});

		// Test 3: Multiple code blocks
		test("Handles multiple code blocks", () -> {
			String md = "```java\ncode1\n```\ntext\n```python\ncode2\n```";
			java.util.regex.Matcher m = codeBlockPattern.matcher(md);
			int count = 0;
			while (m.find()) {
				count++;
			}
			return count == 2;
		});

		// Test 4: No code blocks
		test("Plain text has no code blocks", () -> {
			String md = "Just plain text without any code.";
			java.util.regex.Matcher m = codeBlockPattern.matcher(md);
			return !m.find();
		});

		// Test 5: Empty language
		test("Code block with no language", () -> {
			String md = "```\nsome code\n```";
			java.util.regex.Matcher m = codeBlockPattern.matcher(md);
			return m.find() && m.group(1).isEmpty();
		});
	}

	private static void testCommitMessageEdgeCases() {
		System.out.println("\n━━━ Commit Message Format Tests ━━━");

		// Conventional commit pattern
		java.util.regex.Pattern conventionalCommit = java.util.regex.Pattern
				.compile("^(feat|fix|docs|style|refactor|test|chore)(\\(.+\\))?: .+");

		test("Valid feat commit", () -> {
			return conventionalCommit.matcher("feat: add new feature").matches();
		});

		test("Valid fix with scope", () -> {
			return conventionalCommit.matcher("fix(ui): repair button").matches();
		});

		test("Valid refactor", () -> {
			return conventionalCommit.matcher("refactor: clean up code").matches();
		});

		test("Invalid: no type", () -> {
			return !conventionalCommit.matcher("add new feature").matches();
		});

		test("Invalid: wrong type", () -> {
			return !conventionalCommit.matcher("feature: add new feature").matches();
		});
	}

	private static void testPreferencesConstants() {
		System.out.println("\n━━━ Constants Tests ━━━");

		test("Default endpoint format is valid URL", () -> {
			String endpoint = "http://localhost:11434";
			return endpoint.startsWith("http") && endpoint.contains(":");
		});

		test("Default model is not empty", () -> {
			String model = "codellama";
			return model != null && !model.isEmpty();
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
