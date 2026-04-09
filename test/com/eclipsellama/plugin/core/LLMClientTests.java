package com.eclipsellama.plugin.core;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Unit tests for LLM Client logic (JSON helpers, etc).
 * Can run without Eclipse dependencies.
 */
public class LLMClientTests {

    private static int totalPassed = 0;
    private static int totalFailed = 0;

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║     LLM Client Logic Tests             ║");
        System.out.println("╚════════════════════════════════════════╝\n");

        testOllamaJsonHelper();
        testOpenAIJsonHelper();

        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║  Results: " + totalPassed + " passed, " + totalFailed + " failed");
        System.out.println("╚════════════════════════════════════════╝");

        if (totalFailed > 0) {
            System.exit(1);
        }
    }

    private static void testOllamaJsonHelper() {
        System.out.println("━━━ Ollama JSON Helper Tests ━━━");

        test("Ollama buildChatRequest", () -> {
            JSONArray messages = new JSONArray();
            messages.put(OllamaJsonHelper.buildChatMessage("user", "hi"));
            String json = OllamaJsonHelper.buildChatRequest("llama2", messages, true);
            JSONObject obj = new JSONObject(json);
            return "llama2".equals(obj.getString("model")) && obj.getBoolean("stream");
        });

        test("Ollama parseChatChunk", () -> {
            String json = "{\"message\":{\"role\":\"assistant\",\"content\":\"hello\"},\"done\":false}";
            return "hello".equals(OllamaJsonHelper.parseChatChunk(json));
        });
        
        test("Ollama isDone", () -> {
            String json = "{\"done\":true}";
            return OllamaJsonHelper.isDone(json);
        });
    }

    private static void testOpenAIJsonHelper() {
        System.out.println("\n━━━ OpenAI JSON Helper Tests ━━━");

        test("OpenAI buildOpenAIChatRequest", () -> {
            JSONArray messages = new JSONArray();
            messages.put(OpenAIJsonHelper.buildChatMessage("user", "hi"));
            String json = OpenAIJsonHelper.buildOpenAIChatRequest("gpt-3.5-turbo", messages, true);
            JSONObject obj = new JSONObject(json);
            return "gpt-3.5-turbo".equals(obj.getString("model")) && obj.getBoolean("stream");
        });

        test("OpenAI parseOpenAIChatChunk (SSE format)", () -> {
            String sse = "data: {\"choices\":[{\"delta\":{\"content\":\"hello\"}}]}";
            return "hello".equals(OpenAIJsonHelper.parseOpenAIChatChunk(sse));
        });

        test("OpenAI parseOpenAIChatChunk ([DONE] marker)", () -> {
            String done = "data: [DONE]";
            return OpenAIJsonHelper.parseOpenAIChatChunk(done) == null;
        });

        test("OpenAI isOpenAIDone", () -> {
            return OpenAIJsonHelper.isOpenAIDone("data: [DONE]") || OpenAIJsonHelper.isOpenAIDone("[DONE]");
        });
    }

    private static void test(String name, java.util.function.BooleanSupplier test) {
        try {
            if (test.getAsBoolean()) {
                System.out.println("  ✅ " + name);
                totalPassed++;
            } else {
                System.out.println("  ❌ " + name);
                totalFailed++;
            }
        } catch (Exception e) {
            System.out.println("  ❌ " + name + " - " + e.getMessage());
            totalFailed++;
        }
    }
}
