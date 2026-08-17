package com.eclipsellama.plugin.context;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Provides context from the current editor for AI prompts. Includes: current
 * file path, selected text, surrounding code.
 */
public class SimpleContextProvider {

	/**
	 * Get context about the current file and selection.
	 */
	public static Context getCurrentContext() {
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window == null) {
				return Context.empty();
			}

			IWorkbenchPage page = window.getActivePage();
			if (page == null) {
				return Context.empty();
			}

			IEditorPart editor = page.getActiveEditor();
			if (editor == null) {
				return Context.empty();
			}

			// Get file info
			String filePath = "";
			String fileName = "";
			IEditorInput input = editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				filePath = file.getFullPath().toString();
				fileName = file.getName();
			}

			// Get selected text and document content
			String selectedText = "";
			String documentContent = "";

			if (editor instanceof ITextEditor) {
				ITextEditor textEditor = (ITextEditor) editor;
				IDocument doc = textEditor.getDocumentProvider().getDocument(input);

				if (doc != null) {
					documentContent = doc.get();

					ISelection selection = textEditor.getSelectionProvider().getSelection();
					if (selection instanceof ITextSelection) {
						selectedText = ((ITextSelection) selection).getText();
					}
				}
			}

			return new Context(fileName, filePath, selectedText, documentContent);

		} catch (Exception e) {
			System.err.println("EclipseLlama: Failed to get context: " + e.getMessage());
			return Context.empty();
		}
	}

	/**
	 * Build a context string for including in prompts.
	 */
	public static String buildContextPrompt() {
		Context ctx = getCurrentContext();
		if (ctx.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("\n\n--- Current Context ---\n");
		sb.append("File: ").append(ctx.filePath).append("\n");

		if (!ctx.selectedText.isEmpty()) {
			sb.append("Selected code:\n```\n").append(ctx.selectedText).append("\n```\n");
		}

		return sb.toString();
	}

	/**
	 * Container for context information.
	 */
	public static class Context {
		public final String fileName;
		public final String filePath;
		public final String selectedText;
		public final String documentContent;

		public Context(String fileName, String filePath, String selectedText, String documentContent) {
			this.fileName = fileName;
			this.filePath = filePath;
			this.selectedText = selectedText;
			this.documentContent = documentContent;
		}

		public static Context empty() {
			return new Context("", "", "", "");
		}

		public boolean isEmpty() {
			return filePath.isEmpty() && selectedText.isEmpty();
		}

		/**
		 * Get a truncated version of the document (for context window limits).
		 */
		public String getTruncatedDocument(int maxLength) {
			if (documentContent.length() <= maxLength) {
				return documentContent;
			}
			// Try to find a good break point
			int cutPoint = documentContent.lastIndexOf('\n', maxLength);
			if (cutPoint < maxLength / 2) {
				cutPoint = maxLength;
			}
			return documentContent.substring(0, cutPoint) + "\n... (file truncated)";
		}
	}
}
