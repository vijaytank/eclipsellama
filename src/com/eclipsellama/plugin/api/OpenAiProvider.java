package com.eclipsellama.plugin.api;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.eclipsellama.plugin.core.OpenAIClient;
import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;

/**
 * Concrete implementation of LlmProvider for the OpenAI-compatible backend.
 * Delegates all calls to {@link OpenAIClient}. The API key is resolved lazily
 * from configured preferences; it is not hard-coded here.
 */
public class OpenAiProvider implements LlmProvider {

	private final OpenAIClient client;

	public OpenAiProvider() {
		this(new OpenAIClient());
	}

	public OpenAiProvider(OpenAIClient client) {
		this.client = client;
	}

	@Override
	public String getProviderName() {
		return "OpenAI";
	}

	@Override
	public CompletableFuture<String> generate(String prompt, String contextCode) {
		return client.generateAsync(prompt, EclipseLlamaPreferences.getModel());
	}

	@Override
	public Stream<String> stream(String prompt, String contextCode) {
		// TODO(phase-5-streaming): Same limitation as OllamaProvider — returns the
		// full response as a single Stream element (blocking). OpenAIClient does not
		// yet expose a streaming callback for this provider layer.
		return Stream.of(client.generate(prompt, EclipseLlamaPreferences.getModel()));
	}
}