# EclipseLlama 🦙

Topics: eclipse-plugin, ollama, local-llm, ai-coding-assistant, java, eclipse-ide

**A privacy-first AI coding assistant for Eclipse IDE, powered by local Ollama models like CodeLlama and DeepSeek Coder. Features: AI chat, code explanation, bug fixes, test generation, Javadoc. 100% local, no cloud.**

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| **💬 AI Chat** | Conversation with your local LLM directly in Eclipse |
| **🔍 Explain Code** | Select code → Right-click → Get AI explanation |
| **🐛 Fix Code** | AI suggests fixes for selected code |
| **🧪 Generate Tests** | Auto-generate unit tests for your code |
| **📝 Add Documentation** | Generate Javadoc comments automatically |
| **⚡ Smart Autocomplete** | AI-powered code completion suggestions |
| **🔒 100% Private** | Everything runs locally, no cloud required |

---

## 🚀 Quick Start

### Prerequisites

1. **Eclipse IDE** 2023-12 or newer
2. **Java 21** or compatible JDK
3. **Ollama** installed and running ([Download](https://ollama.com))

### Install a Code Model

```bash
# Recommended models for coding
ollama pull codellama
# or
ollama pull deepseek-coder
# or
ollama pull qwen2.5-coder
```

### Install the Plugin

1. **Download** `EclipseLlama.jar` from releases
2. Copy to Eclipse's `dropins/` folder
3. Restart Eclipse
4. Complete the setup wizard

### Install from Update Site

1. In Eclipse, go to **Help → Install New Software...**
2. Click **Add...**
3. Enter Name: `EclipseLlama`
4. Enter Location: `https://vijaytank.github.io/eclipsellama/updatesite/`
5. Click **Add**, select **EclipseLlama**, and follow the wizard.

---

## ⌨️ Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+Shift+L` | Open EclipseLlama Chat |
| `Ctrl+Shift+E` | Explain Selected Code |

---

## 🎯 Usage

### Chat with AI
1. Press `Ctrl+Shift+L` or go to **Help → EclipseLlama Chat**
2. Type your question
3. Press Enter to send

### Code Actions
1. Select code in the editor
2. Right-click → **EclipseLlama** submenu
3. Choose an action:
   - 🔍 Explain This Code
   - 🐛 Fix This Code
   - 🧪 Generate Tests
   - 📝 Add Documentation

### Settings
**Window → Preferences → EclipseLlama**
- Configure Ollama endpoint URL
- Select default model
- Customize commit message prompts

---

## 🛠️ Configuration

Default settings are stored in `~/.eclipsellama/config.properties`:

```properties
ollama.endpoint=http://localhost:11434
ollama.model=codellama
```

---

## 📋 Recommended Models

| Model | Size | Best For |
|-------|------|----------|
| `codellama` | 7B | General coding |
| `codellama:13b` | 13B | Better accuracy |
| `deepseek-coder` | 6.7B | Fast completions |
| `qwen2.5-coder` | 7B | Multi-language |

---

## 🔒 Privacy

EclipseLlama runs **entirely on your machine**:
- ✅ No API keys required
- ✅ No data sent to cloud
- ✅ Works offline
- ✅ Your code stays private

---

## 🤝 Contributing

Issues and pull requests welcome!

---

## 📄 License

MIT License
