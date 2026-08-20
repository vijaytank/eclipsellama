package com.eclipsellama.plugin.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit tests for MCP components. Tests can run without Eclipse dependencies, as
 * a plain JUnit test.
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
		McpMessage msg = new StubMcpMessage("test-id", "test-method", "test-params");
		assertEquals("test-id", msg.getId());
		assertEquals("test-method", msg.getMethod());
		assertEquals("test-params", msg.getParams());
	}

	@Test
	public void testMcpMessageNullParams() {
		McpMessage msg = new StubMcpMessage("id1", "method1", null);
		assertEquals(null, msg.getParams());
	}

	@Test
	public void testMcpServerConfigName() {
		McpServerConfig config = new McpServerConfig();
		config.setName("nakshastramcp");
		assertEquals("nakshastramcp", config.getName());
	}

	@Test
	public void testMcpServerConfigTools() {
		McpServerConfig config = new McpServerConfig();
		config.setTools(java.util.List.of("ping", "search_codebase", "read_file"));
		assertEquals(3, config.getTools().size());
		assertTrue(config.getTools().contains("search_codebase"));
	}

	@Test
	public void testMcpProviderNameFormat() {
		McpServerConfig config = new McpServerConfig();
		config.setName("my-custom-mcp");
		McpProvider provider = new McpProvider(config, new McpConnectionManager());
		assertEquals("my-custom-mcp", provider.getProviderName());
	}

	@Test
	public void testMcpServerStoreSaveAndLoad() {
		java.util.List<McpServerConfig> list = new java.util.ArrayList<>();
		McpServerConfig c1 = new McpServerConfig();
		c1.setName("nakshastramcp");
		c1.setTransport("http-streamable");
		c1.setEndpoint("http://127.0.0.1:2102/mcp");
		c1.setTools(java.util.List.of("ping", "search_codebase", "read_file"));
		list.add(c1);

		com.eclipsellama.plugin.preferences.McpServerStore.save(list);
		java.util.List<McpServerConfig> loaded = com.eclipsellama.plugin.preferences.McpServerStore.load();
		assertTrue(loaded.size() >= 1);
		boolean found = loaded.stream()
				.anyMatch(s -> "nakshastramcp".equals(s.getName()) && s.getTools().contains("search_codebase"));
		assertTrue(found);
	}

	// Stub implementation of McpMessage for testing
	private static class StubMcpMessage extends McpMessage {
		protected StubMcpMessage(String id, String method, Object params) {
			super(id, method, params);
		}
	}
}