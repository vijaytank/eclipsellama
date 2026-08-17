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
 * Preferences page for EclipseLlama settings.
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

	@Override
	public void init(IWorkbench workbench) {
		setDescription("Configure EclipseLlama AI coding assistant");
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		createConnectionGroup(container);
		createRetryGroup(container);
		createModelGroup(container);
		createPromptsGroup(container);

		loadPreferences();
		return container;
	}

	private void createConnectionGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("LLM Connection");
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		Label endpointLabel = new Label(group, SWT.NONE);
		endpointLabel.setText("Endpoint URL:");

		endpointText = new Text(group, SWT.BORDER);
		endpointText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Label apiKeyLabel = new Label(group, SWT.NONE);
		apiKeyLabel.setText("Api key:");

		apiKeyText = new Text(group, SWT.BORDER);
		apiKeyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Button testBtn = new Button(group, SWT.PUSH);
		testBtn.setText("Test");
		testBtn.addListener(SWT.Selection, e -> testConnection());

		new Label(group, SWT.NONE); // Spacer
		statusLabel = new Label(group, SWT.NONE);
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		new Label(group, SWT.NONE);
		new Label(group, SWT.NONE);
	}

	private void createRetryGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Timeout & Retry");
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
		refreshBtn.setText("Refresh");
		refreshBtn.addListener(SWT.Selection, e -> refreshModels());

		Label infoLabel = new Label(group, SWT.NONE);
		infoLabel.setText("Recommended: codellama, deepseek-coder, qwen2.5-coder");
		infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
	}

	private void createPromptsGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Commit Message Prompt");
		group.setLayout(new GridLayout(1, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label promptLabel = new Label(group, SWT.NONE);
		promptLabel.setText("Template for generating commit messages:");

		commitPromptText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData promptData = new GridData(SWT.FILL, SWT.FILL, true, true);
		promptData.heightHint = 100;
		commitPromptText.setLayoutData(promptData);
	}

	private void loadPreferences() {
		endpointText.setText(EclipseLlamaPreferences.getEndpoint());
		apiKeyText.setText(EclipseLlamaPreferences.getApiKey());
		timeoutText.setText(String.valueOf(EclipseLlamaPreferences.getTimeoutSeconds()));
		retryText.setText(String.valueOf(EclipseLlamaPreferences.getRetryAttempts()));
		commitPromptText.setText(EclipseLlamaPreferences.getCommitPrompt());

		// Load models
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
		statusLabel.setText("Testing...");

		new Thread(() -> {
			boolean ok = ClientProvider.getClient().isServerReachable();
			Display.getDefault().asyncExec(() -> {
				if (ok) {
					statusLabel.setText("✅ Connected successfully");
					refreshModels();
				} else {
					statusLabel.setText("❌ Cannot connect to endpoint");
				}
			});
		}).start();
	}

	private void refreshModels() {
		savePreferences();
		new Thread(() -> {
			String[] models = ClientProvider.getClient().getAvailableModels();
			Display.getDefault().asyncExec(() -> {
				if (models.length > 0) {
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
		EclipseLlamaPreferences.save();
	}

	private int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (Exception e) {
			return fallback;
		}
	}

	@Override
	protected void performDefaults() {
		endpointText.setText("http://localhost:11434");
		modelCombo.setText("codellama");
		timeoutText.setText("60");
		retryText.setText("3");
		commitPromptText.setText("Generate a concise Conventional Commit message for the following Git diff. "
				+ "Format: type(scope): description\\n\\n[optional body]");
		super.performDefaults();
	}
}
