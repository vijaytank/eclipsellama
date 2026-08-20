# EclipseLlama 🦙

**Topics**: eclipse-plugin, ollama, openai, local-llm, mcp, model-context-protocol, ai-coding-assistant, java, eclipse-ide, web-search

**A powerful, privacy-first AI coding assistant for the Eclipse IDE. Powered by local Ollama, OpenAI-compatible endpoints (like llama-server, vLLM, LM Studio, Groq), live Web Search Grounding, and Model Context Protocol (MCP) integrations.**

---

## ✨ Features

| Feature | Description |
| :--- | :--- |
| **💬 Modern AI Chat** | Streaming Markdown chat with persistent conversation history across Eclipse restarts. |
| **🌐 Web Search Grounding** | Live internet search grounding (DuckDuckGo, Brave, SearXNG) with Smart, Ask, and Always modes. |
| **🔌 Model Context Protocol (MCP)** | Connect local and remote MCP servers (STDIO, SSE, Streamable HTTP) for tool-assisted AI. |
| **🔀 Interactive Code Diff** | Side-by-side visual diff dialog powered by Eclipse CompareUI for reviewing AI fixes. |
| **🔍 Explain & Review Code** | Select code in any editor → Right-click → Get in-depth explanations and architectural reviews. |
| **🐛 Fix & Refactor Code** | One-click automated bug-fixing and clean-code refactoring proposals. |
| **🧪 Test Generation** | Auto-generate comprehensive JUnit test suites with edge cases. |
| **📝 Documentation** | Generate clean, complete Javadoc comments automatically. |
| **⚡ Smart Code Insertion** | One-click **Copy Code** and **Insert in Editor** at the exact cursor position. |
| **🔒 Privacy First** | Run 100% locally with Ollama/llama-server or securely connect to your preferred cloud backend. |

---

## 🚀 Quick Start

### Prerequisites

1. **Operating System**: Windows 10/11, macOS, or Linux.
2. **Eclipse IDE**: 2023-12 or newer.
3. **Java**: JDK 21 or compatible runtime.
4. **Backend AI Engine**:
   - **Local Ollama** ([Download Ollama](https://ollama.com)), or
   - **OpenAI Compatible Endpoint** (e.g., `llama-server`, LM Studio, vLLM, LocalAI), or
   - **Cloud API** (OpenAI, OpenRouter, Groq, Together, Mistral, DeepSeek).

### Recommended Coding Models

```bash
# High-performance local coding models via Ollama
ollama pull qwen2.5-coder:7b
# or
ollama pull deepseek-coder:6.7b
# or
ollama pull codellama:7b
```

---

## 📦 Installation

### Method 1: Install from Update Site (Recommended)

1. In Eclipse, go to **Help → Install New Software...**
2. Click **Add...**
3. Enter Name: `EclipseLlama`
4. Enter Location: `https://vijaytank.github.io/eclipsellama/updatesite/`
5. Select **EclipseLlama** and complete the wizard.
6. Restart Eclipse.

### Method 2: Manual Dropins

1. Download the latest `EclipseLlama.jar` from the [Releases](https://github.com/vijaytank/eclipsellama/releases) page.
2. Place the JAR in your Eclipse `dropins/` folder.
3. Restart Eclipse.

---

## ⌨️ Keyboard Shortcuts & Quick Actions

| Shortcut | Action |
| :--- | :--- |
| `Ctrl+Shift+L` | Open EclipseLlama Chat View |
| `Ctrl+Shift+E` | Explain Selected Code |

### Right-Click Context Menu
Select any code in the Java editor and right-click to open the **EclipseLlama** submenu:
- ⚡ **Explain This Code**
- 🛠️ **Fix This Code** (Opens Side-by-Side Diff)
- 🧪 **Generate Tests**
- 📖 **Add Documentation / Javadoc**
- 🔄 **Refactor Code**
- 🔎 **Review Code**
- 🌐 **Convert Code**

---

## ⚙️ Configuration & Preferences

Open **Window → Preferences → EclipseLlama**:

### 1. General Preferences
- **Backend Type**: Select `Ollama` or `OpenAI`.
- **Endpoint URL**: e.g., `http://localhost:11434` (Ollama) or `http://localhost:8080/v1` (llama-server / OpenAI).
- **Default Model**: Choose or refresh available models directly from your endpoint.
- **Connection Timeout & Retries**: Configurable request timeouts (default 60s) and automatic retry attempts.

### 2. Web Search Preferences
- **Search Provider**: Built-in DuckDuckGo (zero-config), Brave Search (API key), or SearXNG (custom endpoint).
- **Search Mode**:
  - `Smart`: Model dynamically determines if web search is needed.
  - `Ask`: Prompts user confirmation before executing searches.
  - `Always`: Searches for every query.
  - `Off`: Disables web search.

### 3. MCP Servers (*Preferences → EclipseLlama → MCP Servers*)
- Add and configure Model Context Protocol (MCP) servers.
- Supports **STDIO** (local commands like `npx` or Python scripts), **SSE** (remote HTTP Server-Sent Events), and **Streamable HTTP**.
- Built-in live connection testing and tool discovery diagnostics.

---

## 🔒 Privacy & Security

- **100% Local by Default**: When using local Ollama or llama-server, your code never leaves your computer.
- **No Telemetry**: No hidden tracking or code analytics.
- **Secure Credentials**: API keys and tokens are stored securely via Eclipse Secure Storage.

---

## 🤝 Contributing

Contributions, bug reports, and feature suggestions are always welcome! Please check out [CONTRIBUTING.md](file:///E:/Projects/eclipsellama/CONTRIBUTING.md) and open an issue or pull request.

---

## 📄 License

MIT License © 2026 Vijay Tank & Contributors
