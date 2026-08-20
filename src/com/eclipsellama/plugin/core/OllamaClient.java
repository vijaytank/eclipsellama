package com.eclipsellama.plugin.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;

import com.eclipsellama.plugin.api.LlmException;
import com.eclipsellama.plugin.api.RetryPolicy;
import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;

public class OllamaClient implements LLMClient {

	private static final long INITIAL_BACKOFF_MS = 500;

	public OllamaClient() {
		// Constructor
	}

	private String getEndpoint() {
		return EclipseLlamaPreferences.getEndpoint();
	}

	private int getConnectTimeoutMs() {
		return 10000; // 10s connect timeout
	}

	private int getReadTimeoutMs() {
		int configured = EclipseLlamaPreferences.getTimeoutSeconds();
		return Math.max(60000, configured * 1000); // at least 60s for model inference / loading
	}

	private int getQuickReadTimeoutMs() {
		return 10000; // 10s for metadata & reachability checks
	}

	private RetryPolicy retryPolicy() {
		return new RetryPolicy(EclipseLlamaPreferences.getRetryAttempts(), INITIAL_BACKOFF_MS);
	}

	@Override
	public boolean isServerReachable() {
		try {
			URL url = java.net.URI.create(getEndpoint() + "/api/tags").toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(getConnectTimeoutMs());
			conn.setReadTimeout(getQuickReadTimeoutMs());
			try {
				conn.connect();
				return conn.getResponseCode() == 200;
			} finally {
				conn.disconnect();
			}
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public String[] getAvailableModels() {
		try {
			return retryPolicy().execute(attempt -> {
				try {
					return doGetModels();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		} catch (Exception e) {
			return new String[0];
		}
	}

	@Override
	public String chat(String prompt, String model) {
		return chat(prompt, model, 0.7f, 2048);
	}

	@Override
	public String chat(String prompt, String model, float temperature, int maxTokens) {
		try {
			String payload = OllamaJsonHelper.buildGenerateRequest(model, prompt, false);
			String response = sendPost(getEndpoint() + "/api/generate", payload);
			return OllamaJsonHelper.parseGenerateResponse(response);
		} catch (Exception e) {
			return "Error: " + e.getMessage();
		}
	}

	@Override
	public void streamChat(List<ChatMessage> messages, String model, Consumer<String> onChunk,
			Consumer<String> onComplete, Consumer<String> onError) {
		streamChat(messages, model, 0.7f, 2048, onChunk, onComplete, onError);
	}

	@Override
	public void streamChat(List<ChatMessage> messages, String model, float temperature, int maxTokens,
			Consumer<String> onChunk, Consumer<String> onComplete, Consumer<String> onError) {
		CompletableFuture.runAsync(() -> {
			StringBuilder fullResponse = new StringBuilder();
			HttpURLConnection conn = null;

			try {
				JSONArray jsonMessages = new JSONArray();
				for (ChatMessage msg : messages) {
					jsonMessages.put(OllamaJsonHelper.buildChatMessage(msg.getRole(), msg.getContent()));
				}

				String payload = OllamaJsonHelper.buildChatRequest(model, jsonMessages, true);

				URL url = java.net.URI.create(getEndpoint() + "/api/chat").toURL();
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setRequestProperty("Accept", "application/json");
				conn.setConnectTimeout(getConnectTimeoutMs());
				conn.setReadTimeout(getReadTimeoutMs());
				conn.setDoOutput(true);

				try (OutputStream os = conn.getOutputStream()) {
					os.write(payload.getBytes(StandardCharsets.UTF_8));
					os.flush();
				}

				InputStream stream = (conn.getResponseCode() >= 400) ? conn.getErrorStream() : conn.getInputStream();

				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(stream, StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						String chunk = OllamaJsonHelper.parseChatChunk(line);
						if (!chunk.isEmpty()) {
							fullResponse.append(chunk);
							final String c = chunk;
							Display.getDefault().asyncExec(() -> onChunk.accept(c));
						}
					}
				}

				final String result = fullResponse.toString();
				Display.getDefault().asyncExec(() -> onComplete.accept(result));

			} catch (Exception e) {
				final String error = e.getMessage();
				Display.getDefault().asyncExec(() -> onError.accept(error));
			} finally {
				if (conn != null) {
					conn.disconnect();
				}
			}
		});
	}

	@Override
	public String generate(String prompt, String model) {
		try {
			String payload = OllamaJsonHelper.buildGenerateRequest(model, prompt, false);
			String response = sendPost(getEndpoint() + "/api/generate", payload);
			return OllamaJsonHelper.parseGenerateResponse(response);
		} catch (Exception e) {
			return "Error: " + e.getMessage();
		}
	}

	@Override
	public CompletableFuture<String> generateAsync(String prompt, String model) {
		return CompletableFuture.supplyAsync(() -> generate(prompt, model));
	}

	// --- Helper Methods ---

	private String sendPost(String endpoint, String payload) throws IOException {
		try {
			return retryPolicy().execute(attempt -> {
				try {
					return doPost(endpoint, payload);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (LlmException e) {
			Throwable cause = e.getCause();
			if (cause instanceof IOException) {
				throw (IOException) cause;
			}
			throw new IOException(e.getMessage(), cause);
		}
	}

	private String[] doGetModels() throws IOException {
		URL url = java.net.URI.create(getEndpoint() + "/api/tags").toURL();
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(getConnectTimeoutMs());
		conn.setReadTimeout(getQuickReadTimeoutMs());
		try {
			conn.connect();
			if (conn.getResponseCode() != 200) {
				throw new IOException("Ollama /api/tags returned HTTP " + conn.getResponseCode());
			}
			String response = readResponse(conn.getInputStream());
			return OllamaJsonHelper.parseModelList(response);
		} finally {
			conn.disconnect();
		}
	}

	private String doPost(String endpoint, String payload) throws IOException {
		URL url = java.net.URI.create(endpoint).toURL();
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Accept", "application/json");
		conn.setConnectTimeout(getConnectTimeoutMs());
		conn.setReadTimeout(getReadTimeoutMs());
		conn.setDoOutput(true);

		try {
			try (OutputStream os = conn.getOutputStream()) {
				os.write(payload.getBytes(StandardCharsets.UTF_8));
				os.flush();
			}
			InputStream stream = (conn.getResponseCode() >= 400) ? conn.getErrorStream() : conn.getInputStream();
			return readResponse(stream);
		} finally {
			conn.disconnect();
		}
	}

	private String readResponse(InputStream stream) throws IOException {
		if (stream == null) {
			return "";
		}
		StringBuilder response = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			char[] buffer = new char[8192];
			int len;
			while ((len = reader.read(buffer)) != -1) {
				response.append(buffer, 0, len);
			}
		}
		return response.toString();
	}
}
