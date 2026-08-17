package com.eclipsellama.plugin.mcp;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

/**
 * Unit tests for {@link McpProvider} that verify the JSON-RPC request is sent
 * over a (stubbed) connection and the server's response is returned verbatim.
 * Uses a stub {@link McpConnectionManager} so no real process/network is
 * involved. Runs as a plain JUnit test.
 */
public class McpProviderTest {

	private static McpServerConfig stdioConfig() {
		McpServerConfig c = new McpServerConfig();
		c.setTransport("stdio");
		c.setCommand("echo");
		return c;
	}

	private static class FakeConnection implements McpConnection {
		private final String response;
		private String written;

		FakeConnection(String response) {
			this.response = response;
		}

		@Override
		public String readMessage() throws IOException {
			return response;
		}

		@Override
		public void writeMessage(String message) throws IOException {
			this.written = message;
		}

		@Override
		public void close() throws IOException {
		}
	}

	private static class FakeManager extends McpConnectionManager {
		private final FakeConnection conn;

		FakeManager(FakeConnection conn) {
			this.conn = conn;
		}

		@Override
		public McpConnection getOrCreate(McpServerConfig config) throws IOException {
			return conn;
		}
	}

	@Test
	public void testGenerateRoundTrip() throws Exception {
		FakeConnection conn = new FakeConnection("{\"result\":\"hello\"}");
		McpProvider provider = new McpProvider(stdioConfig(), new FakeManager(conn));
		String response = provider.generate("hi", null).get();
		assertTrue(conn.written != null && conn.written.contains("hi"));
		assertTrue(conn.written != null && conn.written.contains("tools/call"));
		assertTrue("{\"result\":\"hello\"}".equals(response));
	}

	@Test
	public void testConnectionFailureWrapsLlmException() throws Exception {
		McpConnectionManager failing = new McpConnectionManager() {
			@Override
			public McpConnection getOrCreate(McpServerConfig config) throws IOException {
				throw new IOException("boom");
			}
		};
		McpProvider provider = new McpProvider(stdioConfig(), failing);
		CompletableFuture<String> f = provider.generate("hi", null);
		try {
			f.get();
			assertTrue("expected failure", false);
		} catch (java.util.concurrent.ExecutionException e) {
			assertTrue(e.getCause() instanceof RuntimeException);
		}
	}

	@Test
	public void testProviderName() {
		McpProvider provider = new McpProvider(stdioConfig(), new FakeManager(new FakeConnection("")));
		assertTrue(provider.getProviderName().contains("stdio"));
	}
}
