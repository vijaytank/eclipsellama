# NakshAstra Architectural Report

> **Generated at**: System32 mapping

## 🏆 High-Impact Files
These files have the highest centrality (PageRank) and are critical to the system architecture.

- [src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java) (Score: 0.0485)
- [src/com/eclipsellama/plugin/ui/chat/ChatView.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/ui/chat/ChatView.java) (Score: 0.0411)
- [src/com/eclipsellama/plugin/setup/SetupLauncher.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/setup/SetupLauncher.java) (Score: 0.0377)
- [src/com/eclipsellama/plugin/core/ClientProvider.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/core/ClientProvider.java) (Score: 0.0366)
- [src/com/eclipsellama/plugin/core/OllamaClient.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/core/OllamaClient.java) (Score: 0.0339)
- [src/com/eclipsellama/plugin/api/LlmException.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/api/LlmException.java) (Score: 0.0337)
- [src/com/eclipsellama/plugin/core/OpenAIClient.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/core/OpenAIClient.java) (Score: 0.0335)
- [src/com/eclipsellama/plugin/mcp/McpServerConfig.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/mcp/McpServerConfig.java) (Score: 0.0331)
- [src/com/eclipsellama/plugin/api/LlmProviderRegistry.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/api/LlmProviderRegistry.java) (Score: 0.0329)
- [src/com/eclipsellama/plugin/api/OpenAiProvider.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/api/OpenAiProvider.java) (Score: 0.0329)

## 🧬 High-Impact Symbols
These class and function abstractions are the key logical hubs of the system.

- [load](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java#L69-L96) (method in `src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java` · Score: 1.0000)
- [getConfigFile](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java#L62-L64) (method in `src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java` · Score: 0.9670)
- [getConfigDir](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java#L55-L57) (method in `src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java` · Score: 0.8980)
- [execute](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/api/RetryPolicy.java#L39-L67) (method in `src/com/eclipsellama/plugin/api/RetryPolicy.java` · Score: 0.5649)
- [RetryPolicy](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/api/RetryPolicy.java#L11-L68) (class in `src/com/eclipsellama/plugin/api/RetryPolicy.java` · Score: 0.5538)
- [EclipseLlamaPreferences](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java#L15-L270) (class in `src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java` · Score: 0.4479)
- [ChatMessage](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/core/ChatMessage.java#L6-L66) (class in `src/com/eclipsellama/plugin/core/ChatMessage.java` · Score: 0.3888)
- [getEndpoint](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java#L122-L125) (method in `src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java` · Score: 0.3531)
- [McpConnectionManager](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/mcp/McpConnectionManager.java#L12-L121) (class in `src/com/eclipsellama/plugin/mcp/McpConnectionManager.java` · Score: 0.2219)
- [LlmProviderRegistry](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/api/LlmProviderRegistry.java#L12-L64) (class in `src/com/eclipsellama/plugin/api/LlmProviderRegistry.java` · Score: 0.2068)

## 📦 Module Communities (Louvain)
The following clusters represent tightly-coupled functional modules detected in the graph.

### Module 1: src Cluster
- **Size**: 11 nodes
- **Key Files**: src/com/eclipsellama/plugin/core/OpenAIClient.java, src/com/eclipsellama/plugin/api/OllamaProvider.java, src/test/java/com/eclipsellama/plugin/setup/SetupLauncherTest.java, src/com/eclipsellama/plugin/api/RetryPolicy.java, src/com/eclipsellama/plugin/api/LlmProvider.java...

### Module 2: src Cluster
- **Size**: 7 nodes
- **Key Files**: src/com/eclipsellama/plugin/completion/EclipseLlamaProposalComputer.java, src/com/eclipsellama/plugin/handlers/GenerateCommitHandler.java, src/com/eclipsellama/plugin/setup/SetupWizardDialog.java, src/com/eclipsellama/plugin/git/CommitMessageGenerator.java, src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferencePage.java...

### Module 3: src Cluster
- **Size**: 6 nodes
- **Key Files**: src/com/eclipsellama/plugin/handlers/FixCodeHandler.java, src/com/eclipsellama/plugin/handlers/DocumentCodeHandler.java, src/com/eclipsellama/plugin/handlers/ExplainCodeHandler.java, src/com/eclipsellama/plugin/handlers/GenerateTestsHandler.java, src/com/eclipsellama/plugin/core/ChatMessage.java...

### Module 4: src Cluster
- **Size**: 6 nodes
- **Key Files**: src/com/eclipsellama/plugin/mcp/StreamableHttpMcpConnection.java, src/com/eclipsellama/plugin/preferences/McpServerStore.java, src/com/eclipsellama/plugin/mcp/McpConnection.java, src/com/eclipsellama/plugin/mcp/McpConnectionManager.java, src/com/eclipsellama/plugin/preferences/McpPreferencePage.java...

### Module 5: src Cluster
- **Size**: 1 nodes
- **Key Files**: src/com/eclipsellama/plugin/search/SearchResult.java


## ⚠️ Blast Radius Warnings
Modifying the 'God Nodes' listed above will impact the majority of the modules listed in this report. Exercise caution.

---
*Report generated by NakshAstraMCP Knowledge Mapping Engine.*