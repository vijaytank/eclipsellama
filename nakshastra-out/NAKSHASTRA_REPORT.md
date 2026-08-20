# NakshAstra Architectural Report

> **Generated at**: Antigravity IDE mapping

## 🏆 High-Impact Files
These files have the highest centrality (PageRank) and are critical to the system architecture.

- [src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java) (Score: 0.0351)
- [src/com/eclipsellama/plugin/ui/chat/ChatView.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/ui/chat/ChatView.java) (Score: 0.0345)
- [src/com/eclipsellama/plugin/core/ClientProvider.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/core/ClientProvider.java) (Score: 0.0250)
- [src/com/eclipsellama/plugin/core/ChatMessage.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/core/ChatMessage.java) (Score: 0.0247)
- [src/com/eclipsellama/plugin/search/SearchResult.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/search/SearchResult.java) (Score: 0.0236)
- [src/com/eclipsellama/plugin/mcp/McpServerConfig.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/mcp/McpServerConfig.java) (Score: 0.0235)
- [src/com/eclipsellama/plugin/core/OllamaClient.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/core/OllamaClient.java) (Score: 0.0232)
- [src/com/eclipsellama/plugin/core/OpenAIClient.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/core/OpenAIClient.java) (Score: 0.0229)
- [src/com/eclipsellama/plugin/core/LLMClient.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/core/LLMClient.java) (Score: 0.0229)
- [src/com/eclipsellama/plugin/search/parser/DuckDuckGoHtmlParser.java](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/search/parser/DuckDuckGoHtmlParser.java) (Score: 0.0229)

## 🧬 High-Impact Symbols
These class and function abstractions are the key logical hubs of the system.

- [load](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java#L80-L113) (method in `src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java` · Score: 1.0000)
- [getConfigFile](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java#L73-L75) (method in `src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java` · Score: 0.9250)
- [getConfigDir](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java#L66-L68) (method in `src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java` · Score: 0.8357)
- [execute](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/api/RetryPolicy.java#L39-L69) (method in `src/com/eclipsellama/plugin/api/RetryPolicy.java` · Score: 0.3884)
- [RetryPolicy](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/api/RetryPolicy.java#L11-L70) (class in `src/com/eclipsellama/plugin/api/RetryPolicy.java` · Score: 0.3833)
- [EclipseLlamaPreferences](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java#L15-L391) (class in `src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java` · Score: 0.3542)
- [ChatMessage](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/core/ChatMessage.java#L6-L66) (class in `src/com/eclipsellama/plugin/core/ChatMessage.java` · Score: 0.2928)
- [dispose](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/ui/chat/ChatStyles.java#L265-L293) (method in `src/com/eclipsellama/plugin/ui/chat/ChatStyles.java` · Score: 0.2742)
- [getEndpoint](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java#L139-L142) (method in `src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java` · Score: 0.2390)
- [get](file:///E:/Projects/eclipsellama/src/com/eclipsellama/plugin/search/SearchProviderRegistry.java#L24-L27) (method in `src/com/eclipsellama/plugin/search/SearchProviderRegistry.java` · Score: 0.1948)

## 📦 Module Communities (Louvain)
The following clusters represent tightly-coupled functional modules detected in the graph.

### Module 1: src Cluster
- **Size**: 17 nodes
- **Key Files**: src/com/eclipsellama/plugin/handlers/DocumentCodeHandler.java, test/com/eclipsellama/plugin/search/WebSearchRouterTest.java, src/com/eclipsellama/plugin/storage/ChatHistoryStore.java, src/com/eclipsellama/plugin/core/ChatMessage.java, src/com/eclipsellama/plugin/search/WebSearchRouter.java...

### Module 2: test Cluster
- **Size**: 14 nodes
- **Key Files**: test/com/eclipsellama/plugin/mcp/McpConnectionManagerTest.java, test/com/eclipsellama/plugin/context/CodeContextCollectorTest.java, test/com/eclipsellama/plugin/search/SearXNGSearchProviderTest.java, test/com/eclipsellama/plugin/search/SearchProviderRegistryTest.java, test/com/eclipsellama/plugin/mcp/McpServerRegistryTest.java...

### Module 3: src Cluster
- **Size**: 12 nodes
- **Key Files**: src/com/eclipsellama/plugin/api/RetryPolicy.java, src/com/eclipsellama/plugin/api/LlmProvider.java, src/com/eclipsellama/plugin/mcp/McpProvider.java, src/com/eclipsellama/plugin/core/OllamaClient.java, src/com/eclipsellama/plugin/core/OpenAIClient.java...

### Module 4: src Cluster
- **Size**: 8 nodes
- **Key Files**: src/com/eclipsellama/plugin/setup/SetupWizardDialog.java, src/com/eclipsellama/plugin/preferences/EclipseLlamaPreferences.java, src/com/eclipsellama/plugin/core/ClientProvider.java, src/com/eclipsellama/plugin/handlers/GenerateCommitHandler.java, test/com/eclipsellama/plugin/search/WebSearchServiceTest.java...

### Module 5: src Cluster
- **Size**: 5 nodes
- **Key Files**: src/com/eclipsellama/plugin/search/parser/DuckDuckGoHtmlParser.java, src/com/eclipsellama/plugin/search/SearchResult.java, src/com/eclipsellama/plugin/search/BuiltInSearchProvider.java, test/com/eclipsellama/plugin/search/BuiltInSearchProviderTest.java, test/com/eclipsellama/plugin/search/parser/DuckDuckGoHtmlParserTest.java


## ⚠️ Blast Radius Warnings
Modifying the 'God Nodes' listed above will impact the majority of the modules listed in this report. Exercise caution.

---
*Report generated by NakshAstraMCP Knowledge Mapping Engine.*