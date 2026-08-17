package com.eclipsellama.plugin.api;

import java.util.concurrent.CompletableFuture;

/**
 * Interface defining the contract for all large language model providers.
 * Implementations must handle making API calls from a standardized manner,
 * allowing the central plugin to route requests regardless of backend (Ollama,
 * OpenAI, etc.).
 */
public interface LlmProvider {

	/**
	 * Asynchronously generates a response based on the provided prompt and context.
	 *
	 * @param prompt      The user's instructional prompt.
	 * @param contextCode The selected source code (can be null).
	 * @return A CompletableFuture resolving to the LLM's generated text response.
	 */
	CompletableFuture<String> generate(String prompt, String contextCode);

	/**
	 * Asynchronously streams the response from the LLM. Implementations must manage
	 * backpressure and close the stream properly upon completion or error.
	 *
	 * @param prompt      The user's instructional prompt.
	 * @param contextCode The selected source code (can be null).
	 * @return A Stream of String chunks representing the full response.
	 */
	java.util.stream.Stream<String> stream(String prompt, String contextCode);

	/**
	 * Returns a unique identifier for this provider implementation (e.g., "ollama",
	 * "openai").
	 */
	String getProviderName();
}