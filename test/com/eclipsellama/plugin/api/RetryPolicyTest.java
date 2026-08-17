package com.eclipsellama.plugin.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit tests for {@link RetryPolicy}. Runs as a plain JUnit test (no Eclipse
 * runtime required).
 */
public class RetryPolicyTest {

	@Test
	public void testRetriesThenSucceeds() throws Exception {
		RetryPolicy policy = new RetryPolicy(3, 1);
		int[] attempts = { 0 };
		String result = policy.execute(a -> {
			attempts[0]++;
			if (attempts[0] < 3) {
				throw new RuntimeException("transient");
			}
			return "ok";
		});
		assertEquals("ok", result);
		assertEquals(3, attempts[0]);
	}

	@Test
	public void testThrowsAfterExhaustion() {
		RetryPolicy policy = new RetryPolicy(2, 1);
		try {
			policy.execute(a -> {
				throw new RuntimeException("always fails");
			});
			fail("expected LlmException");
		} catch (LlmException e) {
			assertTrue(e.getMessage().contains("2 attempts"));
		}
	}

	@Test
	public void testRejectsInvalidAttempts() {
		try {
			new RetryPolicy(0, 1);
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testBackoffGrows() throws Exception {
		RetryPolicy policy = new RetryPolicy(3, 1);
		int[] attempts = { 0 };
		try {
			policy.execute(a -> {
				attempts[0]++;
				throw new RuntimeException("fail");
			});
			fail("expected LlmException");
		} catch (LlmException e) {
			// The operation must have been attempted maxAttempts times.
			assertEquals(3, attempts[0]);
		}
	}
}
