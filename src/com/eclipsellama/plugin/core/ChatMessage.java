package com.eclipsellama.plugin.core;

/**
 * Represents a chat message with role and content.
 */
public class ChatMessage {

	public static final String ROLE_SYSTEM = "system";
	public static final String ROLE_USER = "user";
	public static final String ROLE_ASSISTANT = "assistant";

	private final String role;
	private final String content;
	private final long timestamp;

	public ChatMessage(String role, String content) {
		this.role = role;
		this.content = content;
		this.timestamp = System.currentTimeMillis();
	}

	public String getRole() {
		return role;
	}

	public String getContent() {
		return content;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public boolean isUser() {
		return ROLE_USER.equals(role);
	}

	public boolean isAssistant() {
		return ROLE_ASSISTANT.equals(role);
	}

	public boolean isSystem() {
		return ROLE_SYSTEM.equals(role);
	}

	/**
	 * Factory method for user messages.
	 */
	public static ChatMessage user(String content) {
		return new ChatMessage(ROLE_USER, content);
	}

	/**
	 * Factory method for assistant messages.
	 */
	public static ChatMessage assistant(String content) {
		return new ChatMessage(ROLE_ASSISTANT, content);
	}

	/**
	 * Factory method for system messages.
	 */
	public static ChatMessage system(String content) {
		return new ChatMessage(ROLE_SYSTEM, content);
	}
}
