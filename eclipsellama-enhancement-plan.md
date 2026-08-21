# EclipseLlama Enhancement Plan *(Version 2.0 – Active Roadmap)*

> **Status**: Phases 1–6 Completed ✅ | Phase 7 Planning/Autonomous Agent Mode 🚀
> **Branch**: `develop` | **Next Target Release**: `v2.1.0` (Ready) | **Repo**: `https://github.com/vijaytank/eclipsellama`
> **Methodology**: Test-driven development, zero assumptions, AST-based inspection with NakshAstraMCP.

---

## 🗺️ Roadmap & Release Strategy

```mermaid
timeline
    title EclipseLlama Evolution Roadmap
    v2.0.1 (Released) : Initial OpenAI compatible endpoint
                      : Linux & Windows compatibility fixes
    v2.1.0 (Current Target) : Multi-Backend Provider Registry (Ollama, OpenAI, llama-server, LM Studio, Groq)
                            : Live Web Search Grounding (DuckDuckGo, Brave, SearXNG)
                            : Model Context Protocol (MCP) Client & Preferences (STDIO, SSE, HTTP)
                            : Persistent Chat History, Streaming Markdown & CodeDiffDialog
                            : Viewport-aware UI Layout & Scroll Engine
                            : Phase 5 Security Hardening (Secure Storage, Prompt Sanitizer, TLS)
    v2.2.0 / v3.0.0 (Next Major) : Planning Mode (Multi-step task breakdown & user approval)
                                 : Autonomous Agent Mode (ReAct tool execution loop with MCP)
                                 : Direct Workspace Code Patching & Refactoring Engine
```

---

## PART 0 – CURRENT ARCHITECTURAL STATE

### 0.1 Confirmed Integrations & Packages
```
src/com/eclipsellama/plugin/
├── api/            # LlmProvider, LlmProviderRegistry, RetryPolicy, OllamaProvider, OpenAiProvider
├── context/        # CodeContextCollector, ModelRouter, SimpleContextProvider
├── core/           # LLMClient, OllamaClient, OpenAIClient, ChatMessage
├── handlers/       # Explain, Fix, Doc, Test, Refactor, Review, Convert, OpenChat, OpenMcp, GenerateCommit
├── mcp/            # McpConnectionManager, McpProvider, McpServerConfig, Stdio/Sse/Http Connections
├── preferences/    # EclipseLlamaPreferences, McpPreferencePage, McpServerStore
├── search/         # WebSearchService, WebSearchRouter, SearchProviderRegistry, DDG/Brave/SearXNG Providers
├── storage/        # ChatHistoryStore (JSON persistence)
└── ui/
    ├── CodeDiffDialog.java
    ├── EclipseLlamaPerspective.java
    └── chat/ (ChatView, ChatStyles, MarkdownRenderer)
```

---

## PART 1 – PHASE EXECUTION TRACKER

### Phase 1 – Architectural Foundation (Provider Abstraction) ✅ COMPLETED
1. Interface `LlmProvider` and registry `LlmProviderRegistry` implemented.
2. Wrapper implementations `OllamaProvider` and `OpenAiProvider` registered.
3. Resilient `RetryPolicy` with configurable retry count and timeout backoff added.
4. Preferences for `timeout.seconds` (default 60s) and `retry.attempts` (default 3) wired.
5. Automated test suite coverage established in `api/` package.

### Phase 2 – MCP Client Integration & Probing ✅ COMPLETED
1. MCP Core connections implemented: `StdioMcpConnection`, `SseMcpConnection`, `StreamableHttpMcpConnection`.
2. `McpProvider` registered dynamically inside `LlmProviderRegistry`.
3. Preferences page `McpPreferencePage` created under *Preferences → EclipseLlama → MCP Servers* with real-time connection testing.
4. Probing status block builder wired to chat prompts.

### Phase 3 – Web Search Grounding ✅ COMPLETED
1. `SearchProviderRegistry` with pluggable providers:
   - `BuiltInSearchProvider` / `DuckDuckGoSearchProvider` (zero-config, HTML parser).
   - `BraveSearchProvider` (fast privacy API).
   - `SearXNGSearchProvider` (self-hosted metasearch).
2. Intelligent `WebSearchRouter` with 4 modes (`Smart`, `Ask`, `Always`, `Off`).
3. Ephemeral pre-prompt injection without mutating persistent chat history.
4. Collapsible Web Search Context Panel in `ChatView`.

### Phase 4 – Features, Modern UI & UX Hardening ✅ COMPLETED
1. **Persistent History**: `ChatHistoryStore` saves and restores conversations across Eclipse restarts.
2. **Context-Aware Prompts**: `CodeContextCollector` extracts class, method, and package context.
3. **Interactive Diff Dialog**: `CodeDiffDialog` leveraging Eclipse `CompareUI` for AI code fixes.
4. **New Handlers**: `RefactorCodeHandler`, `ReviewCodeHandler`, `ConvertCodeHandler`.
5. **Modern Chat UI**: Streaming markdown rendering, hero welcome card with interactive quick prompt chips, one-click "Copy Code" & "Insert in Editor" buttons.
6. **Viewport-Aware Scroll Engine**:
   - Fixed word-wrap height underestimation with dynamic width calculation (`refreshMinSize()`).
   - Synchronized auto-scroll with `scrolledComposite.getMinHeight()`.
   - Cleaned duplicate bottom quick action buttons to streamline UI.
   - Reused singleton HTTP client in background search pipelines.

---

## PART 2 – PENDING IMPLEMENTATION PHASES

### Phase 5 – Security Hardening ✅ COMPLETED (v2.1.0)

1. **Secure Storage Migration (`SecurePrefsStore`)**
   - Wrapped Eclipse `org.eclipse.equinox.security.storage.ISecurePreferences`.
   - Implemented idempotent migration of plain-text API keys from `config.properties` into encrypted secure storage on first startup (`SetupLauncher.migratePreferences()`).
   - Supports secure retrieval for OpenAI API keys, Brave Search keys, and MCP bearer tokens.
   - Graceful degradation when Equinox security bundle unavailable (logs warning, skips migration).
   - **Verification**: 5 JUnit tests (`SecurePrefsStoreTest`), manual validation confirmed legacy `config.properties` keys cleared on second start.

2. **Prompt Injection & Sanitization (`PromptSanitizer`)**
   - Added `PromptSanitizer.sanitizeUserInput(String)` stripping zero-width characters (`\u200B`, `\u200C`, `\u200D`, `\uFEFF`, `\u2060-\u206F`, `\u3000`) and neutralizing prompt-override phrases (`ignore previous instructions`, `system override`, `you are now`, etc.) with `[SANITIZED_OVERRIDE_ATTEMPT]` audit marker.
   - Integrated at single choke point in `ChatView.sendMessage()` covering all 8 code action handlers (Explain, Fix, Doc, Test, Refactor, Review, Convert, GenerateCommit) plus chat input and quick prompts.
   - **Verification**: 10 JUnit tests (`PromptSanitizerTest`), no regression in existing 82 tests.

3. **MCP Bearer Authentication** (verified existing wiring + secure storage integration)
   - `McpServerConfig` already had `bearerToken` field with getter/setter (lines 34, 123, 127-130).
   - `SseMcpConnection` and `StreamableHttpMcpConnection` already injected `Authorization: Bearer <token>` header when token present (lines 121-122, 172-173, 54-55).
   - Updated `McpServerStore` to persist/retrieve bearer tokens via `SecurePrefsStore` (encrypted).
   - `McpPreferencePage` already provides UI for bearer token input.
   - **Verification**: Code review confirmed; no new code required for auth mechanism itself.

4. **HTTPS / TLS Configuration**
   - Added preferences `security.tls.enforce` (boolean, default false) and `security.tls.truststore.path` (String, default empty) to `EclipseLlamaPreferences`.
   - Added TLS group to `EclipseLlamaPreferencePage` with "Enforce TLS" checkbox and "Custom truststore path" text field.
   - Created testable `TlsPolicy` class enforcing HTTPS for remote endpoints (localhost/LAN still permitted over HTTP).
   - Wired enforcement into `SseMcpConnection` and `StreamableHttpMcpConnection` constructors.
   - **Verification**: 7 JUnit tests (`TlsPolicyTest`), 104 total tests passing.

---

### Phase 6 – Quality Gates & Release Packaging ✅ COMPLETED (v2.1.0 Gate)

1. **Test Suite Verification**: Executed all JUnit test suites — **104/104 tests passing** (82 pre-existing + 22 new security tests). Zero compiler warnings (Java 21 PDE build).
2. **PDE Build & Update Site**: Generated plugin update site for `v2.1.0`:
   - `com.eclipsellama.plugin_2.1.0.jar` (287 KB) — compiled plugin bundle
   - `com.eclipsellama.feature_2.1.0.jar` (908 B) — feature descriptor
   - `content.jar` / `artifacts.jar` — p2 metadata repositories
   - `site.xml` — update site index
   - All built via Eclipse PDE `FeaturesAndBundlesPublisher` against `C:\Users\Vijay\eclipse\committers-2026-06`
3. **OSGi Manifest Validation**: Verified all exported packages and required bundles present in `META-INF/MANIFEST.MF`:
   - `Require-Bundle`: `org.eclipse.compare`, `org.eclipse.equinox.security` (already declared)
   - `Bundle-Version: 2.1.0`
   - `Bundle-RequiredExecutionEnvironment: JavaSE-21`

---

### Phase 7 – Planning Mode & Autonomous Agent Mode 🚀 (Target: v2.2.0 / v3.0.0)

1. **Planning Mode UI & Workflow**:
   - Specialized prompt builder that explores workspace context and produces a structured step-by-step implementation plan.
   - Plan preview card in `ChatView` with **[Approve & Execute]** and **[Refine Plan]** interactive controls.

2. **Autonomous Tool-Calling Loop (ReAct Engine)**:
   - Tool schema serialization (JSON Schema format) for registered MCP and IDE workspace tools.
   - Parser in `OllamaClient` / `OpenAIClient` to detect structured `tool_calls`.
   - Multi-turn execution loop: Intercept tool call -> Execute via `McpConnectionManager` -> Inject tool result -> Request next action -> Complete task.

3. **Direct Workspace Patching**:
   - Integration with Eclipse JDT / Workspace API to apply multi-file edits and preview AST diffs safely before committing.

---

## PART 3 – VERIFICATION & QUALITY GATES

| Gate | Requirement | Status |
| :--- | :--- | :--- |
| **Compilation** | Java 21 PDE build with zero warnings | ✅ Passed |
| **Unit Tests** | All 104 JUnit 4 tests passing | ✅ 104/104 OK |
| **Scroll / Layout** | Resizing, streaming, and large message scrolling | ✅ Verified |
| **Security** | Secure storage migration, prompt sanitizer, TLS enforcement | ✅ Phase 5 Complete |
| **Release Artifacts** | Plugin JAR, Feature JAR, p2 metadata (content/artifacts JARs), site.xml | ✅ v2.1.0 Built |
| **OSGi Manifest** | Required bundles: `org.eclipse.compare`, `org.eclipse.equinox.security` | ✅ Validated |
| **Tool Execution** | ReAct autonomous agent loop | 🚀 Phase 7 planned |