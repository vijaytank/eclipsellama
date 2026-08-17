package com.eclipsellama.plugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Handler to open the EclipseLlama chat view.
 */
public class OpenChatHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			if (page != null) {
				page.showView("com.eclipsellama.plugin.view.chat");
			}
		} catch (PartInitException e) {
			throw new ExecutionException("Failed to open EclipseLlama chat", e);
		}
		return null;
	}
}
