package com.eclipsellama.plugin.ui.chat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eclipsellama.plugin.api.LlmProvider;
import com.eclipsellama.plugin.api.LlmProviderRegistry;
import com.eclipsellama.plugin.context.CodeContextCollector;
import com.eclipsellama.plugin.core.ChatMessage;
import com.eclipsellama.plugin.core.ClientProvider;
import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;
import com.eclipsellama.plugin.search.SearchResult;
import com.eclipsellama.plugin.search.WebSearchRouter;
import com.eclipsellama.plugin.search.WebSearchService;
import com.eclipsellama.plugin.storage.ChatHistoryStore;
import com.eclipsellama.plugin.ui.CodeDiffDialog;

/**
 * Modern, memory-aware interactive chat interface for EclipseLlama. Features:
 * sliding context window, ephemeral search context (no history bloat), dynamic
 * MCP tool routing, theme-adaptive cards, Markdown, streaming, dual code
 * actions.
 */
public class ChatView extends ViewPart {

	public static final String ID = "com.eclipsellama.plugin.view.chat";

	// Memory & Context window limits (Sliding window budget)
	private static final int MAX_CONTEXT_MESSAGES = 10;
	private static final int MAX_CONTEXT_CHARS = 12000;

	// UI Components
	private ScrolledComposite scrolledComposite;
	private Composite messagesContainer;
	private Text inputField;
	private Button sendButton;
	private Button stopButton;
	private Combo modelCombo;
	private Label statusDot;
	private Label statusLabel;
	private Label charCountLabel;

	// State
	private final List<ChatMessage> conversation = new ArrayList<>();
	private volatile boolean isStreaming = false;
	private StringBuilder currentResponse;
	private StyledText currentAssistantBubble;

	// Track last action and context
	private String lastAction = "";
	private String lastContext = "";

	// Web search
	private WebSearchService webSearchService = createWebSearchService();
	private Combo searchModeCombo;
	private Composite searchResultsPanel;
	private StyledText searchResultsText;

	// Persistence
	private final ChatHistoryStore historyStore = new ChatHistoryStore();

	// Styling
	private ChatStyles styles;
	private MarkdownRenderer markdownRenderer;
	private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

	@Override
	public void createPartControl(Composite parent) {
		Display display = parent.getDisplay();
		styles = ChatStyles.getInstance(display);
		markdownRenderer = new MarkdownRenderer(styles);

		parent.setLayout(new GridLayout(1, false));

		createToolbar(parent);
		createChatArea(parent);
		createInputArea(parent);

		addSystemMessage();
		refreshModels();
		loadHistory();
		updateSearchModeCombo();
	}

	private void createToolbar(Composite parent) {
		Composite toolbar = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(8, false);
		layout.marginWidth = 6;
		layout.marginHeight = 4;
		layout.horizontalSpacing = 6;
		toolbar.setLayout(layout);
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Model selector
		Label modelLabel = new Label(toolbar, SWT.NONE);
		modelLabel.setText("🦙 Model:");
		modelLabel.setFont(styles.getBoldFont());

		modelCombo = new Combo(toolbar, SWT.DROP_DOWN | SWT.READ_ONLY);
		GridData comboData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		comboData.minimumWidth = 140;
		modelCombo.setLayoutData(comboData);
		modelCombo.addListener(SWT.Selection, e -> {
			String selected = modelCombo.getText();
			if (!selected.isEmpty()) {
				EclipseLlamaPreferences.setModel(selected);
				EclipseLlamaPreferences.save();
			}
		});

		// Refresh button
		Button refreshBtn = new Button(toolbar, SWT.PUSH | SWT.FLAT);
		refreshBtn.setText("🔄");
		refreshBtn.setToolTipText("Refresh available models from endpoint");
		refreshBtn.addListener(SWT.Selection, e -> refreshModels());

		// Clear button
		Button clearBtn = new Button(toolbar, SWT.PUSH | SWT.FLAT);
		clearBtn.setText("🗑️");
		clearBtn.setToolTipText("Clear conversation history");
		clearBtn.addListener(SWT.Selection, e -> clearConversation());

		// Web Search mode selector
		Label searchModeLabel = new Label(toolbar, SWT.NONE);
		searchModeLabel.setText("🌐 Search:");
		searchModeLabel.setFont(styles.getBoldFont());

		searchModeCombo = new Combo(toolbar, SWT.DROP_DOWN | SWT.READ_ONLY);
		searchModeCombo.setItems(new String[] { "Off", "Smart", "Ask", "Always" });
		searchModeCombo.setToolTipText(
				"Off: no search\nSmart: model decides\nAsk: confirm before search\nAlways: search every message");
		searchModeCombo.addListener(SWT.Selection, e -> {
			String mode = mapModeLabelToKey(searchModeCombo.getText());
			EclipseLlamaPreferences.setSearchMode(mode);
			EclipseLlamaPreferences.save();
		});

		// Settings button
		Button settingsBtn = new Button(toolbar, SWT.PUSH | SWT.FLAT);
		settingsBtn.setText("⚙️");
		settingsBtn.setToolTipText("Open EclipseLlama preferences");
		settingsBtn.addListener(SWT.Selection, e -> {
			org.eclipse.ui.dialogs.PreferencesUtil
					.createPreferenceDialogOn(parent.getShell(), "com.eclipsellama.plugin.preferences", null, null)
					.open();
		});
	}

	private void createChatArea(Composite parent) {
		scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.BORDER);
		scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		messagesContainer = new Composite(scrolledComposite, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 8;
		layout.marginHeight = 8;
		layout.verticalSpacing = 10;
		messagesContainer.setLayout(layout);
		messagesContainer.setBackground(styles.getContainerBackground());

		scrolledComposite.setContent(messagesContainer);

		// Smart word wrap resize listener: adjusts container width dynamically to the
		// visible viewport
		scrolledComposite.addListener(SWT.Resize, e -> {
			int width = scrolledComposite.getClientArea().width;
			if (width > 50) {
				messagesContainer.setSize(width, messagesContainer.computeSize(width, SWT.DEFAULT).y);
				messagesContainer.layout(true, true);
				scrolledComposite.setMinSize(messagesContainer.computeSize(width, SWT.DEFAULT));
			}
		});

		createSearchResultsPanel(parent);
		addWelcomeMessage();
	}

	/**
	 * Modern collapsible card showing web search results.
	 */
	private void createSearchResultsPanel(Composite parent) {
		Button toggle = new Button(parent, SWT.CHECK);
		toggle.setText("🌐 Web Search Context Panel");
		toggle.setFont(styles.getSmallFont());
		toggle.setSelection(false);
		toggle.addListener(SWT.Selection, e -> {
			if (searchResultsPanel != null && !searchResultsPanel.isDisposed()) {
				searchResultsPanel.setVisible(toggle.getSelection());
				((GridData) searchResultsPanel.getLayoutData()).exclude = !toggle.getSelection();
				parent.layout(true, true);
			}
		});

		searchResultsPanel = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 8;
		layout.marginHeight = 6;
		searchResultsPanel.setLayout(layout);
		GridData panelData = new GridData(SWT.FILL, SWT.FILL, true, false);
		panelData.exclude = true;
		searchResultsPanel.setLayoutData(panelData);
		searchResultsPanel.setVisible(false);

		searchResultsText = new StyledText(searchResultsPanel, SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
		GridData textData = new GridData(SWT.FILL, SWT.FILL, true, false);
		textData.heightHint = 85;
		searchResultsText.setLayoutData(textData);
		searchResultsText.setWordWrap(true);
		searchResultsText.setBackground(styles.getAssistantBubbleBackground());
		searchResultsText.setForeground(styles.getAssistantBubbleForeground());
	}

	/**
	 * Hero welcome state with interactive action chips.
	 */
	private void addWelcomeMessage() {
		Composite welcome = new Composite(messagesContainer, SWT.BORDER);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 16;
		layout.marginHeight = 16;
		layout.verticalSpacing = 8;
		welcome.setLayout(layout);
		welcome.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		welcome.setBackground(styles.getAssistantBubbleBackground());

		Label logo = new Label(welcome, SWT.CENTER);
		logo.setText("🦙 EclipseLlama");
		logo.setFont(styles.getHeaderFont());
		logo.setForeground(styles.getHeaderColor());
		logo.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		logo.setBackground(styles.getAssistantBubbleBackground());

		Label tagline = new Label(welcome, SWT.CENTER);
		tagline.setText("Your AI coding assistant • Local Ollama • OpenAI • Model Context Protocol (MCP)");
		tagline.setFont(styles.getNormalFont());
		tagline.setForeground(styles.getTimestampColor());
		tagline.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		tagline.setBackground(styles.getAssistantBubbleBackground());

		// Quick action chips row
		Composite chipsContainer = new Composite(welcome, SWT.NONE);
		GridLayout chipsLayout = new GridLayout(5, false);
		chipsLayout.marginWidth = 0;
		chipsLayout.marginHeight = 8;
		chipsLayout.horizontalSpacing = 6;
		chipsContainer.setLayout(chipsLayout);
		chipsContainer.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		chipsContainer.setBackground(styles.getAssistantBubbleBackground());

		createChip(chipsContainer, "⚡ Explain Code",
				() -> triggerQuickPrompt("Explain this code step-by-step with complexity analysis:\n"));
		createChip(chipsContainer, "✨ Fix Bugs",
				() -> triggerQuickPrompt("Find and fix bugs in this code, explaining the root cause:\n"));
		createChip(chipsContainer, "🧪 Generate Tests",
				() -> triggerQuickPrompt("Generate comprehensive JUnit 4 unit tests covering edge cases for:\n"));
		createChip(chipsContainer, "📖 Add Javadoc",
				() -> triggerQuickPrompt("Generate clean Javadoc documentation and parameter descriptions for:\n"));
		createChip(chipsContainer, "🔄 Refactor Code", () -> triggerQuickPrompt(
				"Refactor this code for optimal readability, performance, and clean architecture:\n"));
	}

	private void createChip(Composite parent, String text, Runnable onClick) {
		Button chip = new Button(parent, SWT.PUSH | SWT.FLAT);
		chip.setText(text);
		chip.setFont(styles.getSmallFont());
		chip.setBackground(styles.getChipBackground());
		chip.setForeground(styles.getChipForeground());
		chip.addListener(SWT.Selection, e -> onClick.run());
	}

	private void triggerQuickPrompt(String prefix) {
		inputField.setText(prefix);
		inputField.setFocus();
		inputField.setSelection(prefix.length());
	}

	private void createInputArea(Composite parent) {
		Composite inputArea = new Composite(parent, SWT.NONE);
		GridLayout inputLayout = new GridLayout(2, false);
		inputLayout.marginWidth = 4;
		inputLayout.marginHeight = 4;
		inputArea.setLayout(inputLayout);
		inputArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Input field
		inputField = new Text(inputArea, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData inputData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		inputData.heightHint = 60;
		inputField.setLayoutData(inputData);
		inputField.setMessage("Ask anything... (Enter to send, Shift+Enter for newline)");

		inputField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					if ((e.stateMask & SWT.SHIFT) == 0) {
						e.doit = false;
						sendMessage();
					}
				}
			}
		});

		inputField.addModifyListener(e -> {
			int len = inputField.getText().length();
			charCountLabel.setText(len + " chars");
		});

		// Buttons composite
		Composite buttons = new Composite(inputArea, SWT.NONE);
		GridLayout btnLayout = new GridLayout(1, false);
		btnLayout.marginWidth = 0;
		btnLayout.marginHeight = 0;
		buttons.setLayout(btnLayout);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		sendButton = new Button(buttons, SWT.PUSH);
		sendButton.setText("Send ➤");
		sendButton.setFont(styles.getBoldFont());
		sendButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		sendButton.addListener(SWT.Selection, e -> sendMessage());

		stopButton = new Button(buttons, SWT.PUSH);
		stopButton.setText("Stop ⏹");
		stopButton.setEnabled(false);
		stopButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		stopButton.addListener(SWT.Selection, e -> stopStreaming());

		// Status bar
		Composite statusBar = new Composite(parent, SWT.NONE);
		GridLayout statusLayout = new GridLayout(3, false);
		statusLayout.marginWidth = 6;
		statusLayout.marginHeight = 2;
		statusBar.setLayout(statusLayout);
		statusBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		statusDot = new Label(statusBar, SWT.NONE);
		statusDot.setText("🟢");
		statusDot.setFont(styles.getSmallFont());

		statusLabel = new Label(statusBar, SWT.NONE);
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		statusLabel.setText("Ready");
		statusLabel.setFont(styles.getSmallFont());

		charCountLabel = new Label(statusBar, SWT.RIGHT);
		charCountLabel.setText("0 chars");
		charCountLabel.setFont(styles.getSmallFont());
		charCountLabel.setForeground(styles.getTimestampColor());
	}

	private void addSystemMessage() {
		String systemPrompt = "You are EclipseLlama, a helpful AI coding assistant. "
				+ "You help developers write, understand, and improve their code. "
				+ "Be concise and provide code examples when helpful.";
		conversation.add(ChatMessage.system(systemPrompt));
	}

	private void loadHistory() {
		List<ChatMessage> saved = historyStore.load();
		for (ChatMessage m : saved) {
			conversation.add(m);
			if (m.isUser()) {
				addMessageBubble("👤 You", m.getContent(), true);
			} else if (m.isAssistant()) {
				addMessageBubble("🦙 EclipseLlama", m.getContent(), false);
			}
		}
		if (!saved.isEmpty()) {
			messagesContainer.layout(true, true);
			refreshMinSize();
		}
	}

	private void refreshModels() {
		updateStatus("🟡", "Loading models...");
		modelCombo.removeAll();

		new Thread(() -> {
			String[] models = ClientProvider.getClient().getAvailableModels();
			Display display = Display.getDefault();
			if (display == null || display.isDisposed()) {
				return;
			}
			display.asyncExec(() -> {
				if (modelCombo == null || modelCombo.isDisposed()) {
					return;
				}
				if (models == null || models.length == 0) {
					updateStatus("🔴", "Endpoint unreachable. Check settings.");
					for (String model : EclipseLlamaPreferences.getRecommendedCodeModels()) {
						modelCombo.add(model);
					}
				} else {
					for (String model : models) {
						modelCombo.add(model);
					}
					updateStatus("🟢", "Ready • " + models.length + " models available");
				}

				String currentModel = EclipseLlamaPreferences.getModel();
				int index = modelCombo.indexOf(currentModel);
				if (index >= 0) {
					modelCombo.select(index);
				} else if (modelCombo.getItemCount() > 0) {
					modelCombo.select(0);
				}
			});
		}).start();
	}

	private void updateStatus(String dot, String text) {
		Display display = Display.getDefault();
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> {
			if (statusDot != null && !statusDot.isDisposed()) {
				statusDot.setText(dot);
			}
			if (statusLabel != null && !statusLabel.isDisposed()) {
				statusLabel.setText(text);
			}
		});
	}

	private void sendMessage() {
		String input = inputField.getText().trim();
		if (input.isEmpty()) {
			return;
		}

		if (isStreaming) {
			updateStatus("⚠️", "Already thinking... please wait or click Stop.");
			return;
		}

		// Add clean user message to persistent conversation
		ChatMessage userMsg = ChatMessage.user(input);
		conversation.add(userMsg);
		addMessageBubble("👤 You", input, true);

		inputField.setText("");
		charCountLabel.setText("0 chars");

		String selectedModel = modelCombo.getText();
		if (selectedModel == null || selectedModel.isEmpty()) {
			selectedModel = EclipseLlamaPreferences.getModel();
		}

		if (selectedModel == null || selectedModel.isEmpty()) {
			addMessageBubble("🦙 EclipseLlama",
					"⚠️ No model selected. Please select a model in the toolbar or settings.", false);
			return;
		}

		final String model = selectedModel;
		startStreaming();
		currentResponse = new StringBuilder();

		// Create assistant bubble for streaming
		currentAssistantBubble = addMessageBubble("🦙 EclipseLlama • " + model, "", false);

		// Execute web search, MCP probing, and inference asynchronously
		CompletableFuture.runAsync(() -> {
			try {
				if (!isStreaming) {
					return;
				}

				// Execute web search if enabled and get ephemeral search prompt
				String searchPrompt = executeWebSearchIfNeeded(input, model);

				if (!isStreaming) {
					return;
				}

				// Build memory-aware sliding window (prevents context blowout)
				List<ChatMessage> activeWindow = getActiveConversationWindow(searchPrompt);

				LlmProviderRegistry registry = LlmProviderRegistry.getInstance();
				LlmProvider provider;
				if (registry.hasExplicitActiveProvider()) {
					provider = registry.getActiveProvider().orElse(null);
				} else {
					String backendName = EclipseLlamaPreferences
							.getBackendType() == EclipseLlamaPreferences.BackendType.OPENAI ? "OpenAI" : "Ollama";
					provider = registry.getProvider(backendName).or(() -> registry.getActiveProvider()).orElse(null);
				}

				if (provider == null) {
					ClientProvider.getClient().streamChat(activeWindow, model, this::onChunk, this::onComplete,
							this::onError);
					return;
				}

				StringBuilder full = new StringBuilder();
				String prompt = buildConversationPrompt(activeWindow);
				provider.stream(prompt, lastContext).forEach(chunk -> {
					if (isStreaming && chunk != null && !chunk.isEmpty()) {
						full.append(chunk);
						onChunk(chunk);
					}
				});

				if (isStreaming) {
					onComplete(full.toString());
				}
			} catch (Exception e) {
				onError(e.getMessage());
			}
		});
	}

	/**
	 * Memory-aware sliding window: returns system prompt + recent N messages within
	 * token/character budget to prevent blowing up the context window.
	 */
	private List<ChatMessage> getActiveConversationWindow(String currentSearchPrompt) {
		List<ChatMessage> window = new ArrayList<>();
		if (conversation.isEmpty()) {
			return window;
		}

		ChatMessage systemMsg = conversation.get(0).isSystem() ? conversation.get(0) : null;
		if (systemMsg != null) {
			window.add(systemMsg);
		}

		List<ChatMessage> recent = new ArrayList<>();
		int charCount = 0;
		int startIndex = conversation.size() - 1;

		for (int i = startIndex; i >= (systemMsg != null ? 1 : 0); i--) {
			ChatMessage msg = conversation.get(i);
			int len = msg.getContent().length();
			if (recent.size() >= MAX_CONTEXT_MESSAGES || (charCount + len > MAX_CONTEXT_CHARS && !recent.isEmpty())) {
				break;
			}
			recent.add(0, msg);
			charCount += len;
		}

		// Apply ephemeral search context only to the current user prompt in the active
		// window
		if (currentSearchPrompt != null && !recent.isEmpty()) {
			int lastIdx = recent.size() - 1;
			ChatMessage last = recent.get(lastIdx);
			if (last.isUser()) {
				recent.set(lastIdx, ChatMessage.user(currentSearchPrompt));
			}
		}

		window.addAll(recent);
		return window;
	}

	private String executeWebSearchIfNeeded(String input, String model) {
		String mode = EclipseLlamaPreferences.getSearchMode();
		if ("off".equals(mode)) {
			return null;
		}

		boolean shouldSearch = false;
		String query = input;

		if ("always".equals(mode)) {
			shouldSearch = true;
		} else {
			WebSearchRouter.SearchDecision decision = new WebSearchRouter(ClientProvider.getClient()).decide(input,
					model);
			if (decision.isSearch()) {
				query = decision.getQuery();
				if ("ask".equals(mode)) {
					final String q = query;
					final boolean[] confirmed = new boolean[1];
					Display.getDefault().syncExec(() -> {
						confirmed[0] = confirmSearch(q);
					});
					shouldSearch = confirmed[0];
				} else {
					shouldSearch = true;
				}
			}
		}

		if (shouldSearch && isStreaming) {
			final String q = query;
			updateStatus("🌐", "Searching the web (" + q + ")...");

			WebSearchService service = webSearchService;
			List<SearchResult> results = service.search(q);
			String prompt = service.buildPrompt(input, results);

			Display.getDefault().asyncExec(() -> {
				renderSearchResults(q, results);
				updateStatus("🦙", "Generating response...");
			});
			return prompt;
		}
		return null;
	}

	private String buildConversationPrompt(List<ChatMessage> window) {
		StringBuilder sb = new StringBuilder();
		for (ChatMessage msg : window) {
			String role = msg.isSystem() ? "System" : (msg.isUser() ? "User" : "Assistant");
			sb.append(role).append(": ").append(msg.getContent()).append('\n');
		}
		String mcpContext = buildMcpStatusBlock();
		if (!mcpContext.isEmpty()) {
			sb.append("\n[EclipseLlama configured MCP servers - Model Context Protocol AI tools]\n").append(mcpContext)
					.append("\n[/MCP servers]\n");
		}
		return sb.toString();
	}

	private String buildMcpStatusBlock() {
		StringBuilder sb = new StringBuilder();
		for (LlmProvider p : LlmProviderRegistry.getInstance().getAllProviders().values()) {
			if (p instanceof com.eclipsellama.plugin.mcp.McpProvider) {
				com.eclipsellama.plugin.mcp.McpProvider mcp = (com.eclipsellama.plugin.mcp.McpProvider) p;
				com.eclipsellama.plugin.mcp.McpServerConfig cfg = mcp.getConfig();
				com.eclipsellama.plugin.mcp.McpProvider.ProbeResult probe = mcp.probeStatus();
				String serverName = (cfg.getName() != null && !cfg.getName().isBlank()) ? cfg.getName() : cfg.getId();
				sb.append("- server \"").append(serverName).append("\": transport=").append(cfg.getTransport());
				if (cfg.getEndpoint() != null && !cfg.getEndpoint().isEmpty()) {
					sb.append(", endpoint=").append(cfg.getEndpoint());
				}
				sb.append(", status=").append(probe.connected ? "connected" : "unreachable");
				if (!probe.connected) {
					sb.append(" (").append(probe.detail).append(")");
				}
				sb.append(", tools=").append(probe.tools.isEmpty() ? "none" : String.join(", ", probe.tools));
				sb.append('\n');
			}
		}
		return sb.toString();
	}

	private WebSearchService createWebSearchService() {
		int max = EclipseLlamaPreferences.getSearchMaxResults();
		java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder()
				.followRedirects(java.net.http.HttpClient.Redirect.NORMAL).build();
		var searxng = new com.eclipsellama.plugin.search.SearXNGSearchProvider(
				EclipseLlamaPreferences.getSearchSearxngEndpoint(), max, http);
		var brave = new com.eclipsellama.plugin.search.BraveSearchProvider(EclipseLlamaPreferences.getSearchApiKey(),
				max, http);
		var builtin = new com.eclipsellama.plugin.search.BuiltInSearchProvider(max, http,
				new com.eclipsellama.plugin.search.parser.DuckDuckGoHtmlParser());
		var registry = new com.eclipsellama.plugin.search.SearchProviderRegistry(List.of(builtin, searxng, brave));
		return new WebSearchService(registry);
	}

	private boolean confirmSearch(String query) {
		Display display = Display.getDefault();
		if (display == null || display.isDisposed()) {
			return false;
		}
		MessageBox box = new MessageBox(display.getActiveShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		box.setText("Web Search Confirmation");
		box.setMessage(
				"EclipseLlama wants to search the web:\n\n\"" + query + "\"\n\nAllow web search to enrich response?");
		return box.open() == SWT.YES;
	}

	private void updateSearchModeCombo() {
		if (searchModeCombo == null || searchModeCombo.isDisposed()) {
			return;
		}
		String mode = EclipseLlamaPreferences.getSearchMode();
		searchModeCombo.select(indexOfMode(mode));
	}

	private static String mapModeLabelToKey(String label) {
		return switch (label) {
		case "Off" -> "off";
		case "Smart" -> "smart";
		case "Ask" -> "ask";
		case "Always" -> "always";
		default -> "smart";
		};
	}

	private static int indexOfMode(String mode) {
		return switch (mode) {
		case "off" -> 0;
		case "smart" -> 1;
		case "ask" -> 2;
		case "always" -> 3;
		default -> 1;
		};
	}

	private void renderSearchResults(String query, List<SearchResult> results) {
		if (searchResultsText == null || searchResultsText.isDisposed()) {
			return;
		}
		StringBuilder sb = new StringBuilder("🔍 Search Results for \"").append(query).append("\"\n\n");
		int i = 1;
		if (results != null) {
			for (SearchResult r : results) {
				sb.append(i++).append(". ").append(r.getTitle() == null ? "" : r.getTitle()).append('\n')
						.append("   🔗 ").append(r.getUrl() == null ? "" : r.getUrl()).append('\n').append("   ")
						.append(r.getSnippet() == null ? "" : r.getSnippet()).append("\n\n");
			}
		}
		searchResultsText.setText(sb.toString());
	}

	/**
	 * Modern Card Message Bubble with header pill, timestamp, and actions toolbar.
	 */
	private StyledText addMessageBubble(String sender, String content, boolean isUser) {
		Composite bubble = new Composite(messagesContainer, SWT.BORDER);
		GridLayout bubbleLayout = new GridLayout(1, false);
		bubbleLayout.marginWidth = 12;
		bubbleLayout.marginHeight = 10;
		bubbleLayout.verticalSpacing = 6;
		bubble.setLayout(bubbleLayout);

		GridData bubbleData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		bubble.setLayoutData(bubbleData);

		Color bgColor = isUser ? styles.getUserBubbleBackground() : styles.getAssistantBubbleBackground();
		Color fgColor = isUser ? styles.getUserBubbleForeground() : styles.getAssistantBubbleForeground();
		bubble.setBackground(bgColor);

		// Header Row: Sender label on left, Timestamp on right
		Composite headerRow = new Composite(bubble, SWT.NONE);
		GridLayout headerLayout = new GridLayout(2, false);
		headerLayout.marginWidth = 0;
		headerLayout.marginHeight = 0;
		headerRow.setLayout(headerLayout);
		headerRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		headerRow.setBackground(bgColor);

		Label senderLabel = new Label(headerRow, SWT.NONE);
		senderLabel.setText(sender);
		senderLabel.setFont(styles.getBoldFont());
		senderLabel.setForeground(isUser ? styles.getLinkColor() : styles.getHeaderColor());
		senderLabel.setBackground(bgColor);

		Label timeLabel = new Label(headerRow, SWT.RIGHT);
		timeLabel.setText(timeFormat.format(new Date()));
		timeLabel.setFont(styles.getSmallFont());
		timeLabel.setForeground(styles.getTimestampColor());
		timeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		timeLabel.setBackground(bgColor);

		// Message text content
		StyledText messageText = new StyledText(bubble, SWT.WRAP | SWT.READ_ONLY);
		messageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		messageText.setBackground(bgColor);
		messageText.setForeground(fgColor);
		messageText.setWordWrap(true);

		applyMarkdownStyles(messageText, content);

		// Code block actions toolbar (Copy + Insert in Editor)
		if (content != null && (content.contains("```") || markdownRenderer.hasCodeBlocks(content))) {
			addCodeActionsToolbar(bubble, content, bgColor);
		}

		messagesContainer.layout(true, true);
		refreshMinSize();
		scrollToBottom();

		return messageText;
	}

	private void applyMarkdownStyles(StyledText textWidget, String content) {
		if (textWidget == null || textWidget.isDisposed()) {
			return;
		}
		List<MarkdownRenderer.StyledSegment> segments = markdownRenderer.parse(content);

		StringBuilder visualText = new StringBuilder();
		List<StyleRange> ranges = new ArrayList<>();

		for (MarkdownRenderer.StyledSegment seg : segments) {
			int start = visualText.length();
			visualText.append(seg.text);

			StyleRange sr = markdownRenderer.createStyleRange(seg, start);
			if (sr != null) {
				ranges.add(sr);
			}
		}

		textWidget.setText(visualText.toString());
		textWidget.setStyleRanges(ranges.toArray(new StyleRange[0]));
	}

	/**
	 * Adds modern action toolbar with Copy and Insert at Cursor buttons.
	 */
	private void addCodeActionsToolbar(Composite parent, String content, Color bgColor) {
		Composite toolbar = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 4;
		layout.horizontalSpacing = 6;
		toolbar.setLayout(layout);
		toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		toolbar.setBackground(bgColor);

		// Copy Button
		Button copyBtn = new Button(toolbar, SWT.PUSH | SWT.FLAT);
		copyBtn.setText("📋 Copy Code");
		copyBtn.setFont(styles.getSmallFont());
		copyBtn.setBackground(styles.getChipBackground());
		copyBtn.setForeground(styles.getChipForeground());
		copyBtn.addListener(SWT.Selection, e -> {
			String code = extractCodeOnly(content);
			copyToClipboard(code);
			copyBtn.setText("✅ Copied!");
			Display.getDefault().timerExec(2000, () -> {
				if (!copyBtn.isDisposed()) {
					copyBtn.setText("📋 Copy Code");
				}
			});
		});

		// Insert at Cursor Button
		Button insertBtn = new Button(toolbar, SWT.PUSH | SWT.FLAT);
		insertBtn.setText("📥 Insert in Editor");
		insertBtn.setFont(styles.getSmallFont());
		insertBtn.setToolTipText("Insert this code at current cursor in active Java editor");
		insertBtn.setBackground(styles.getChipBackground());
		insertBtn.setForeground(styles.getChipForeground());
		insertBtn.addListener(SWT.Selection, e -> {
			String code = extractCodeOnly(content);
			boolean inserted = insertIntoActiveEditor(code);
			if (inserted) {
				insertBtn.setText("✅ Inserted!");
				Display.getDefault().timerExec(2000, () -> {
					if (!insertBtn.isDisposed()) {
						insertBtn.setText("📥 Insert in Editor");
					}
				});
			}
		});
	}

	private String extractCodeOnly(String content) {
		List<MarkdownRenderer.CodeBlock> blocks = markdownRenderer.extractCodeBlocks(content);
		if (blocks.isEmpty()) {
			return content;
		}
		StringBuilder sb = new StringBuilder();
		for (MarkdownRenderer.CodeBlock b : blocks) {
			sb.append(b.code).append('\n');
		}
		return sb.toString();
	}

	/**
	 * Inserts code directly at the cursor position in the active Eclipse Java text
	 * editor.
	 */
	private boolean insertIntoActiveEditor(String text) {
		try {
			IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			if (editor instanceof ITextEditor textEditor) {
				IDocumentProvider provider = textEditor.getDocumentProvider();
				IDocument doc = provider.getDocument(textEditor.getEditorInput());
				ITextSelection sel = (ITextSelection) textEditor.getSelectionProvider().getSelection();
				if (doc != null && sel != null) {
					doc.replace(sel.getOffset(), sel.getLength(), text);
					return true;
				}
			}
		} catch (BadLocationException ignored) {
		}
		return false;
	}

	private void copyToClipboard(String text) {
		Display display = Display.getDefault();
		if (display == null || display.isDisposed()) {
			return;
		}
		Clipboard clipboard = new Clipboard(display);
		try {
			TextTransfer textTransfer = TextTransfer.getInstance();
			clipboard.setContents(new Object[] { text }, new Transfer[] { textTransfer });
		} finally {
			clipboard.dispose();
		}
	}

	/**
	 * Recalculates the scroll range using the actual viewport width so that
	 * word-wrapped text is measured correctly. Must be called on the UI thread.
	 */
	private void refreshMinSize() {
		int width = scrolledComposite.getClientArea().width;
		if (width > 0) {
			messagesContainer.setSize(width, messagesContainer.computeSize(width, SWT.DEFAULT).y);
			scrolledComposite.setMinSize(messagesContainer.computeSize(width, SWT.DEFAULT));
		} else {
			scrolledComposite.setMinSize(messagesContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		}
	}

	private void scrollToBottom() {
		Display display = Display.getDefault();
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> {
			if (scrolledComposite != null && !scrolledComposite.isDisposed()) {
				scrolledComposite.setOrigin(0, scrolledComposite.getMinHeight());
			}
		});
	}

	private void startStreaming() {
		isStreaming = true;
		sendButton.setEnabled(false);
		stopButton.setEnabled(true);
		updateStatus("🦙", "Thinking...");
	}

	private void stopStreaming() {
		isStreaming = false;
		sendButton.setEnabled(true);
		stopButton.setEnabled(false);
		updateStatus("⏹️", "Stopped");
	}

	private void onChunk(String chunk) {
		if (!isStreaming || currentAssistantBubble == null || currentAssistantBubble.isDisposed()) {
			return;
		}

		currentResponse.append(chunk);
		Display display = Display.getDefault();
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> {
			if (!isStreaming) {
				return;
			}
			if (currentAssistantBubble != null && !currentAssistantBubble.isDisposed()) {
				currentAssistantBubble.append(chunk);
				messagesContainer.layout(true, true);
				refreshMinSize();
				scrollToBottom();
			}
		});
	}

	private void onComplete(String fullResponse) {
		if (!isStreaming) {
			return;
		}
		isStreaming = false;

		String responseText = (fullResponse != null && !fullResponse.isEmpty()) ? fullResponse
				: currentResponse.toString();
		conversation.add(ChatMessage.assistant(responseText));
		historyStore.save(conversation);

		Display display = Display.getDefault();
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> {
			if (sendButton != null && !sendButton.isDisposed()) {
				sendButton.setEnabled(true);
			}
			if (stopButton != null && !stopButton.isDisposed()) {
				stopButton.setEnabled(false);
			}
			updateStatus("🟢", "Ready");

			if (currentAssistantBubble != null && !currentAssistantBubble.isDisposed()) {
				applyMarkdownStyles(currentAssistantBubble, responseText);

				if (markdownRenderer.hasCodeBlocks(responseText)) {
					Composite parent = currentAssistantBubble.getParent();
					addCodeActionsToolbar(parent, responseText, styles.getAssistantBubbleBackground());
					parent.layout(true, true);
				}
			}

			if ("fix".equals(lastAction) && lastContext != null && !lastContext.isEmpty()) {
				CodeDiffDialog.show("Code Fix Suggestion", lastContext, responseText);
			}

			if (messagesContainer != null && !messagesContainer.isDisposed()) {
				messagesContainer.layout(true, true);
				refreshMinSize();
				scrollToBottom();
			}
		});
	}

	private void onError(String error) {
		isStreaming = false;

		Display display = Display.getDefault();
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> {
			if (currentAssistantBubble != null && !currentAssistantBubble.isDisposed()) {
				currentAssistantBubble.append("\n❌ Error: " + error);
				currentAssistantBubble.setForeground(styles.getErrorColor());
			}
			if (sendButton != null && !sendButton.isDisposed()) {
				sendButton.setEnabled(true);
			}
			if (stopButton != null && !stopButton.isDisposed()) {
				stopButton.setEnabled(false);
			}
			updateStatus("🔴", "Error occurred: " + error);
		});
	}

	private void clearConversation() {
		for (Control child : messagesContainer.getChildren()) {
			child.dispose();
		}

		conversation.clear();
		addSystemMessage();
		historyStore.save(conversation);
		addWelcomeMessage();

		messagesContainer.layout(true, true);
		refreshMinSize();
		updateStatus("🟢", "Conversation cleared");
	}

	@Override
	public void setFocus() {
		if (inputField != null && !inputField.isDisposed()) {
			inputField.setFocus();
		}
	}

	@Override
	public void dispose() {
		isStreaming = false;
		super.dispose();
	}

	public void setContext(String context, String action) {
		this.lastAction = action;
		this.lastContext = context;

		String contextPrompt = buildContextAwarePrompt(context, action);
		inputField.setText(contextPrompt);
		inputField.setFocus();

		Display display = Display.getDefault();
		if (display != null && !display.isDisposed()) {
			display.asyncExec(() -> {
				if (!inputField.isDisposed()) {
					sendMessage();
				}
			});
		}
	}

	private String buildContextAwarePrompt(String selectedCode, String action) {
		if (selectedCode == null || selectedCode.isBlank()) {
			return selectedCode == null ? "" : selectedCode;
		}

		CodeContextCollector.Context ctx = CodeContextCollector.collect(selectedCode);
		StringBuilder prompt = new StringBuilder();

		String prefix = ctx.toPromptPrefix();
		if (!prefix.isBlank()) {
			prompt.append(prefix);
		}

		String instruction = switch (action) {
		case "explain" -> "Explain this code:";
		case "fix" -> "Find and fix any issues in this code:";
		case "test" -> "Generate unit tests for this code:";
		case "document" -> "Generate Javadoc documentation for this code:";
		case "refactor" -> "Refactor this code for clarity and maintainability:";
		case "review" -> "Review this code and list issues, risks, and suggestions:";
		case "convert" -> "Convert this code to a different language/format:";
		default -> selectedCode;
		};

		prompt.append(instruction).append("\n```\n").append(selectedCode).append("\n```");
		return prompt.toString();
	}
}