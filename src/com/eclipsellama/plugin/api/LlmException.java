package com.eclipsellama.plugin.api;

/**
 * Custom exception used throughout the EclipseLlama plugin for handling
 * provider-specific or communication-related failures that are not standard
 * IO/network exceptions.
 *
 * This allows handlers to catch a highly specific exception type and manage
 * retries or fallbacks gracefully.
 */
public class LlmException extends Exception {

	private static final long serialVersionUID = 1L;

	public LlmException(String message) {
		super(message);
	}

	public LlmException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Indicates whether the exception represents a transient failure that should be
	 * retried. Currently all LlmExceptions are considered transient.
	 */
	public boolean isTransient() {
		// In a full implementation this could inspect the exception cause
		// to determine if it is a transient condition (e.g., timeout, rate-limit).
		return true;
	}
}