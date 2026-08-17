package com.eclipsellama.plugin.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit tests for {@link McpServerRegistry}. Runs as a plain JUnit test.
 */
public class McpServerRegistryTest {

	private static McpServerConfig cfg(String id) {
		McpServerConfig c = new McpServerConfig();
		c.setTransport("stdio");
		c.setCommand("echo");
		return c;
	}

	@Test
	public void testRegisterAndGet() {
		McpServerRegistry reg = new McpServerRegistry();
		McpServerConfig c = cfg("x");
		reg.register(c);
		assertTrue(reg.get(c.getId()).isPresent());
		assertEquals(1, reg.size());
	}

	@Test
	public void testReplaceById() {
		McpServerRegistry reg = new McpServerRegistry();
		McpServerConfig c = cfg("x");
		reg.register(c);
		reg.register(c); // same id
		assertEquals(1, reg.size());
	}

	@Test
	public void testUnregister() {
		McpServerRegistry reg = new McpServerRegistry();
		McpServerConfig c = cfg("x");
		reg.register(c);
		reg.unregister(c.getId());
		assertEquals(0, reg.size());
	}

	@Test
	public void testGetAllUnmodifiable() {
		McpServerRegistry reg = new McpServerRegistry();
		reg.register(cfg("x"));
		try {
			reg.getAll().clear();
			fail("expected UnsupportedOperationException");
		} catch (UnsupportedOperationException e) {
			// expected
		}
	}

	@Test
	public void testNullRejected() {
		McpServerRegistry reg = new McpServerRegistry();
		try {
			reg.register(null);
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
}
