package com.eclipsellama.plugin.setup;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.eclipsellama.plugin.core.ClientProvider;
import com.eclipsellama.plugin.core.OllamaClient;
import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;

/**
 * First-time setup wizard dialog.
 */
public class SetupWizardDialog extends TitleAreaDialog {

    private Text endpointText;
    private Text apiKeyText;
    private Combo modelCombo;
    private Label statusLabel;

    public SetupWizardDialog() {
        super(Display.getDefault().getActiveShell());
        setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("EclipseLlama Setup");
    }

    @Override
    public void create() {
        super.create();
        setTitle("Welcome to EclipseLlama 🦙");
        setMessage("Configure your local AI coding assistant");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout(2, false));

        // Endpoint
        Label endpointLabel = new Label(container, SWT.NONE);
        endpointLabel.setText("Endpoint URL:");

        endpointText = new Text(container, SWT.BORDER);
        endpointText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        endpointText.setText(EclipseLlamaPreferences.getEndpoint());

        // Api key
        Label apiKeyLabel = new Label(container, SWT.NONE);
        apiKeyLabel.setText("Api key:");

        apiKeyText = new Text(container, SWT.BORDER);
        apiKeyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        apiKeyText.setText(EclipseLlamaPreferences.getApiKey());

        // Test connection button
        new Label(container, SWT.NONE); // Spacer
        Button testBtn = new Button(container, SWT.PUSH);
        testBtn.setText("Test Connection");
        testBtn.addListener(SWT.Selection, e -> testConnection());

        // Model selection
        Label modelLabel = new Label(container, SWT.NONE);
        modelLabel.setText("Default Model:");

        modelCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        modelCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Add recommended models
        for (String model : EclipseLlamaPreferences.getRecommendedCodeModels()) {
            modelCombo.add(model);
        }
        modelCombo.select(0);

        // Status
        new Label(container, SWT.NONE); // Spacer
        statusLabel = new Label(container, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        statusLabel.setText("");

        // Info text
        Label infoLabel = new Label(container, SWT.WRAP);
        GridData infoData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        infoData.horizontalSpan = 2;
        infoData.widthHint = 400;
        infoLabel.setLayoutData(infoData);
        infoLabel.setText(
                "\n📋 Quick Start(Ollama):\n" +
                        "1. Install Ollama from ollama.com\n" +
                        "2. Run: ollama pull codellama\n" +
                        "3. Make sure Ollama is running\n" +
                        "4. Click 'Test Connection' above\n\n" +
                 "\n📋 Quick Start(Open AI compabitble):\n" +
                        "1. Make sure your Open AI compatible endpoint (f.e. llama-server) is running \n" +
                        "2. Make sure test endpoint Url ends with 'v1' \n" +
                        "3. Click 'Test Connection' above\n\n" +
                        "💡 Keyboard shortcut: Ctrl+Shift+L opens chat");

        // Try to load models
        testConnection();

        return area;
    }

    private void testConnection() {
        String endpoint = endpointText.getText().trim();
        EclipseLlamaPreferences.setEndpoint(endpoint);
        String apiKey = apiKeyText.getText().trim();
        EclipseLlamaPreferences.setApiKey(apiKey);

        statusLabel.setText("Testing connection...");

        new Thread(() -> {
            boolean reachable = ClientProvider.getClient().isServerReachable();
            String[] models = reachable ? ClientProvider.getClient().getAvailableModels() : new String[0];

            Display.getDefault().asyncExec(() -> {
                if (reachable) {
                    statusLabel.setText("✅ Connected! " + models.length + " models available");

                    // Update model combo with actual models
                    if (models.length > 0) {
                        modelCombo.removeAll();
                        for (String model : models) {
                            modelCombo.add(model);
                        }
                        // Select a code model if available
                        for (int i = 0; i < models.length; i++) {
                            if (models[i].contains("code") || models[i].contains("llama")) {
                                modelCombo.select(i);
                                break;
                            }
                        }
                        if (modelCombo.getSelectionIndex() < 0) {
                            modelCombo.select(0);
                        }
                    }
                } else {
                    statusLabel.setText("❌ Cannot connect. Is endpoint running?");
                }
            });
        }).start();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID, "Skip", false);
        createButton(parent, IDialogConstants.OK_ID, "Save & Start", true);
    }

    @Override
    protected void okPressed() {
        // Save settings
        EclipseLlamaPreferences.setEndpoint(endpointText.getText().trim());
        EclipseLlamaPreferences.setApiKey(apiKeyText.getText().trim());

        String selectedModel = modelCombo.getText();
        if (!selectedModel.isEmpty()) {
            EclipseLlamaPreferences.setModel(selectedModel);
        }

        EclipseLlamaPreferences.setSetupComplete(true);
        EclipseLlamaPreferences.save();

        super.okPressed();
    }
}
