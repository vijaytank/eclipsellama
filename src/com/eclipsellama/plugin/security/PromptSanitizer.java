package com.eclipsellama.plugin.security;

/**
 * Prompt injection defense — strips zero-width/invisible characters and
 * neutralizes prompt-override attempts before any user input reaches the LLM.
 *
 * Used by all code-action handlers (Explain, Fix, Doc, Test, Refactor, Review,
 * Convert, GenerateCommit) before LLM transmission.
 */
public final class PromptSanitizer {

	private PromptSanitizer() {
		// utility class — no instantiation
	}

	private static final String SANITIZED_OVERRIDE_ATTEMPT = "[SANITIZED_OVERRIDE_ATTEMPT]";

	/**
	 * Sanitizes user input by: 1. Removing zero-width / invisible formatting
	 * characters. 2. Replacing known prompt-override phrases with a visible audit
	 * marker. 3. Normalizing whitespace (trim + collapse multiple spaces).
	 */
	public static String sanitizeUserInput(String input) {
		if (input == null || input.isEmpty()) {
			return input;
		}

		String result = input;

		// 1. Strip zero-width / invisible formatting characters
		// \u200B ZWSP (zero-width space), \u200C ZWNJ, \u200D ZWJ,
		// \uFEFF BOM, \u2060-\u206F word joiners / invisible operators,
		// \u3000 ideographic space (used in injection payloads)
		result = result.replace("\u200B", "").replace("\u200C", "").replace("\u200D", "").replace("\uFEFF", "")
				.replace("\u3000", "");
		for (int cp = 0x2060; cp <= 0x206F; cp++) {
			result = result.replace(String.valueOf((char) cp), "");
		}

		// 2. Neutralize prompt override injection patterns (case-insensitive,
		// regex-based). Replace with audit marker so malicious attempts
		// remain visible in logs/debug output.
		String[] overridePatterns = { "(?i)ignore\\s+(previous|prior|earlier|above|last)\\s+instructions",
				"(?i)system\\s+override", "(?i)you\\s+are\\s+now", "(?i)new\\s+instruction\\s*:",
				"(?i)disregard\\s+(all|previous|prior)\\s+instructions", "(?i)forget\\s+everything\\s+before", };

		for (String pattern : overridePatterns) {
			java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
			java.util.regex.Matcher m = p.matcher(result);
			result = m.replaceAll(SANITIZED_OVERRIDE_ATTEMPT);
		}

		// 3. Normalize whitespace: trim and collapse multiple spaces
		result = result.trim().replaceAll("\\s+", " ");

		return result;
	}
}
