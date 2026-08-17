package com.eclipsellama.plugin.git;

import com.eclipsellama.plugin.core.ClientProvider;
import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;

/**
 * Generates commit messages from git diff using AI.
 */
public class CommitMessageGenerator {

	// Debug mode - set to true for development only
	private static final boolean DEBUG_MODE = false;

	// Stores debug info for display
	private static String lastRawResponse = "";
	private static String lastModel = "";

	/**
	 * Generate a commit message from the diff.
	 */
	public static String generate(String diff) {
		if (diff == null || diff.trim().isEmpty()) {
			lastRawResponse = "(no diff provided)";
			return "chore: update files";
		}

		// Truncate very long diffs
		String truncatedDiff = diff.length() > 3000 ? diff.substring(0, 3000) + "\n..." : diff;

		// Simple prompt
		String prompt = "Git commit message for this diff (format: type: description):\n\n" + truncatedDiff
				+ "\n\nCommit message:";

		try {
			lastModel = EclipseLlamaPreferences.getModel();
			String result = ClientProvider.getClient().generate(prompt, lastModel);
			lastRawResponse = result;

			// Clean up the response
			result = cleanResponse(result);

			return result.isEmpty() ? "chore: update files" : result;

		} catch (Exception e) {
			lastRawResponse = "ERROR: " + e.getMessage();
			return "chore: update files";
		}
	}

	/**
	 * Get debug info for display. TODO: Remove before publishing!
	 */
	public static String getDebugInfo() {
		if (!DEBUG_MODE) {
			return null;
		}
		return "Model: " + lastModel + "\n\n" + "Raw Response:\n" + lastRawResponse;
	}

	/**
	 * Clean the AI response to extract just the commit message.
	 */
	private static String cleanResponse(String response) {
		if (response == null || response.isEmpty()) {
			return "";
		}

		String result = response.trim();

		// Take first line only
		int newline = result.indexOf('\n');
		if (newline > 0) {
			result = result.substring(0, newline).trim();
		}

		// Remove common prefixes (case insensitive)
		String lower = result.toLowerCase();
		String[] prefixes = { "commit:", "message:", "commit message:", "git commit:", "```", "sure,", "here's" };
		for (String prefix : prefixes) {
			if (lower.startsWith(prefix)) {
				result = result.substring(prefix.length()).trim();
				lower = result.toLowerCase();
			}
		}

		// Remove quotes and backticks
		result = result.replace("`", "").replace("\"", "").replace("'", "");

		// Fix "type: actual content" where type is literally "type"
		if (lower.startsWith("type:")) {
			result = "chore:" + result.substring(5);
		}

		// Remove parenthetical explanations like "(because...)" or "(since...)"
		int parenStart = result.indexOf(" (");
		if (parenStart > 5) {
			result = result.substring(0, parenStart).trim();
		}

		// If still invalid, search for valid line in response
		if (!isValidFormat(result)) {
			String[] lines = response.split("\n");
			for (String line : lines) {
				line = line.trim().replace("`", "");
				if (isValidFormat(line)) {
					int paren = line.indexOf(" (");
					if (paren > 5) {
						line = line.substring(0, paren).trim();
					}
					return line;
				}
			}
			return "chore: update files";
		}

		return result;
	}

	/**
	 * Check if message has valid conventional commit format.
	 */
	private static boolean isValidFormat(String msg) {
		if (msg == null || msg.length() < 8 || msg.length() > 100) {
			return false;
		}

		String lower = msg.toLowerCase();
		String[] types = { "feat:", "fix:", "docs:", "style:", "refactor:", "test:", "chore:", "build:", "ci:",
				"perf:" };

		for (String type : types) {
			if (lower.startsWith(type)) {
				return true;
			}
		}

		// Also check with scope: feat(scope):
		if (lower.matches("^(feat|fix|docs|style|refactor|test|chore|build|ci|perf)\\([^)]+\\):.*")) {
			return true;
		}

		return false;
	}

	/**
	 * Generate async and call the callback with result.
	 */
	public static void generateAsync(String diff, java.util.function.Consumer<String> callback) {
		new Thread(() -> {
			String message = generate(diff);
			callback.accept(message);
		}, "EclipseLlama-CommitGen").start();
	}
}
