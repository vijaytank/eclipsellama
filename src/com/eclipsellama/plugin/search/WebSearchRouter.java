package com.eclipsellama.plugin.search;

import org.json.JSONObject;

import com.eclipsellama.plugin.core.LLMClient;

/**
 * Model-driven web-search router. Uses a short, constrained LLM "planner pass"
 * to decide whether the current user message needs web search, instead of
 * keyword matching. Works for any backend (OpenAI or Ollama) because it only
 * relies on plain text completion, not native tool calling.
 *
 * <p>
 * The planner returns exactly one JSON object:
 *
 * <pre>
 * {"action":"answer"}
 * </pre>
 *
 * or
 *
 * <pre>
 * {"action":"web_search","query":"..."}
 * </pre>
 *
 * If parsing fails, times out, or the action is invalid, the router fails
 * closed to {@code answer} (no search).
 */
public final class WebSearchRouter {

	private static final int MAX_QUERY_LENGTH = 300;
	private static final int MAX_RESULTS = 10;

	private static final String PLANNER_PROMPT = """
			You are the tool-use planner for EclipseLlama.

			You may either:
			- answer: continue without external web information, or
			- web_search: retrieve web information before answering.

			Choose web_search only if the user request cannot be answered reliably
			from the conversation and supplied IDE/workspace context alone, and
			current or externally verifiable information would materially improve
			the answer.

			Return exactly one JSON object matching this schema:
			{"action":"answer"}
			or
			{"action":"web_search","query":"a concise search query"}

			Do not search just because search is available.
			Do not search for greetings, casual conversation, transformations of
			user-provided text, coding/debugging based on supplied code, or general
			timeless explanations.

			User request:
			%s
			""";

	private final LLMClient llmClient;

	public WebSearchRouter(LLMClient llmClient) {
		this.llmClient = llmClient;
	}

	/**
	 * Runs the planner pass and returns the decision. Never returns null; on any
	 * failure the decision is {@code answer}.
	 */
	public SearchDecision decide(String userMessage, String model) {
		if (userMessage == null || userMessage.isBlank()) {
			return SearchDecision.answer();
		}
		try {
			String prompt = PLANNER_PROMPT.formatted(userMessage);
			String raw = llmClient.chat(prompt, model, 0.2f, 128);
			return parse(raw);
		} catch (Exception e) {
			// Fail closed: never search when the planner is unreliable.
			return SearchDecision.answer();
		}
	}

	private static SearchDecision parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return SearchDecision.answer();
		}
		String trimmed = raw.trim();
		int start = trimmed.indexOf('{');
		int end = trimmed.lastIndexOf('}');
		if (start < 0 || end <= start) {
			return SearchDecision.answer();
		}
		try {
			JSONObject obj = new JSONObject(trimmed.substring(start, end + 1));
			String action = obj.optString("action", "");
			if ("web_search".equals(action)) {
				String query = obj.optString("query", "").trim();
				if (query.isEmpty() || query.length() > MAX_QUERY_LENGTH) {
					return SearchDecision.answer();
				}
				int max = Math.max(1, Math.min(MAX_RESULTS, obj.optInt("max_results", 5)));
				return SearchDecision.webSearch(query, max);
			}
			return SearchDecision.answer();
		} catch (Exception e) {
			return SearchDecision.answer();
		}
	}

	/** Result of a planner decision. */
	public static final class SearchDecision {
		private final boolean doSearch;
		private final String query;
		private final int maxResults;

		private SearchDecision(boolean doSearch, String query, int maxResults) {
			this.doSearch = doSearch;
			this.query = query;
			this.maxResults = maxResults;
		}

		public static SearchDecision answer() {
			return new SearchDecision(false, null, 0);
		}

		public static SearchDecision webSearch(String query, int maxResults) {
			return new SearchDecision(true, query, maxResults);
		}

		public boolean isSearch() {
			return doSearch;
		}

		public String getQuery() {
			return query;
		}

		public int getMaxResults() {
			return maxResults;
		}
	}
}
