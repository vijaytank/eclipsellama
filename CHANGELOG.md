# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.org/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.1.0] - Unreleased

### Added
- **Multi-Backend Provider Registry**: Introduced centralized `LlmProviderRegistry` supporting Ollama, OpenAI-compatible servers (llama-server, vLLM, LM Studio, Groq, OpenRouter), and custom providers.
- **Live Web Search Grounding**: Intelligent search integration with multiple providers (DuckDuckGo Built-in, Brave Search, SearXNG) and customizable search modes (`Smart`, `Ask`, `Always`, `Off`) with a collapsible results context panel.
- **Model Context Protocol (MCP) Client**: Full support for STDIO, SSE, and Streamable HTTP MCP transports with a dedicated configuration preference page (*Preferences → EclipseLlama → MCP Servers*) and live status diagnostics.
- **Persistent Chat History**: Added `ChatHistoryStore` to save and restore chat sessions seamlessly across Eclipse restarts.
- **Modern Chat Interface**: Streaming markdown rendering, hero welcome card with interactive quick prompt chips, and one-click **Copy Code** and **Insert in Editor** action bars.
- **Interactive Code Diff Dialog**: Side-by-side visual diff review using Eclipse CompareUI for proposed AI code fixes.
- **Context-Aware Prompting**: Added `CodeContextCollector` to extract class, method, imports, and enclosing context for enhanced code actions.
- **New AI Actions**: Added dedicated commands and handlers for **Refactor Code**, **Review Code**, and **Convert Code**.
- **Resilience & Retry Policy**: Wrapped network calls with exponential backoff retry policies and configurable timeout preferences.

### Fixed
- **Chat Viewport Scroll Freezing**: Resolved issue where long chat messages could not be scrolled by implementing dynamic word-wrapped container bounds calculation (`refreshMinSize`).
- **Duplicate Quick Actions UI**: Removed duplicate quick action buttons from the bottom bar in favor of the rich welcome card prompt chips.
- **Web Search Prompt Duplication**: Switched web search prompt enrichment to an ephemeral, pure-function model to prevent duplicate assistant responses and conversation history pollution.
- **Search Client Connection Pool**: Reused singleton `HttpClient` instances in background web search tasks rather than instantiating new connection pools on every search.
- **Auto-Scroll Synchronization**: Synchronized `scrollToBottom` with `ScrolledComposite.getMinHeight()` to ensure smooth tracking during streaming token generation.

---

## [2.0.1] - 2026-04-09

### Added
- **OpenAI Compatible Endpoint Support**: Full support for OpenAI-compatible APIs (e.g., `llama-server`) alongside local Ollama instances.
- **Windows OS Support**: Enhanced compatibility and specialized fixes for Windows 10/11 environments.
- **Connection Reliability**: Increased default connection timeout to 60 seconds to accommodate slower local LLM response times.

### Fixed
- **Menu Icon & Indentation**: Resolved the "indented" appearance of the `EclipseLlama` context menu heading and associated icon rendering issues on Linux/Ubuntu environments.
- **Contextual Chat Submission**: Fixed a bug where the Chat "Send" button and keyboard shortcuts would not trigger correctly when the input was pre-filled via "Explain this code" or "Fix this code" actions.

### Changed
- **Preference Persistence**: Improved the preference handling logic to ensure API keys and settings are saved automatically during connection tests and model refreshes.

### Credits
- Special thanks to [@bstoltefuss](https://github.com/bstoltefuss) for contributing the OpenAI backend architecture and stability improvements.
- Thanks to the community for reporting UI and submission issues on Linux.
