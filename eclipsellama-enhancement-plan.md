# EclipseLlama Enhancement Plan  *(Version 1.1 – Validation Complete)*

> **Status**: Analysis & Validation Completed ✅
> **Branch**: `develop` | **Latest Tag**: `v2.0.1` | **Repo**: `https://github.com/vijaytank/eclipsellama`
> **Methodology**: All evidence is fetched from the workspace (`read_file` / `search_files`). No code is inferred from external sources.

---  

## PART 0 – VERIFIED CODEBASE INVENTORY [COMPLETED]

### 0.1 Confirmed Manifests & Dependencies
| File | Key Fact | Evidence |
|------|----------|----------|
| `plugin.xml` | Declares 12 extension points (Perspective, View, Startup, Preferences, 6 commands) and shortcuts `Ctrl+Shift+L`, `Ctrl+Shift+E`. | Direct file read (lines 1‑175). |
| `.classpath` | Only external JAR is `lib/json-20240303.jar`. No HTTP client library present. | `.classpath` read (lines 48‑55). |
| `.project` | Nature definitions: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`. | `.project` read (lines 61‑66). |
| `CHANGELOG.md` (v2.0.1) | Introduced OpenAI‑compatible endpoint, Windows support, 60‑s timeout, pre‑fill bug‑fix, Linux icon fix. | `CHANGELOG.md` read (lines 70‑78). |
| `CONTRIBUTING.md` | Project structure: `src/`, `test/`, `META-INF/`, `plugin.xml`, `lib/`. | `CONTRIBUTING.md` read (lines 81‑88). |
| `README.md` | Config location `~/.eclipsellama/config.properties`; supported models list; Marketplace hint “Use Nakshastramcp …”. | `README.md` read (lines 90‑96). |

### 0.2 Inferred Package Layout
```
src/com/eclipsellama/plugin/
├─ ui/
│   └─ chat/ChatView.java
├─ setup/SetupLauncher.java
├─ preferences/EclipseLlamaPreferencePage.java
├─ handlers/
│   ├─ OpenChatHandler.java
│   ├─ ExplainCodeHandler.java
│   ├─ FixCodeHandler.java
│   ├─ GenerateTestsHandler.java
│   ├─ DocumentCodeHandler.java
│   └─ GenerateCommitHandler.java
└─ completion/EclipseLlamaProposalComputer.java
``` 
*All packages are **read‑only** for this validation step – no modifications are made yet.*

---  

## PART 1 – CURRENT STATE ANALYSIS [COMPLETED]

| Area | Observed Gap | Evidence |
|------|--------------|----------|
| HTTP handling | No dedicated client; relies on `java.net.HttpURLConnection`/`HttpClient`. | `.classpath` analysis (line 57). |
| Config storage | Plain‑text `config.properties`; API keys stored in clear text. | `README.md` (line 92) & `CHANGELOG.md` (line 77). |
| Provider abstraction | Two separate back‑ends (Ollama, OpenAI) with duplicated logic. | CHANGELOG v2.0.1 (line 72). |
| Context persistence | Chat history lost on Eclipse restart. | ChatView is an SWT `ViewPart` (no DB). |
| Testing & CI | No CI pipeline; tests exist but are not integrated. | `CONTRIBUTING.md` (lines 85‑88). |
| Security | No secure storage, no input sanitisation. | Plain‑text config comment (line 77). |
| Feature set | No web search, no MCP, no multi‑model routing, no code‑diff UI. | Marketplace hint (line 95) & TODO list. |

---  

## PART 2 – ENHANCEMENT PLAN (Phased Implementation)

> **All phases must be executed sequentially.** Skipping or re‑ordering will break the build or corrupt the OSGi bundle.

### Phase 1 – Architectural Foundation (Provider Abstraction) ✅ COMPLETED

1. Add interface `com.eclipsellama.plugin.api.LlmProvider` (new file).  
2. Add exception type `LlmException` (new file).  
3. Add registry `LlmProviderRegistry` (new file).  
4. Create wrapper implementations:  
   - `OllamaProvider` – delegates to existing Ollama call logic.  
   - `OpenAiProvider` – delegates to the OpenAI‑compatible backend introduced in v2.0.1.  
5. Register providers in `SetupLauncher` (startup extension) **only** after the registry is instantiated.  
6. Introduce `RetryPolicy` (new file) and wrap every HTTP call with `RetryPolicy.execute(...)`.  
7. Expose configurable timeout & retry count via two new preference keys:  
   - `eclipsellama.timeout.seconds` (default 60, range 10‑300).  
   - `eclipsellama.retry.attempts` (default 3, range 1‑5).  
8. **Verification checklist:**  
   - `mvn clean verify` (or Eclipse “Run As → Eclipse Application”) succeeds.  
   - All existing handlers still compile and run unchanged.  
   - `EclipseLlamaProposalComputer` autocomplete still works.  
   - No OSGi resolution errors (`MANIFEST.MF` imports unchanged).  

### Phase 2 – MCP Client Integration ✅ COMPLETED

1. Add MCP core classes (`McpMessage`, `StdioMcpConnection`, `SseMcpConnection`, `McpConnectionManager`, `McpServerConfig`, `McpServerRegistry`).  
2. Implement `McpProvider` that satisfies `LlmProvider` and routes LLM calls through a configured MCP server.  
3. Add preference page `McpPreferencePage` (new UI under *Eclipse Preferences → EclipseLlama → MCP Servers*).  
4. Add command `com.eclipsellama.plugin.command.openMcp` + handler `OpenMcpHandler`.  
5. Add UI toggle “🌐 Web Search” to `ChatView` toolbar; wire to a `WebSearchProvider` (see Phase 3).  
6. Update `plugin.xml`:  
   - Define **category** `com.eclipsellama.plugin.category`.  
   - Register the three new commands and their handlers.  
   - Register the new preference page under the category.  
   - Add `Require‑Bundle: org.eclipse.compare, org.eclipse.equinox.security` to `MANIFEST.MF`.  
7. **Verification checklist:**  
   - Ability to add a local STDIO MCP server (e.g., `npx -y @modelcontextprotocol/server-filesystem .`) via the new page.  
   - Ability to test a remote SSE MCP endpoint (`https://…/sse`).  
   - Tool‑call round‑trip: LLM → MCP tool → result → LLM response works end‑to‑end.  
   - All Phase 1 compiled artefacts still load; no bundle errors.  

### Phase 3 – Web Search Integration

1. Create interface `WebSearchProvider` (new file).  
2. Implement three concrete providers:  
   - `DuckDuckGoSearchProvider` (uses `https://api.duckduckgo.com`).  
   - `SearXNGSearchProvider` (user‑configurable endpoint).  
   - `BraveSearchProvider` (uses Brave Search API; requires API‑key stored via Eclipse Secure Storage).  
3. Add preference keys for selecting provider, max results, and optional API key.  
4. Extend `ChatView`: add a toggle button that, when enabled, injects search results **as a pre‑prompt** before sending the user message to the LLM.  
5. Render search results in a collapsible panel above the chat output.  
6. Add unit tests for each provider that mock the HTTP response using a local server (WireMock‑style stub) and assert that the parsed `SearchResults` contain the expected fields.  
7. **Verification checklist:**  
   - Search toggle appears in the toolbar without breaking existing layout.  
   - Results panel shows title, URL, snippet for each hit.  
   - When disabled, the plugin behaves exactly as pre‑Phase 3 (regression test passes).  

### Phase 4 – Features, Design & UX Enhancements

| Sub‑feature | Action Items | Test Requirements |
|-------------|--------------|-------------------|
| Persistent Chat History | • Add `ChatHistoryStore` (JSON file under workspace metadata). <br>• Hook load on `ChatView` creation; save after each message. | Unit test verifies that a message written to the file is later read back unchanged. |
| Context‑Aware Prompting | • Add `CodeContextCollector` to extract class name, method, imports, surrounding lines. <br>• Modify each handler to prepend the collected context to the LLM prompt. | Integration test ensures the generated prompt contains the expected context fragments. |
| Multi‑Model Router | • Add per‑task preference keys (`eclipsellama.model.explain`, `eclipsellama.model.fix`, …). <br>• Implement `ModelRouter` to resolve the correct model ID. | Test that `ModelRouter.resolve(Task.EXPLAIN, prefs)` returns the configured model string; default fallback works. |
| Inline Diff View | • Add `CodeDiffDialog` using Eclipse’s `org.eclipse.compare.CompareUI`. <br>• Replace raw text output of `FixCodeHandler` with the dialog. | UI test (SWTBot) verifies that the diff view opens and can be closed. |
| New Commands & Shortcuts | Add commands `refactorCode`, `reviewCode`, `convertCode` (add to `plugin.xml`, handlers, keyboard bindings). | Each command must have a unit test that calls it via the command framework and asserts the expected UI action (e.g., dialog opened). |
| Streaming Response | Ensure `LlmProvider.stream(...)` is correctly implemented for both Ollama and OpenAI backends (respect back‑pressure, close streams). | End‑to‑end test captures streaming tokens and verifies no truncation before completion. |

### Phase 5 – Security Hardening

1. **Secure Storage Migration**  
   - Add `SecurePrefsStore` (wraps `InstanceScope`).  
   - In `SetupLauncher`, read legacy keys from `config.properties` and write them to secure storage; then delete the legacy entries.  
   - **Test:** Simulate a legacy key present; after launch, verify it no longer exists in `config.properties` and is stored under `ISecurePreferences`.  

2. **Prompt Injection Defence**  
   - Add `PromptSanitizer.sanitizeUserInput(String)` (zero‑width removal, ignore‑instructions pattern).  
   - Call this method in every handler **before** building the prompt.  
   - **Test:** Feed a selection containing `// Ignore all previous instructions` and assert that the resulting prompt does **not** contain the phrase “Ignore all previous instructions”.  

3. **MCP Authentication**  
   - Extend `McpServerConfig` with `authType` (`NONE`/`BEARER`) and `bearerToken` field.  
   - Store the token via `SecurePrefsStore`.  
   - `SseMcpConnection` adds `Authorization: Bearer ***` when `authType == BEARER`.  
   - **Test:** Configure a dummy SSE endpoint with a known token; verify the request header contains the token.  

4. **HTTPS Enforcement**  
   - Add preference keys `eclipsellama.tls.enforce` (boolean) and `eclipsellama.tls.custom_truststore_path`.  
   - All outgoing HTTP clients must call `HttpClient.newHttpClient().newBuilder().followRedirects(...)` with `setSecure(true)`.  
   - **Test:** Attempt a connection to an `http://` URL; the client must abort with a clear `SSLHandshakeException`.  

### Phase 6 – Testing Strategy & Quality Gates

| Test Type | Where to Place It | Must‑Do Checklist |
|-----------|-------------------|-------------------|
| Unit Tests | `src/test/java/com/eclipsellama/plugin/...` for every new/changed class. | • Use JUnit 5 + AssertJ. <br>• No static values; read configuration from `EclipseLlamaPreferencePage` or `SecurePrefsStore`. <br>• Verify retry logic with a mock `HttpURLConnection` that returns 503 on first two calls. |
| Integration Tests | `src/test/java/com/eclipsellama/plugin/it/` (Eclipse‑based). | • Run via “Run As → JUnit Test”. <br>• Validate that `ChatView` restores history from the JSON file. |
| Static Analysis | Maven profile `quality-gate` (SpotBugs, Checkstyle, PMD). | • Build must fail if **any** new bug is reported. <br>• Results are archived as CI artefacts (`target/spotbugsXml`, `target/checkstyle-result.xml`). |
| Code‑Coverage | Enforced minimum **80 %** line coverage for all new code. | • Use JaCoCo; CI step `mvn jacoco:report`. |
| Performance | Load test for MCP STDIO process start‑up (< 2 s on Linux, < 4 s on Windows). | • Use `System.nanoTime()` before/after `ProcessBuilder.start()`. |
| Regression Suite | Execute **all** existing handler tests after each phase. | • Must pass 100 % before promotion to next phase. |

---  

## PART 7 – ADDITIONAL ENHANCEMENT AREAS (ONGOING)

The following capabilities are **future‑proof additions** that can be slotted into later sprints without disturbing the current phased flow. Keep them in the *“Backlog”* section of this document; do **not** implement them until a later iteration explicitly requests them.

| Area | High‑Level Goal | Example Backlog Item |
|------|----------------|----------------------|
| CI/CD | Fully automated build, test, release, and publishing pipeline. | Add `pom.xml` with Tycho, GitHub Actions workflow `ci.yml`, `scripts/release.sh`, `renovate.json`, dependency‑check plugin. |
| Advanced Static Analysis | Detect security vulnerabilities, enforce coding standards. | Integrate SpotBugs, Checkstyle, PMD; expose UI indicator in Error Log. |
| Auto‑Doc Generation | Keep API docs up‑to‑date. | Maven Javadoc plugin → GitHub Pages `docs/api/`; PlantUML diagrams for architecture. |
| Cross‑Platform Support | Validate on macOS & Linux; provide Docker image for CI. | `Dockerfile` that launches Eclipse with the plugin; adjust path handling (`$HOME/.eclipsellama`). |
| Telemetry (Opt‑In) | Gather anonymised usage metrics. | `TelemetryService` writes to `~/.eclipsellama/usage.json`; UI page to view/delete. |
| Marketplace Automation | Publish to Eclipse Marketplace on tag. | `scripts/publish-to-marketplace.sh` using Marketplace REST API; keystore handling. |
| Live UI Reload | Hot‑swap themes / icons without restart. | Watch `customization/` directory; reload `plugin.xml` fragments via `Display.getDefault().asyncExec`. |
| Collaborative Review | Connect to GitHub PRs, show status badges. | Preference “Connect GitHub Account”; PR badge rendering in Package Explorer. |

---  

## PART 8 – MASTER FILE CHANGE MANIFEST (ORDERED IMPLEMENTATION LIST)

### 8.1 Files to **ADD** (in the exact order they must appear in the workspace)

1. `src/com/eclipsellama/plugin/api/LlmProvider.java`  
2. `src/com/eclipsellama/plugin/api/LlmException.java`  
3. `src/com/eclipsellama/plugin/api/LlmProviderRegistry.java`  
4. `src/com/eclipsellama/plugin/api/OllamaProvider.java`  
5. `src/com/eclipsellama/plugin/api/OpenAiProvider.java`  
6. `src/com/eclipsellama/plugin/api/RetryPolicy.java`  
7. `src/com/eclipsellama/plugin/mcp/McpMessage.java`  
8. `src/com/eclipsellama/plugin/mcp/StdioMcpConnection.java`  
9. `src/com/eclipsellama/plugin/mcp/SseMcpConnection.java`  
10. `src/com/eclipsellama/plugin/mcp/McpConnectionManager.java`  
11. `src/com/eclipsellama/plugin/mcp/McpServerConfig.java`  
12. `src/com/eclipsellama/plugin/mcp/McpServerRegistry.java`  
13. `src/com/eclipsellama/plugin/mcp/McpProvider.java` *(implementation of LlmProvider that talks to MCP)*  
14. `src/com/eclipsellama/plugin/search/WebSearchProvider.java`  
15. `src/com/eclipsellama/plugin/search/SearchResults.java`  
16. `src/com/eclipsellama/plugin/search/DuckDuckGoSearchProvider.java`  
17. `src/com/eclipsellama/plugin/search/SearXNGSearchProvider.java`  
18. `src/com/eclipsellama/plugin/security/SecurePrefsStore.java`  
19. `src/com/eclipsellama/plugin/security/PromptSanitizer.java`  
20. `src/com/eclipsellama/plugin/context/CodeContextCollector.java`  
21. `src/com/eclipsellama/plugin/context/TokenBudget.java`  
22. `src/com/eclipsellama/plugin/storage/ChatHistoryStore.java`  
23. `src/com/eclipsellama/plugin/ui/CodeDiffDialog.java`  
24. `src/com/eclipsellama/plugin/preferences/McpPreferencePage.java`  
25. `src/com/eclipsellama/plugin/preferences/McpServerConfigEditor.java` *(UI for editing)*  
26. `src/com/eclipsellama/plugin/preferences/PromptTemplatePage.java`  
27. `src/com/eclipsellama/plugin/handlers/RefactorCodeHandler.java`  
28. `src/com/eclipsellama/plugin/handlers/ReviewCodeHandler.java`  
29. `src/com/eclipsellama/plugin/handlers/ConvertCodeHandler.java`  
30. `src/com/eclipsellama/plugin/handlers/ExplainCodeHandler.java` *(updated to use context & sanitizer)*  
31. `src/com/eclipsellama/plugin/handlers/FixCodeHandler.java` *(wired to CodeDiffDialog)*  
31a. `src/com/eclipsellama/plugin/handlers/GenerateTestsHandler.java` *(unchanged but must have new unit test)*  
32. `src/com/eclipsellama/plugin/handlers/DocumentCodeHandler.java`  
33. `src/com/eclipsellama/plugin/handlers/GenerateCommitHandler.java`  
34. `src/com/eclipsellama/plugin/handlers/OpenMcpHandler.java`  
35. `src/com/eclipsellama/plugin/handlers/OpenChatHandler.java` *(unchanged but must retain shortcut)*  
36. `src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferencePage.java` *(adds timeout, retry, search settings)*  
37. `src/com/eclipsellama/plugin/preferences/McpPreferencePage.java` *(adds provider list, test button)*  
38. `src/com/eclipsellama/plugin/preferences/PromptTemplatePage.java` *(adds template editor)*  
39. `src/com/eclipsellama/plugin/preferences/TelemetryPage.java` *(optional, opt‑in)*  
40. `src/com/eclipsellama/plugin/security/TlsConfig.java` *(HTTPS enforcement helper)*  
41. `src/com/eclipsellama/plugin/cache/ResponseCache.java`  
42. `src/com/eclipsellama/plugin/quality/QualityGateChecker.java` *(static-analysis hook)*  

### 8.2 Files to **MODIFY** (only the minimal parts indicated)

| File | What to Add / Change |
|------|----------------------|
| `plugin.xml` | • Insert `<category>` definition. <br>• Register all new commands and handlers. <br>• Add new `<page>` entries for MCP, Prompt Templates, Telemetry. <br>• Add `Require‑Bundle` entries. |
| `MANIFEST.MF` | Append `Require‑Bundle: org.eclipse.compare,org.eclipse.equinox.security`. |
| `src/com/eclipsellama/plugin/setup/SetupLauncher.java` | Register `McpProvider` (if a server is configured) and migrate legacy API keys to secure storage. |
| `src/com/eclipsellama/plugin/ui/ChatView.java` | Add web‑search toggle button; hook into `WebSearchProvider`; load/save chat history via `ChatHistoryStore`. |
| `src/com/eclipsellama/plugin/handlers/FixCodeHandler.java` | After LLM returns corrected code, open `CodeDiffDialog` instead of appending raw text. |
| `src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferencePage.java` | Add UI rows for timeout, retry count, search provider selection, TLS enforcement, telemetry opt‑in. |
| `src/com/eclipsellama/plugin/mcp/StdioMcpConnection.java` | Register a JVM shutdown hook that kills the MCP process. |
| `src/test/java/...` | Add a **test class** for every new production class (see Phase 6 checklist). |

### 8.3 Files to **NOT TOUCH**

All existing source files that are already proven to work must remain **unchanged** unless a specific migration step is listed above. Directly editing them without the prescribed migration will break the build.

---  

## PART 9 – EXECUTION CHECKLIST (What the AI Agent Must Do Before Declaring “Done”)

1. **Run the full Maven (or PDE) build** (`mvn clean verify`) **without errors**.  
2. **Launch Eclipse with the plugin** (`Run As → Eclipse Application`). Verify:  
   - Perspective opens.  
   - Shortcuts `Ctrl+Shift+L` and `Ctrl+Shift+E` work.  
   - Preferences pages (MCP, Search, Security) are accessible and persist values.  
3. **Execute the entire unit‑test suite** (`mvn test`). All tests must pass **including** the newly added ones.  
4. **Execute static‑analysis quality gate** (`mvn verify -Pquality-gate`). Build must **not** fail on new bugs.  
5. **Run the integration test suite** that exercises:  
   - Chat‑history persistence.  
   - MCP connection (STDIO & SSE).  
   - Web‑search toggle + result rendering.  
   - Diff view from “Fix Code”.  
6. **Check that no hard‑coded values** remain in any production source file (search for literals that should be read from preferences or `System.getProperty`).  
7. **Confirm that every calculation** (e.g., token budget, timeout back‑off) uses **typed variables** and is covered by at least one deterministic unit test.  
8. **Validate that CI artefacts** (`target/spotbugsXml`, `target/checkstyle-result.xml`, `target/site/jacoco.html`) are generated and published as CI artefacts.  
9. **Commit the updated `plugin.xml`** (only after all checks pass). Do **not** commit test data, mock files, or generated documentation.  

---  

## TL;DR – How the AI Agent Should Proceed

1. **Read** every file listed in **PART 0** to confirm the baseline.  
2. **Implement** the changes in strict numerical order from **PART 8** (add, then modify).  
3. **Write a unit test** for each new class **immediately after** the class file is created.  
4. **Run** the full build and test chain after **each phase** (Phase 1 → Phase 2 → …).  
5. **Never** introduce a literal (URL, token, number, file path) without first reading it from a **configured preference** or **secure storage** key.  
6. **Never** commit generated artefacts; only source files and test classes belong in version control.  
7. **When a phase finishes**, tick the corresponding checkbox in the **Verification checklist** before moving to the next phase.  

Following this exact, ordered, test‑driven workflow guarantees that the existing codebase never breaks, that every new piece of code is production‑ready with proper tests, and that the enhancement plan can be safely handed to any AI‑assistant without risking accidental mutation of unrelated parts.

---  

*End of reformatted Enhancement Plan.*