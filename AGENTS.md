# NakshAstraMCP: Agent Instructions (v3.20.0)

This repository is optimized for use with **NakshAstraMCP**. 
**MANDATORY**: Always prioritize MCP tools over manual file-dumps or generic search.

---

## 🛠️ MCP Capabilities (Ground Truth)

| Tool | Priority | Usage Instruction |
| :--- | :--- | :--- |
| **`deep_context`** | **Primary** | Start here for all tasks. Locates files and adds their **immediate graph neighbors (1-hop)** for structural context. Strongly prefer over scattered `search_codebase` calls. |
| **`search_codebase`**| **Discovery** | Broad "grep-style" keyword search when you lack a specific entry point. |
| **`find_symbol`** | **Surgical** | Precise lookup for classes/functions by name. Use for "Go to Definition." |
| **`read_file`** | **Verification**| **MANDATORY** before any edit. Surgically read the relevant target block range (using line limits) to avoid assumptions. |
| **`find_references`**| **Audit** | Trace usages (Blast-radius) before changing custom functions. Avoid calling on common utilities / standard libraries to save tokens. |
| **`generate_report`** | **Map** | Produce a macro-level architectural report of the workspace. |
| **`server_status`** | **Diagnostic**| Use only if indexing or memory issues are suspected. |

---

## 📂 Workspace Artifacts

NakshAstraMCP generates architectural insights in the following directory:
- **`nakshastra-out/`**: Contains synthesis reports and interactive graphs:
  - `nakshastra-out/NAKSHASTRA_REPORT.md`: A comprehensive, macro-level architectural walkthrough of the repository, including key file clusters, PageRank matrices, and import dependencies.
  - `nakshastra-out/graph.json`: The raw symbol relationship graph in JSON format, which can be visualized.

---

## 🔄 Standardized Workflow

Follow this pattern for every non-trivial change:

1. **`generate_report`**: (Optional) Check `nakshastra-out/NAKSHASTRA_REPORT.md` for macro-view.
2. **`deep_context`**: Discover relevant files and architectural neighbors.
3. **`find_symbol`**: Locate the exact definition (line ranges) for the target logic.
4. **`read_file`**: **MUST** read the relevant target code block or line range (with enclosing context) before proposing edits.
5. **`find_references`**: Audit call sites only on custom functions (avoid common libraries/utils) to prevent token waste.

---

## 🛡️ Guardrails

- **No Snippet Assumptions**: Snippets are for discovery only. Never assume a full definition from a snippet.
- **Verification First**: Never edit code blocks you haven't read fully in their local context. Use line-range reads surgically to save tokens.
- **Tool Efficiency**: Prefer `deep_context` for architectural discovery over scattered `search_codebase` calls.
- **Log and Output Truncation**: When analyzing logs or test outputs, do not read complete log streams. Focus exclusively on relevant failure stack traces or tail lines to minimize token footprint.
- **Security Protocols**: All paths are jailed. If a tool fails with a security error, verify the path is within a registered workspace.
- **No Terminal/Shell MCP Execution**: Never attempt to run MCP tools (such as `generate_report` or `deep_context`) as command-line commands in the terminal (e.g., executing `nakshastramcp_generate_report`). MCP tools must be called strictly via the Model Context Protocol's tool-calling mechanism (using the `call_mcp_tool` wrapper or `mcp_nakshastramcp_...` native tools).
