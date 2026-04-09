package com.eclipsellama.plugin.setup;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;

import com.eclipsellama.plugin.core.ClientProvider;
import com.eclipsellama.plugin.core.OllamaClient;
import com.eclipsellama.plugin.preferences.EclipseLlamaPreferences;

/**
 * Runs on Eclipse startup to check Ollama connection and show setup if needed.
 */
public class SetupLauncher implements IStartup {

    @Override
    public void earlyStartup() {
        // Delay to let Eclipse finish loading
        Display.getDefault().asyncExec(() -> {
            Display.getDefault().timerExec(3000, this::checkSetup);
        });
    }

    private void checkSetup() {
        // Skip if already configured
        if (EclipseLlamaPreferences.isSetupComplete()) {
            // Just verify connection silently
            if (!ClientProvider.getClient().isServerReachable()) {
                System.out.println("EclipseLlama: Warning - endpoint not reachable at "
                        + EclipseLlamaPreferences.getEndpoint());
            }
            return;
        }

        // Show setup wizard for first-time users
        Display.getDefault().asyncExec(() -> {
            SetupWizardDialog wizard = new SetupWizardDialog();
            wizard.open();
        });
    }
}
