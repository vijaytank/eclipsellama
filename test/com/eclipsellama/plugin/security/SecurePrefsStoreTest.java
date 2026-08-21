package com.eclipsellama.plugin.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SecurePrefsStore} using an in-memory backend, so they
 * run as a plain JUnit test without the Eclipse OSGi secure store.
 */
public class SecurePrefsStoreTest {

	private SecurePrefsStore.InMemoryStoreBackend fake;

	@Before
	public void setUp() {
		fake = new SecurePrefsStore.InMemoryStoreBackend();
		SecurePrefsStore.setBackendForTest(fake);
	}

	@After
	public void tearDown() {
		SecurePrefsStore.setBackendForTest(null);
	}

	@Test
	public void testPutAndGetRoundtrip() {
		SecurePrefsStore.securePutEncrypted("api/openai", "apiKey", "sk-test-123");
		assertTrue("key should be stored", fake.contains("api/openai", "apiKey"));
		assertEquals("sk-test-123", SecurePrefsStore.secureGetOrDefault("api/openai", "apiKey", ""));
	}

	@Test
	public void testMissingKeyReturnsFallback() {
		assertEquals("fallback", SecurePrefsStore.secureGetOrDefault("api/openai", "missing", "fallback"));
	}

	@Test
	public void testRemoveKeyClearsValue() {
		SecurePrefsStore.securePutEncrypted("mcp/myServer", "bearerToken", "tok-abc");
		SecurePrefsStore.secureRemove("mcp/myServer", "bearerToken");
		assertFalse("token should be removed", fake.contains("mcp/myServer", "bearerToken"));
	}

	@Test
	public void testSecurePutNullIsNoOp() {
		SecurePrefsStore.securePutEncrypted("api/openai", "apiKey", null);
		assertFalse("null must not be stored", fake.contains("api/openai", "apiKey"));
	}

	@Test
	public void testIsAvailableOnFakeBackend() {
		assertTrue(SecurePrefsStore.isAvailable());
	}
}