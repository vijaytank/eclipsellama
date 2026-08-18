package com.eclipsellama.plugin.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.junit.Test;

import com.eclipsellama.plugin.core.ChatMessage;
import com.eclipsellama.plugin.core.LLMClient;
import com.eclipsellama.plugin.search.WebSearchRouter.SearchDecision;

/**
 * Unit tests for {@link WebSearchRouter}. Uses a stub {@link LLMClient} that
 * returns a canned planner response.
 */
public class WebSearchRouterTest {

	private static LLMClient stubClient(String response) {
		return new LLMClient() {
			@Override
			public boolean isServerReachable() {
				return true;
			}

			@Override
			public String[] getAvailableModels() {
				return new String[0];
			}

			@Override
			public String chat(String prompt, String model) {
				return response;
			}

			@Override
			public String chat(String prompt, String model, float temperature, int maxTokens) {
				return response;
			}

			@Override
			public void streamChat(List<ChatMessage> messages, String model, Consumer<String> onChunk,
					Consumer<String> onComplete, Consumer<String> onError) {
			}

			@Override
			public void streamChat(List<ChatMessage> messages, String model, float temperature, int maxTokens,
					Consumer<String> onChunk, Consumer<String> onComplete, Consumer<String> onError) {
			}

			@Override
			public String generate(String prompt, String model) {
				return response;
			}

			@Override
			public CompletableFuture<String> generateAsync(String prompt, String model) {
				return CompletableFuture.completedFuture(response);
			}
		};
	}

	@Test
	public void testAnswerDecision() {
		WebSearchRouter router = new WebSearchRouter(stubClient("{\"action\":\"answer\"}"));
		SearchDecision d = router.decide("hi", "model");
		assertFalse(d.isSearch());
	}

	@Test
	public void testWebSearchDecision() {
		WebSearchRouter router = new WebSearchRouter(
				stubClient("{\"action\":\"web_search\",\"query\":\"latest Qwen\"}"));
		SearchDecision d = router.decide("What is the newest Qwen?", "model");
		assertTrue(d.isSearch());
		assertEquals("latest Qwen", d.getQuery());
		assertEquals(5, d.getMaxResults());
	}

	@Test
	public void testMalformedJsonFailsClosedToAnswer() {
		WebSearchRouter router = new WebSearchRouter(stubClient("not json"));
		SearchDecision d = router.decide("question", "model");
		assertFalse(d.isSearch());
	}

	@Test
	public void testEmptyQueryFailsClosed() {
		WebSearchRouter router = new WebSearchRouter(stubClient("{\"action\":\"web_search\",\"query\":\"   \"}"));
		SearchDecision d = router.decide("question", "model");
		assertFalse(d.isSearch());
	}

	@Test
	public void testExceptionFailsClosed() {
		LLMClient broken = new LLMClient() {
			@Override
			public String chat(String prompt, String model, float temperature, int maxTokens) {
				throw new RuntimeException("boom");
			}

			@Override
			public boolean isServerReachable() {
				return false;
			}

			@Override
			public String[] getAvailableModels() {
				return new String[0];
			}

			@Override
			public String chat(String prompt, String model) {
				throw new RuntimeException("boom");
			}

			@Override
			public void streamChat(List<ChatMessage> messages, String model, Consumer<String> onChunk,
					Consumer<String> onComplete, Consumer<String> onError) {
			}

			@Override
			public void streamChat(List<ChatMessage> messages, String model, float temperature, int maxTokens,
					Consumer<String> onChunk, Consumer<String> onComplete, Consumer<String> onError) {
			}

			@Override
			public String generate(String prompt, String model) {
				throw new RuntimeException("boom");
			}

			@Override
			public CompletableFuture<String> generateAsync(String prompt, String model) {
				throw new RuntimeException("boom");
			}
		};
		WebSearchRouter router = new WebSearchRouter(broken);
		SearchDecision d = router.decide("question", "model");
		assertFalse(d.isSearch());
	}
}
