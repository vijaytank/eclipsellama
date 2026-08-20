package com.eclipsellama.plugin.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.eclipsellama.plugin.core.ClientProvider;

/**
 * Preferences page for EclipseLlama settings with modern layout, live latency
 * test feedback, and organized configuration sections.
 */
public class EclipseLlamaPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public EclipseLlamaPreferencePage() {
	}

	private Text endpointText;
	private Text apiKeyText;
	private Combo modelCombo;
	private Text commitPromptText;
	private Text timeoutText;
	private Text retryText;
	private Label statusLabel;
	private Combo searchProviderCombo;
	private Combo searchModeCombo;
	private Text searchMaxResultsText;
	private Text searchApiKeyText;
	private Text searchSearxngEndpointText;
	private Button searchFallbackButton;

	@Override
	public void init(IWorkbench workbench) {
		setDescription("Configure EclipseLlama AI Coding Assistant settings, providers, and search.");
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.verticalSpacing = 12;
		container.setLayout(layout);

		createConnectionGroup(container);
		createRetryGroup(container);
		createModelGroup(container);
		createSearchGroup(container);
		createPromptsGroup(container);

		loadPreferences();
		return container;
	}

	private void createConnectionGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("LLM Connection & Credentials");
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		Label endpointLabel = new Label(group, SWT.NONE);
		endpointLabel.setText("Endpoint URL:");

		endpointText = new Text(group, SWT.BORDER);
		endpointText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		endpointText.setMessage("e.g. http://localhost:11434 or https://api.openai.com/v1");

		Label apiKeyLabel = new Label(group, SWT.NONE);
		apiKeyLabel.setText("API Key (OpenAI):");

		apiKeyText = new Text(group, SWT.BORDER | SWT.PASSWORD);
		apiKeyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		apiKeyText.setMessage("Optional for local Ollama; required for OpenAI");

		Button testBtn = new Button(group, SWT.PUSH);
		testBtn.setText("⚡ Test Connection");
		testBtn.addListener(SWT.Selection, e -> testConnection());

		statusLabel = new Label(group, SWT.NONE);
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		statusLabel.setText("Click 'Test Connection' to verify reachability.");
	}

	private void createRetryGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Timeout & Retry Settings");
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		new Label(group, SWT.NONE).setText("Timeout (seconds, 10-300):");
		timeoutText = new Text(group, SWT.BORDER);
		timeoutText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		new Label(group, SWT.NONE).setText("Retry attempts (1-5):");
		retryText = new Text(group, SWT.BORDER);
		retryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	private void createModelGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Model Selection");
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		Label modelLabel = new Label(group, SWT.NONE);
		modelLabel.setText("Default Model:");

		modelCombo = new Combo(group, SWT.DROP_DOWN);
		modelCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Button refreshBtn = new Button(group, SWT.PUSH);
		refreshBtn.setText("🔄 Refresh");
		refreshBtn.addListener(SWT.Selection, e -> refreshModels());

		Label infoLabel = new Label(group, SWT.NONE);
		infoLabel.setText("Recommended models: codellama, deepseek-coder, qwen2.5-coder, llama3.2");
		infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
	}

	private void createSearchGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Web Search & RAG Context");
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		new Label(group, SWT.NONE).setText("Provider:");
		searchProviderCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
		searchProviderCombo.setItems(new String[] { "builtin", "searxng", "brave", "disabled" });
		searchProviderCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		new Label(group, SWT.NONE).setText("Search Mode:");
		searchModeCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
		searchModeCombo.setItems(new String[] { "off", "smart", "ask", "always" });
		searchModeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		new Label(group, SWT.NONE).setText("Max results (1-10):");
		searchMaxResultsText = new Text(group, SWT.BORDER);
		searchMaxResultsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		new Label(group, SWT.NONE).setText("SearXNG instance URL:");
		searchSearxngEndpointText = new Text(group, SWT.BORDER);
		searchSearxngEndpointText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		searchSearxngEndpointText.setMessage("e.g. http://localhost:8080/search");

		new Label(group, SWT.NONE).setText("Brave Search API Key:");
		searchApiKeyText = new Text(group, SWT.BORDER | SWT.PASSWORD);
		searchApiKeyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		new Label(group, SWT.NONE).setText("Fallback:");
		searchFallbackButton = new Button(group, SWT.CHECK);
		searchFallbackButton.setText("Use built-in search if selected provider is unavailable");
		searchFallbackButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	private void createPromptsGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Commit Message Prompt Template");
		group.setLayout(new GridLayout(1, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label promptLabel = new Label(group, SWT.NONE);
		promptLabel.setText("Template for generating Git commit messages:");

		commitPromptText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData promptData = new GridData(SWT.FILL, SWT.FILL, true, true);
		promptData.heightHint = 80;
		commitPromptText.setLayoutData(promptData);
	}

	private void loadPreferences() {
		endpointText.setText(EclipseLlamaPreferences.getEndpoint());
		apiKeyText.setText(EclipseLlamaPreferences.getApiKey());
		timeoutText.setText(String.valueOf(EclipseLlamaPreferences.getTimeoutSeconds()));
		retryText.setText(String.valueOf(EclipseLlamaPreferences.getRetryAttempts()));
		searchProviderCombo
				.select(indexOf(searchProviderCombo.getItems(), EclipseLlamaPreferences.getSearchProvider()));
		searchModeCombo.select(indexOf(searchModeCombo.getItems(), EclipseLlamaPreferences.getSearchMode()));
		searchMaxResultsText.setText(String.valueOf(EclipseLlamaPreferences.getSearchMaxResults()));
		searchApiKeyText.setText(EclipseLlamaPreferences.getSearchApiKey());
		searchSearxngEndpointText.setText(EclipseLlamaPreferences.getSearchSearxngEndpoint());
		searchFallbackButton.setSelection(EclipseLlamaPreferences.getSearchFallbackToBuiltin());
		commitPromptText.setText(EclipseLlamaPreferences.getCommitPrompt());

		// Load recommended models
		for (String model : EclipseLlamaPreferences.getRecommendedCodeModels()) {
			modelCombo.add(model);
		}

		String currentModel = EclipseLlamaPreferences.getModel();
		int index = modelCombo.indexOf(currentModel);
		if (index >= 0) {
			modelCombo.select(index);
		} else {
			modelCombo.setText(currentModel);
		}
	}

	private void testConnection() {
		savePreferences();
		statusLabel.setText("🟡 Testing connection to " + endpointText.getText() + "...");

		long start = System.currentTimeMillis();
		new Thread(() -> {
			boolean ok = ClientProvider.getClient().isServerReachable();
			long duration = System.currentTimeMillis() - start;
			Display.getDefault().asyncExec(() -> {
				if (!statusLabel.isDisposed()) {
					if (ok) {
						statusLabel.setText("🟢 Connected successfully (" + duration + " ms latency)");
						refreshModels();
					} else {
						statusLabel.setText("🔴 Connection failed. Verify endpoint URL and network reachability.");
					}
				}
			});
		}).start();
	}

	private void refreshModels() {
		savePreferences();
		new Thread(() -> {
			String[] models = ClientProvider.getClient().getAvailableModels();
			Display.getDefault().asyncExec(() -> {
				if (models != null && models.length > 0 && !modelCombo.isDisposed()) {
					String current = modelCombo.getText();
					modelCombo.removeAll();
					for (String model : models) {
						modelCombo.add(model);
					}
					int idx = modelCombo.indexOf(current);
					if (idx >= 0) {
						modelCombo.select(idx);
					} else if (modelCombo.getItemCount() > 0) {
						modelCombo.select(0);
					}
				}
			});
		}).start();
	}

	@Override
	public boolean performOk() {
		savePreferences();
		return super.performOk();
	}

	@Override
	protected void performApply() {
		savePreferences();
		super.performApply();
	}

	private void savePreferences() {
		EclipseLlamaPreferences.setEndpoint(endpointText.getText());
		EclipseLlamaPreferences.setApiKey(apiKeyText.getText());
		EclipseLlamaPreferences.setModel(modelCombo.getText());
		EclipseLlamaPreferences.setCommitPrompt(commitPromptText.getText());
		EclipseLlamaPreferences.setTimeoutSeconds(parseInt(timeoutText.getText(), 60));
		EclipseLlamaPreferences.setRetryAttempts(parseInt(retryText.getText(), 3));
		EclipseLlamaPreferences.setSearchProvider(selectedItem(searchProviderCombo));
		EclipseLlamaPreferences.setSearchMode(selectedItem(searchModeCombo));
		EclipseLlamaPreferences.setSearchMaxResults(parseInt(searchMaxResultsText.getText(), 5));
		EclipseLlamaPreferences.setSearchApiKey(searchApiKeyText.getText());
		EclipseLlamaPreferences.setSearchSearxngEndpoint(searchSearxngEndpointText.getText());
		EclipseLlamaPreferences.setSearchFallbackToBuiltin(searchFallbackButton.getSelection());
		EclipseLlamaPreferences.save();
	}

	private int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (Exception e) {
			return fallback;
		}
	}

	private int indexOf(String[] items, String value) {
		for (int i = 0; i < items.length; i++) {
			if (items[i].equalsIgnoreCase(value)) {
				return i;
			}
		}
		return 0;
	}

	private String selectedItem(Combo combo) {
		int sel = combo.getSelectionIndex();
		return sel >= 0 ? combo.getItem(sel) : combo.getText();
	}

	@Override
	protected void performDefaults() {
		endpointText.setText("http://localhost:11434");
		modelCombo.setText("codellama");
		timeoutText.setText("60");
		retryText.setText("3");
		searchProviderCombo.select(0);
		searchModeCombo.select(0);
		searchMaxResultsText.setText("5");
		searchApiKeyText.setText("");
		searchSearxngEndpointText.setText("");
		searchFallbackButton.setSelection(true);
		commitPromptText.setText("Generate a concise Conventional Commit message for the following Git diff. "
				+ "Format: type(scope): description\n\n[optional body]");
		super.performDefaults();
	}
}
