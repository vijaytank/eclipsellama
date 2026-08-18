package com.eclipsellama.plugin.ui.chat;

import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

import com.eclipsellama.plugin.context.CodeContextCollector;
import com.eclipsellama.plugin.core.ChatMessage;
import com.eclipsellama.plugin.core.ClientProvider;
import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;
import com.eclipsellama.plugin.search.WebSearchService;
import com.eclipsellama.plugin.ui.CodeDiffDialog;

/**
 * Modern chat view for EclipseLlama. Features: message bubbles, markdown
 * rendering, streaming responses, copy code.
 */
public class ChatView extends ViewPart {

	public static final String ID = "com.eclipsellama.plugin.view.chat";

	// UI Components
	private ScrolledComposite scrolledComposite;
	private Composite messagesContainer;
	private Text inputField;
	private Button sendButton;
	private Button stopButton;
	private Combo modelCombo;
	private Label statusLabel;
	private Label charCountLabel;

	// State
	private final List<ChatMessage> conversation = new ArrayList<>();
	private boolean isStreaming = false;
	private boolean webSearchEnabled = false;
	private StringBuilder currentResponse;
	private StyledText currentAssistantBubble;
	// Track last action and context for diff and context-aware prompts
	private String lastAction = "";
	private String lastContext = "";

	// Web search
	private WebSearchService webSearchService = createWebSearchService();
	private Combo searchModeCombo;
	private Composite searchResultsPanel;
	private StyledText searchResultsText;

	// Persistence
	private final com.eclipsellama.plugin.storage.ChatHistoryStore historyStore = new com.eclipsellama.plugin.storage.ChatHistoryStore();

	// Styling
	private ChatStyles styles;
	private MarkdownRenderer markdownRenderer;

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
		webSearchEnabled = !"off".equals(EclipseLlamaPreferences.getSearchMode());
		updateSearchModeCombo();
	}

	private void createToolbar(Composite parent) {
		Composite toolbar = new Composite(parent, SWT.NONE);
		toolbar.setLayout(new GridLayout(7, false));
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Model selector
		Label modelLabel = new Label(toolbar, SWT.NONE);
		modelLabel.setText("🦙 Model:");

		modelCombo = new Combo(toolbar, SWT.DROP_DOWN | SWT.READ_ONLY);
		modelCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		modelCombo.addListener(SWT.Selection, e -> {
			String selected = modelCombo.getText();
			if (!selected.isEmpty()) {
				EclipseLlamaPreferences.setModel(selected);
				EclipseLlamaPreferences.save();
			}
		});

		// Refresh button
		Button refreshBtn = new Button(toolbar, SWT.PUSH);
		refreshBtn.setText("🔄");
		refreshBtn.setToolTipText("Refresh available models");
		refreshBtn.addListener(SWT.Selection, e -> refreshModels());

		// Clear button
		Button clearBtn = new Button(toolbar, SWT.PUSH);
		clearBtn.setText("🗑️");
		clearBtn.setToolTipText("Clear conversation");
		clearBtn.addListener(SWT.Selection, e -> clearConversation());

		// Web Search mode selector
		Label searchModeLabel = new Label(toolbar, SWT.NONE);
		searchModeLabel.setText("🌐 Search:");

		searchModeCombo = new Combo(toolbar, SWT.DROP_DOWN | SWT.READ_ONLY);
		searchModeCombo.setItems(new String[] { "Off", "Smart", "Ask", "Always" });
		searchModeCombo.setToolTipText(
				"Off: no search. Smart: model decides. Ask: confirm before search. Always: search every message.");
		searchModeCombo.addListener(SWT.Selection, e -> {
			String mode = mapModeLabelToKey(searchModeCombo.getText());
			EclipseLlamaPreferences.setSearchMode(mode);
			EclipseLlamaPreferences.save();
			webSearchEnabled = !"off".equals(mode);
		});

		// Settings button
		Button settingsBtn = new Button(toolbar, SWT.PUSH);
		settingsBtn.setText("⚙️");
		settingsBtn.setToolTipText("Open preferences");
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
		messagesContainer.setLayout(new GridLayout(1, false));
		messagesContainer.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		scrolledComposite.setContent(messagesContainer);

		createSearchResultsPanel(parent);
		// Welcome message
		addWelcomeMessage();
	}

	/**
	 * Collapsible panel that shows the last web search results (title, URL,
	 * snippet) above the chat output.
	 */
	private void createSearchResultsPanel(Composite parent) {
		Button toggle = new Button(parent, SWT.CHECK);
		toggle.setText("Show web search results");
		toggle.setSelection(false);
		toggle.addListener(SWT.Selection, e -> {
			if (searchResultsPanel != null && !searchResultsPanel.isDisposed()) {
				searchResultsPanel.setVisible(toggle.getSelection());
				parent.layout(true, true);
			}
		});

		searchResultsPanel = new Composite(parent, SWT.BORDER);
		searchResultsPanel.setLayout(new GridLayout(1, false));
		searchResultsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		searchResultsPanel.setVisible(false);

		searchResultsText = new StyledText(searchResultsPanel, SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
		GridData textData = new GridData(SWT.FILL, SWT.FILL, true, true);
		textData.heightHint = 120;
		searchResultsText.setLayoutData(textData);
		searchResultsText.setWordWrap(true);
	}

	private void addWelcomeMessage() {
		Composite welcome = new Composite(messagesContainer, SWT.NONE);
		welcome.setLayout(new GridLayout(1, false));
		welcome.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		welcome.setBackground(messagesContainer.getBackground());

		Label logo = new Label(welcome, SWT.CENTER);
		logo.setText("🦙 EclipseLlama");
		logo.setFont(styles.getHeaderFont());
		logo.setForeground(styles.getHeaderColor());
		logo.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		logo.setBackground(messagesContainer.getBackground());

		Label tagline = new Label(welcome, SWT.CENTER);
		tagline.setText("Your AI coding assistant • Powered by Ollama");
		tagline.setForeground(styles.getTimestampColor());
		tagline.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		tagline.setBackground(messagesContainer.getBackground());

		Label hint = new Label(welcome, SWT.CENTER);
		hint.setText("\n💡 Tip: Select code and right-click → EclipseLlama for quick actions\n");
		hint.setForeground(styles.getTimestampColor());
		hint.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		hint.setBackground(messagesContainer.getBackground());
	}

	private void createInputArea(Composite parent) {
		Composite inputArea = new Composite(parent, SWT.NONE);
		inputArea.setLayout(new GridLayout(3, false));
		inputArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Input field
		inputField = new Text(inputArea, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData inputData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		inputData.heightHint = 60;
		inputField.setLayoutData(inputData);
		inputField.setMessage("Ask anything... (Enter to send, Shift+Enter for new line)");

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
		buttons.setLayout(new GridLayout(1, false));

		sendButton = new Button(buttons, SWT.PUSH);
		sendButton.setText("Send ➤");
		sendButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		sendButton.addListener(SWT.Selection, e -> sendMessage());

		stopButton = new Button(buttons, SWT.PUSH);
		stopButton.setText("Stop ⏹");
		stopButton.setEnabled(false);
		stopButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		stopButton.addListener(SWT.Selection, e -> stopStreaming());

		// Status bar
		Composite statusBar = new Composite(parent, SWT.NONE);
		statusBar.setLayout(new GridLayout(2, false));
		statusBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		statusLabel = new Label(statusBar, SWT.NONE);
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		statusLabel.setText("Ready");

		charCountLabel = new Label(statusBar, SWT.RIGHT);
		charCountLabel.setText("0 chars");
		charCountLabel.setForeground(styles.getTimestampColor());
	}

	private void addSystemMessage() {
		String systemPrompt = "You are EclipseLlama, a helpful AI coding assistant. "
				+ "You help developers write, understand, and improve their code. "
				+ "Be concise and provide code examples when helpful.";
		conversation.add(ChatMessage.system(systemPrompt));
	}

	/**
	 * Restores a previously persisted conversation into the chat area.
	 */
	private void loadHistory() {
		java.util.List<ChatMessage> saved = historyStore.load();
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
			scrolledComposite.setMinSize(messagesContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		}
	}

	private void refreshModels() {
		statusLabel.setText("Loading models...");
		modelCombo.removeAll();

		new Thread(() -> {
			String[] models = ClientProvider.getClient().getAvailableModels();
			Display.getDefault().asyncExec(() -> {
				if (models.length == 0) {
					statusLabel.setText("⚠️ No models found. Is endpoint running?");
					for (String model : EclipseLlamaPreferences.getRecommendedCodeModels()) {
						modelCombo.add(model);
					}
				} else {
					for (String model : models) {
						modelCombo.add(model);
					}
					statusLabel.setText("Ready • " + models.length + " models available");
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

	private void sendMessage() {
		String input = inputField.getText().trim();
		if (input.isEmpty()) {
			return;
		}

		if (isStreaming) {
			statusLabel.setText("⚠️ Already thinking... please wait or stop.");
			return;
		}

		// Add user message bubble
		ChatMessage userMsg = ChatMessage.user(input);
		conversation.add(userMsg);
		addMessageBubble("👤 You", input, true);

		// Enrich with web search only when the selected mode warrants it. The model
		// decides in Smart mode; Ask requires user confirmation; Off never searches.
		if (shouldSearch(input)) {
			enrichWithWebSearch();
		}

		inputField.setText("");
		charCountLabel.setText("0 chars");

		String model = modelCombo.getText();
		if (model.isEmpty()) {
			model = EclipseLlamaPreferences.getModel();
		}

		if (model.isEmpty()) {
			addMessageBubble("🦙 EclipseLlama",
					"⚠️ No model selected. Please select a model in the toolbar or settings.", false);
			return;
		}

		startStreaming();
		currentResponse = new StringBuilder();

		// Create assistant bubble for streaming
		currentAssistantBubble = addMessageBubble("🦙 EclipseLlama", "", false);

		ClientProvider.getClient().streamChat(conversation, model, this::onChunk, this::onComplete, this::onError);
	}

	/**
	 * Builds the web search service (registry + built-in default) from preferences.
	 * External providers are only used when configured.
	 */
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
		var registry = new com.eclipsellama.plugin.search.SearchProviderRegistry(
				java.util.List.of(builtin, searxng, brave));
		return new WebSearchService(registry);
	}

	/**
	 * Whether the current message should trigger a web search, based on the
	 * selected mode. Off never searches; Always searches everything; Smart uses the
	 * LLM router; Ask uses the router and then requires user confirmation.
	 */
	private boolean shouldSearch(String message) {
		String mode = EclipseLlamaPreferences.getSearchMode();
		if ("off".equals(mode)) {
			return false;
		}
		if ("always".equals(mode)) {
			return true;
		}
		// smart or ask: ask the model whether search is warranted.
		String model = modelCombo.getText();
		if (model.isEmpty()) {
			model = EclipseLlamaPreferences.getModel();
		}
		com.eclipsellama.plugin.search.WebSearchRouter.SearchDecision decision = new com.eclipsellama.plugin.search.WebSearchRouter(
				ClientProvider.getClient()).decide(message, model);
		if (!decision.isSearch()) {
			return false;
		}
		if ("ask".equals(mode)) {
			return confirmSearch(decision.getQuery());
		}
		return true;
	}

	private boolean confirmSearch(String query) {
		String[] labels = { "Search", "Answer without web" };
		org.eclipse.swt.widgets.MessageBox box = new org.eclipse.swt.widgets.MessageBox(
				Display.getDefault().getActiveShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		box.setText("Web search");
		box.setMessage("The assistant wants to search the web:\n\nQuery: " + query
				+ "\n\n[Yes] Search  [No] Answer without web");
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

	/**
	 * Runs a web search for the last user message and injects the results as a
	 * safe, delimited pre-prompt. Search failures are non-fatal: the message is
	 * sent unchanged.
	 */
	private void enrichWithWebSearch() {
		String query = lastUserMessageContent();
		if (query == null || query.isEmpty()) {
			return;
		}
		statusLabel.setText("🌐 Searching the web...");
		java.util.List<com.eclipsellama.plugin.search.SearchResult> results = webSearchService.search(query);
		String prompt = webSearchService.buildPrompt(query, results);
		prependToLastUserMessage(prompt);
		renderSearchResults(query, results);
		statusLabel.setText("");
	}

	/**
	 * Renders the given results into the collapsible search-results panel.
	 */
	private void renderSearchResults(String query,
			java.util.List<com.eclipsellama.plugin.search.SearchResult> results) {
		if (searchResultsText == null || searchResultsText.isDisposed()) {
			return;
		}
		StringBuilder sb = new StringBuilder("Results for \"").append(query).append("\"\n");
		int i = 1;
		for (com.eclipsellama.plugin.search.SearchResult r : results) {
			sb.append(i++).append(". ").append(r.getTitle() == null ? "" : r.getTitle()).append('\n')
					.append(r.getUrl() == null ? "" : r.getUrl()).append('\n')
					.append(r.getSnippet() == null ? "" : r.getSnippet()).append("\n\n");
		}
		searchResultsText.setText(sb.toString());
	}

	private String lastUserMessageContent() {
		for (int i = conversation.size() - 1; i >= 0; i--) {
			if (conversation.get(i).isUser()) {
				return conversation.get(i).getContent();
			}
		}
		return null;
	}

	private void prependToLastUserMessage(String context) {
		for (int i = conversation.size() - 1; i >= 0; i--) {
			ChatMessage m = conversation.get(i);
			if (m.isUser()) {
				// Replace the message content with context + original (context is a prefix).
				conversation.set(i, ChatMessage.user(context + m.getContent()));
				return;
			}
		}
	}

	/**
	 * Add a styled message bubble to the chat.
	 */
	private StyledText addMessageBubble(String sender, String content, boolean isUser) {
		// Bubble container with padding
		Composite bubble = new Composite(messagesContainer, SWT.NONE);
		GridLayout bubbleLayout = new GridLayout(1, false);
		bubbleLayout.marginWidth = 12;
		bubbleLayout.marginHeight = 8;
		bubble.setLayout(bubbleLayout);

		GridData bubbleData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		bubbleData.widthHint = 500;
		bubble.setLayoutData(bubbleData);

		// Set bubble colors
		Color bgColor = isUser ? styles.getUserBubbleBackground() : styles.getAssistantBubbleBackground();
		bubble.setBackground(bgColor);

		// Sender label
		Label senderLabel = new Label(bubble, SWT.NONE);
		senderLabel.setText(sender);
		senderLabel.setFont(styles.getHeaderFont());
		senderLabel.setForeground(styles.getHeaderColor());
		senderLabel.setBackground(bgColor);

		// Message content
		StyledText messageText = new StyledText(bubble, SWT.WRAP | SWT.READ_ONLY);
		// messageText.setText(content);
		messageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		messageText.setBackground(bgColor);
		messageText.setWordWrap(true);

		// Apply markdown styling
		applyMarkdownStyles(messageText, content);

		// Add copy button if has code
		if (content.contains("```") || markdownRenderer.hasCodeBlocks(content)) {
			addCopyButton(bubble, content, bgColor);
		}

		// Refresh layout
		messagesContainer.layout(true, true);
		scrolledComposite.setMinSize(messagesContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scrollToBottom();

		return messageText;
	}

	/**
	 * Apply markdown styles to text widget.
	 */
	private void applyMarkdownStyles(StyledText textWidget, String content) {
		List<MarkdownRenderer.StyledSegment> segments = markdownRenderer.parse(content);

		StringBuilder visualText = new StringBuilder();
		List<StyleRange> ranges = new ArrayList<>();

		for (MarkdownRenderer.StyledSegment seg : segments) {
			int start = visualText.length();
			visualText.append(seg.text); // Hier kommt NUR der bereinigte Inhalt rein

			StyleRange sr = markdownRenderer.createStyleRange(seg, start);
			if (sr != null) {
				ranges.add(sr);
			}
		}

		// Damit Windows nicht eigenmächtig \r\n einbaut, setzen wir den Text
		// und die Ranges in einem Rutsch.
		textWidget.setText(visualText.toString());
		textWidget.setStyleRanges(ranges.toArray(new StyleRange[0]));
	}

	/**
	 * Add copy button for code blocks.
	 */
	private void addCopyButton(Composite parent, String content, Color bgColor) {
		Button copyBtn = new Button(parent, SWT.PUSH);
		copyBtn.setText("📋 Copy Code");
		copyBtn.setBackground(bgColor);
		copyBtn.addListener(SWT.Selection, e -> {
			// Extract just the code
			List<MarkdownRenderer.CodeBlock> blocks = markdownRenderer.extractCodeBlocks(content);
			StringBuilder codeOnly = new StringBuilder();
			for (MarkdownRenderer.CodeBlock block : blocks) {
				codeOnly.append(block.code).append("\n");
			}

			String textToCopy = codeOnly.length() > 0 ? codeOnly.toString() : content;
			copyToClipboard(textToCopy);
			copyBtn.setText("✅ Copied!");

			// Reset button text after 2 seconds
			Display.getDefault().timerExec(2000, () -> {
				if (!copyBtn.isDisposed()) {
					copyBtn.setText("📋 Copy Code");
				}
			});
		});
	}

	private void copyToClipboard(String text) {
		Clipboard clipboard = new Clipboard(Display.getDefault());
		try {
			TextTransfer textTransfer = TextTransfer.getInstance();
			clipboard.setContents(new Object[] { text }, new Transfer[] { textTransfer });
		} finally {
			clipboard.dispose();
		}
	}

	private void scrollToBottom() {
		Display.getDefault().asyncExec(() -> {
			if (!scrolledComposite.isDisposed()) {
				scrolledComposite.setOrigin(0, messagesContainer.getSize().y);
			}
		});
	}

	private void startStreaming() {
		isStreaming = true;
		sendButton.setEnabled(false);
		stopButton.setEnabled(true);
		statusLabel.setText("🦙 Thinking...");
	}

	private void stopStreaming() {
		isStreaming = false;
		sendButton.setEnabled(true);
		stopButton.setEnabled(false);
		statusLabel.setText("Stopped");
	}

	private void onChunk(String chunk) {
		if (!isStreaming || currentAssistantBubble == null || currentAssistantBubble.isDisposed()) {
			return;
		}

		currentResponse.append(chunk);
		Display.getDefault().asyncExec(() -> {
			if (!currentAssistantBubble.isDisposed()) {
				currentAssistantBubble.append(chunk);
				messagesContainer.layout(true, true);
				scrolledComposite.setMinSize(messagesContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				scrollToBottom();
			}
		});
	}

	private void onComplete(String fullResponse) {
		isStreaming = false;
		conversation.add(ChatMessage.assistant(currentResponse.toString()));
		historyStore.save(conversation);

		Display.getDefault().asyncExec(() -> {
			sendButton.setEnabled(true);
			stopButton.setEnabled(false);
			statusLabel.setText("Ready");

			// Apply final styling and add copy button if needed
			if (currentAssistantBubble != null && !currentAssistantBubble.isDisposed()) {
				applyMarkdownStyles(currentAssistantBubble, currentResponse.toString());

				// Add copy button if has code
				if (markdownRenderer.hasCodeBlocks(currentResponse.toString())) {
					Composite parent = currentAssistantBubble.getParent();
					addCopyButton(parent, currentResponse.toString(), styles.getAssistantBubbleBackground());
					parent.layout(true, true);
				}
			}

			// Show diff dialog for fix actions
			if ("fix".equals(lastAction) && lastContext != null && !lastContext.isEmpty()) {
				// Show the diff between original and fixed code
				CodeDiffDialog.show("Code Fix Suggestion", lastContext, currentResponse.toString());
			}

			messagesContainer.layout(true, true);
			scrolledComposite.setMinSize(messagesContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		});
	}

	private void onError(String error) {
		isStreaming = false;

		Display.getDefault().asyncExec(() -> {
			if (currentAssistantBubble != null && !currentAssistantBubble.isDisposed()) {
				currentAssistantBubble.append("\n❌ Error: " + error);
				currentAssistantBubble.setForeground(styles.getErrorColor());
			}
			sendButton.setEnabled(true);
			stopButton.setEnabled(false);
			statusLabel.setText("Error occurred");
		});
	}

	private void clearConversation() {
		// Remove all message bubbles
		for (org.eclipse.swt.widgets.Control child : messagesContainer.getChildren()) {
			child.dispose();
		}

		conversation.clear();
		addSystemMessage();
		historyStore.save(conversation);
		addWelcomeMessage();

		messagesContainer.layout(true, true);
		scrolledComposite.setMinSize(messagesContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		statusLabel.setText("Conversation cleared");
	}

	@Override
	public void setFocus() {
		inputField.setFocus();
	}

	@Override
	public void dispose() {
		// Note: ChatStyles is a singleton, don't dispose here
		super.dispose();
	}

	/**
	 * Add context (selected code) to the next message. Now includes structural
	 * context via CodeContextCollector.
	 */
	public void setContext(String context, String action) {
		// Store last action and context for diff and context-aware prompts
		this.lastAction = action;
		this.lastContext = context;

		// Build context-aware prompt
		String contextPrompt = buildContextAwarePrompt(context, action);
		inputField.setText(contextPrompt);
		inputField.setFocus();

		// Auto-send after a small delay to ensure UI is ready
		Display.getDefault().asyncExec(() -> {
			if (!inputField.isDisposed()) {
				sendMessage();
			}
		});
	}

	/**
	 * Builds a context-aware prompt using the CodeContextCollector.
	 *
	 * @param selectedCode the code selected by the user
	 * @param action       the action (explain, fix, etc.)
	 * @return the prompt to send to the LLM
	 */
	private String buildContextAwarePrompt(String selectedCode, String action) {
		if (selectedCode == null || selectedCode.isBlank()) {
			return selectedCode;
		}

		// Extract structural context
		CodeContextCollector.Context ctx = CodeContextCollector.collect(selectedCode);
		StringBuilder prompt = new StringBuilder();

		String prefix = ctx.toPromptPrefix();
		if (!prefix.isBlank()) {
			prompt.append(prefix).append("\n---\n");
		}

		// Action-specific instruction
		String instruction = switch (action) {
		case "explain" -> "Explain this code:";
		case "fix" -> "Find and fix any issues in this code:";
		case "test" -> "Generate unit tests for this code:";
		case "document" -> "Generate Javadoc documentation for this code:";
		case "refactor" -> "Refactor this code for clarity and maintainability:";
		case "review" -> "Review this code and list issues, risks, and suggestions:";
		case "convert" -> "Convert this code to a different language/format:";
		default -> selectedCode; // fallback
		};

		prompt.append(instruction).append("\n```\n").append(selectedCode).append("\n```");

		return prompt.toString();
	}
}