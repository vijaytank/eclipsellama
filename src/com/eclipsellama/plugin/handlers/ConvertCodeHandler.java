package com.eclipsellama.plugin.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.eclipsellama.plugin.ui.chat.ChatView;

/**
 * Handler for the "Convert Code" action. Sends selected code to the chat with a
 * language-conversion instruction.
 */
public class ConvertCodeHandler extends ExplainCodeHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String selectedText = getSelectedText(event);
		if (selectedText == null || selectedText.isEmpty()) {
			return null;
		}

		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			ChatView chatView = (ChatView) page.showView("com.eclipsellama.plugin.view.chat");
			chatView.setContext(selectedText, "convert");
		} catch (PartInitException e) {
			throw new ExecutionException("Failed to open chat view", e);
		}

		return null;
	}
}
