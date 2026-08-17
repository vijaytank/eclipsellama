package com.eclipsellama.plugin.api;

/**
 * Utility class implementing an exponential backoff and retry mechanism for any
 * operation that might fail temporarily (e.g., network timeouts, rate
 * limiting).
 * <p>
 * The retry calculation uses an exponential backoff formula: $delay = base *
 * 2^attempt$. This prevents hammering a failing service too quickly.
 */
public class RetryPolicy {

	private final int maxAttempts;
	private final long initialBackoffMs;

	/**
	 * Constructor for configuring retry limits.
	 *
	 * @param maxAttempts      The maximum number of times to try an operation (must
	 *                         be >= 1).
	 * @param initialBackoffMs The base delay in milliseconds.
	 */
	public RetryPolicy(int maxAttempts, long initialBackoffMs) {
		if (maxAttempts < 1) {
			throw new IllegalArgumentException("Max attempts must be at least 1.");
		}
		this.maxAttempts = maxAttempts;
		this.initialBackoffMs = initialBackoffMs;
	}

	/**
	 * Executes a potentially failing operation with automatic retries and backoff.
	 *
	 * @param block The function to execute, accepting the current attempt number.
	 * @param <T>   The return type.
	 * @return The result of the successful execution.
	 * @throws LlmException If the operation fails after all retries.
	 */
	public <T> T execute(java.util.function.Function<Integer, T> block) throws LlmException {
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				// Attempt to run the provided logic
				T result = block.apply(attempt);
				System.out.println("RetryPolicy: Attempt " + attempt + " successful.");
				return result;
			} catch (Exception e) {
				if (attempt == maxAttempts) {
					// Last attempt failed
					throw new LlmException("Operation failed after " + maxAttempts + " attempts.", e);
				}

				// Calculate backoff delay: delay = initialBackoffMs * 2^(attempt - 1)
				long delay = initialBackoffMs * (long) Math.pow(2, attempt - 1);
				System.err.println("RetryPolicy: Attempt " + attempt + " failed. Retrying in " + delay + "ms. Error: "
						+ e.getMessage());

				try {
					Thread.sleep(delay);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new LlmException("Retry interrupted.", ie);
				}
			}
		}
		// This line is unreachable, but satisfies compiler
		throw new LlmException("Retry failed unexpectedly.");
	}
}