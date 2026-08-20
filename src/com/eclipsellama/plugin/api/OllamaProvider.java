package com.eclipsellama.plugin.api;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.eclipsellama.plugin.core.OllamaClient;

/**
 * Concrete implementation of LlmProvider for the Ollama local server. Delegates
 * all calls to {@link OllamaClient}, which owns the real HTTP transport.
 */
public class OllamaProvider implements LlmProvider {

	private final OllamaClient client;
	private final String model;

	public OllamaProvider(String model) {
		this(new OllamaClient(), model);
	}

	public OllamaProvider(OllamaClient client, String model) {
		this.client = client;
		this.model = model;
	}

	@Override
	public String getProviderName() {
		return "Ollama";
	}

	@Override
	public CompletableFuture<String> generate(String prompt, String contextCode) {
		return client.generateAsync(prompt, model);
	}

	@Override
	public Stream<String> stream(String prompt, String contextCode) {
		// TODO(phase-5-streaming): This implementation is synchronous — it blocks
		// until the full response is received and returns it as a single-element
		// Stream. True token-by-token streaming requires wiring OllamaClient's
		// streamChat callback to a blocking queue and wrapping it in a lazy Stream.
		// Performance impact: the UI thread will not receive any updates until the
		// LLM finishes generating the entire reply.
		return Stream.of(client.generate(prompt, model));
	}
}