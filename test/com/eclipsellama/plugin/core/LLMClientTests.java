package com.eclipsellama.plugin.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Unit tests for LLM Client logic (JSON helpers, etc). Can run without Eclipse
 * dependencies, as a plain JUnit test.
 */
public class LLMClientTests {

	@Test
	public void testOllamaBuildChatRequest() {
		JSONArray messages = new JSONArray();
		messages.put(OllamaJsonHelper.buildChatMessage("user", "hi"));
		String json = OllamaJsonHelper.buildChatRequest("llama2", messages, true);
		JSONObject obj = new JSONObject(json);
		assertEquals("llama2", obj.getString("model"));
		assertTrue(obj.getBoolean("stream"));
	}

	@Test
	public void testOllamaParseChatChunk() {
		String json = "{\"message\":{\"role\":\"assistant\",\"content\":\"hello\"},\"done\":false}";
		assertEquals("hello", OllamaJsonHelper.parseChatChunk(json));
	}

	@Test
	public void testOllamaIsDone() {
		assertTrue(OllamaJsonHelper.isDone("{\"done\":true}"));
	}

	@Test
	public void testOpenAIBuildChatRequest() {
		JSONArray messages = new JSONArray();
		messages.put(OpenAIJsonHelper.buildChatMessage("user", "hi"));
		String json = OpenAIJsonHelper.buildOpenAIChatRequest("gpt-3.5-turbo", messages, true);
		JSONObject obj = new JSONObject(json);
		assertEquals("gpt-3.5-turbo", obj.getString("model"));
		assertTrue(obj.getBoolean("stream"));
	}

	@Test
	public void testOpenAIParseChunkSse() {
		String sse = "data: {\"choices\":[{\"delta\":{\"content\":\"hello\"}}]}";
		assertEquals("hello", OpenAIJsonHelper.parseOpenAIChatChunk(sse));
	}

	@Test
	public void testOpenAIParseChunkDoneMarker() {
		assertEquals(null, OpenAIJsonHelper.parseOpenAIChatChunk("data: [DONE]"));
	}

	@Test
	public void testOpenAIIsDone() {
		assertTrue(OpenAIJsonHelper.isOpenAIDone("data: [DONE]") || OpenAIJsonHelper.isOpenAIDone("[DONE]"));
	}
}
