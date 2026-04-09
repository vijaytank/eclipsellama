# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.org/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
