package com.eclipsellama.plugin.mcp;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

/**
 * Unit tests for {@link McpConnectionManager} validation/error paths that do
 * not require launching a real process or network. Runs as a plain JUnit test.
 */
public class McpConnectionManagerTest {

	@Test
	public void testUnsupportedTransport() {
		McpServerConfig c = new McpServerConfig();
		c.setTransport("unknown");
		try {
			new McpConnectionManager().getOrCreate(c);
			fail("expected IOException");
		} catch (IOException e) {
			assertTrue(e.getMessage().contains("Unsupported transport"));
		}
	}

	@Test
	public void testSseWithoutEndpoint() {
		McpServerConfig c = new McpServerConfig();
		c.setTransport("sse");
		try {
			new McpConnectionManager().getOrCreate(c);
			fail("expected IOException");
		} catch (IOException e) {
			assertTrue(e.getMessage().contains("Endpoint"));
		}
	}

	@Test
	public void testNullTransport() {
		McpServerConfig c = new McpServerConfig();
		try {
			new McpConnectionManager().getOrCreate(c);
			fail("expected IOException");
		} catch (IOException e) {
			assertTrue(e.getMessage().contains("Transport"));
		}
	}

	@Test
	public void testCloseUnknownIsNoop() {
		McpConnectionManager m = new McpConnectionManager();
		m.close("nonexistent"); // should not throw
	}
}
