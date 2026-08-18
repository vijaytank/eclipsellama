package com.eclipsellama.plugin.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.eclipsellama.plugin.core.ChatMessage;

/**
 * Unit tests for {@link ChatHistoryStore} using a temp file. Runs as a plain
 * JUnit test (no Eclipse runtime required).
 */
public class ChatHistoryStoreTest {

	@Test
	public void testRoundTrip() throws Exception {
		Path temp = Files.createTempDirectory("history-test").resolve("history.json");
		ChatHistoryStore store = new ChatHistoryStore(temp);

		List<ChatMessage> messages = new ArrayList<>();
		messages.add(ChatMessage.system("system prompt"));
		messages.add(ChatMessage.user("hello"));
		messages.add(ChatMessage.assistant("hi there"));
		store.save(messages);

		List<ChatMessage> loaded = store.load();
		assertEquals(2, loaded.size());
		assertEquals("hello", loaded.get(0).getContent());
		assertTrue(loaded.get(0).isUser());
		assertEquals("hi there", loaded.get(1).getContent());
		assertTrue(loaded.get(1).isAssistant());
	}

	@Test
	public void testLoadEmptyWhenMissing() throws Exception {
		Path temp = Files.createTempDirectory("history-test").resolve("nope.json");
		ChatHistoryStore store = new ChatHistoryStore(temp);
		assertTrue(store.load().isEmpty());
	}
}
