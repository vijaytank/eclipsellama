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
		return Stream.of(client.generate(prompt, model));
	}
}