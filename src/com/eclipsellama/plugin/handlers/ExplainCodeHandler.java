package com.eclipsellama.plugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eclipsellama.plugin.ui.chat.ChatView;

/**
 * Handler for "Explain Code" action.
 */
public class ExplainCodeHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String selectedText = getSelectedText(event);
		if (selectedText == null || selectedText.isEmpty()) {
			return null;
		}

		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			ChatView chatView = (ChatView) page.showView("com.eclipsellama.plugin.view.chat");
			chatView.setContext(selectedText, "explain");
		} catch (PartInitException e) {
			throw new ExecutionException("Failed to open chat view", e);
		}

		return null;
	}

	protected String getSelectedText(ExecutionEvent event) {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (editor instanceof ITextEditor textEditor) {
			ISelection selection = textEditor.getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection textSelection) {
				return textSelection.getText();
			}
		}
		return null;
	}
}
