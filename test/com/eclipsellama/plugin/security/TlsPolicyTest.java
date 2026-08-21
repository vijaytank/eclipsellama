package com.eclipsellama.plugin.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;

/**
 * Plain-JUnit tests for {@link TlsPolicy} TLS-enforcement logic.
 */
public class TlsPolicyTest {

	private static URL url(String u) {
		try {
			return new URI(u).toURL();
		} catch (MalformedURLException | URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Test
	public void testHttpsAlwaysPermittedEvenWhenEnforced() {
		assertTrue(TlsPolicy.isPermitted(url("https://api.openai.com/v1"), true));
		assertTrue(TlsPolicy.isPermitted(url("https://example.com"), true));
	}

	@Test
	public void testHttpAllowedWhenNotEnforced() {
		assertTrue(TlsPolicy.isPermitted(url("http://example.com"), false));
	}

	@Test
	public void testRemoteHttpRejectedWhenEnforced() {
		assertFalse(TlsPolicy.isPermitted(url("http://example.com/v1"), true));
		assertFalse(TlsPolicy.isPermitted(url("http://api.remote.dev"), true));
	}

	@Test
	public void testLocalhostAllowedOverPlainHttpWhenEnforced() {
		assertTrue(TlsPolicy.isPermitted(url("http://localhost:11434"), true));
		assertTrue(TlsPolicy.isPermitted(url("http://127.0.0.1:8080"), true));
	}

	@Test
	public void testPrivateLanAllowedWhenEnforced() {
		assertTrue(TlsPolicy.isPermitted(url("http://192.168.1.10:11434"), true));
		assertTrue(TlsPolicy.isPermitted(url("http://10.0.0.25"), true));
		assertTrue(TlsPolicy.isPermitted(url("http://172.16.5.1"), true));
	}

	@Test
	public void testReservedBlockOutsidePrivateRangeRejectedWhenEnforced() {
		// 172.32.x.x is NOT in the RFC1918 172.16-31 block
		assertFalse(TlsPolicy.isPermitted(url("http://172.32.1.1"), true));
	}

	@Test
	public void testBadSchemeRejected() {
		assertFalse(TlsPolicy.isPermitted(url("ftp://localhost/x"), true));
	}
}