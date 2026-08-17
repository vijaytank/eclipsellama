package com.eclipsellama.plugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for the "Open MCP Server" command. This handler opens the MCP
 * preference page where users can configure STDIO or SSE MCP servers.
 */
public class OpenMcpHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		if (window == null) {
			return null;
		}

		try {
			// Open the preference page for MCP servers
			// The preference page ID is defined in plugin.xml as:
			// "com.eclipsellama.plugin.preferences.mcp"
			Shell shell = window.getShell();
			PreferencesUtil.createPreferenceDialogOn(shell, "com.eclipsellama.plugin.preferences.mcp", null, null)
					.open();
		} catch (Exception e) {
			// Fallback: show a message if programmatic opening fails
			System.out.println("Failed to open MCP preference page programmatically: " + e.getMessage());
			throw new ExecutionException("Failed to open MCP preference page: " + e.getMessage(), e);
		}
		return null;
	}
}