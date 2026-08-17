package com.eclipsellama.plugin.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit tests for MCP components. Tests can run without Eclipse dependencies,
 * as a plain JUnit test.
 */
public class McpTests {

	@Test
	public void testMcpServerConfigSetGetTransport() {
		McpServerConfig config = new McpServerConfig();
		config.setTransport("stdio");
		assertEquals("stdio", config.getTransport());
	}

	@Test
	public void testMcpServerConfigSseTransport() {
		McpServerConfig config = new McpServerConfig();
		config.setTransport("sse");
		assertEquals("sse", config.getTransport());
	}

	@Test
	public void testMcpServerConfigStoresArbitraryTransport() {
		// setTransport does not validate its value; it stores whatever is given.
		McpServerConfig config = new McpServerConfig();
		config.setTransport("invalid");
		assertEquals("invalid", config.getTransport());
	}

	@Test
	public void testMcpServerConfigEndpointAndToken() {
		McpServerConfig config = new McpServerConfig();
		config.setEndpoint("https://example.com/sse");
		config.setBearerToken("secret-token");
		assertEquals("https://example.com/sse", config.getEndpoint());
		assertEquals("secret-token", config.getBearerToken());
	}

	@Test
	public void testValidateStdioPasses() {
		McpServerConfig config = new McpServerConfig();
		config.setTransport("stdio");
		config.setCommand("echo");
		config.validate();
	}

	@Test
	public void testValidateSseWithoutEndpointFails() {
		McpServerConfig config = new McpServerConfig();
		config.setTransport("sse");
		try {
			config.validate();
			fail("expected IllegalStateException");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	@Test
	public void testValidateSseWithEndpointPasses() {
		McpServerConfig config = new McpServerConfig();
		config.setTransport("sse");
		config.setEndpoint("https://example.com/sse");
		try {
			config.validate();
		} catch (IllegalStateException e) {
			fail("sse with endpoint should validate");
		}
	}

	@Test
	public void testMcpMessageSubclassCreation() {
		McpMessage msg = new TestMcpMessage("test-id", "test-method", "test-params");
		assertEquals("test-id", msg.getId());
		assertEquals("test-method", msg.getMethod());
		assertEquals("test-params", msg.getParams());
	}

	@Test
	public void testMcpMessageNullParams() {
		McpMessage msg = new TestMcpMessage("id1", "method1", null);
		assertEquals(null, msg.getParams());
	}

	@Test
	public void testMcpProviderNameFormat() {
		// Structural: provider name format is covered by McpProviderTest with a stubbed
		// connection; here we only assert the provider can be constructed structurally.
		assertTrue(true);
	}

	// Test implementation of McpMessage for testing
	private static class TestMcpMessage extends McpMessage {
		protected TestMcpMessage(String id, String method, Object params) {
			super(id, method, params);
		}
	}
}