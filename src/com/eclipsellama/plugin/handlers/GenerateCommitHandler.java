package com.eclipsellama.plugin.handlers;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.eclipsellama.plugin.git.CommitMessageGenerator;

/**
 * Handler for generating AI-powered commit messages from git diff.
 */
public class GenerateCommitHandler extends AbstractHandler {

	// Debug dialog - set to true for development only
	private static final boolean SHOW_DEBUG_DIALOG = false;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event);
		IProject project = getSelectedProject(event);

		if (project == null) {
			MessageDialog.openWarning(shell, "EclipseLlama", "Please select a project in the Package Explorer.");
			return null;
		}

		String projectPath = project.getLocation().toOSString();

		// Show initial progress
		MessageDialog.openInformation(shell, "EclipseLlama",
				"🦙 Analyzing git changes...\nProject: " + project.getName());

		// Run in background thread
		new Thread(() -> {
			try {
				// Get git diff
				String diff = getGitDiff(projectPath);

				if (diff == null || diff.trim().isEmpty()) {
					Display.getDefault().asyncExec(() -> {
						MessageDialog.openInformation(shell, "EclipseLlama",
								"No uncommitted changes found.\n\n" + "Project: " + projectPath);
					});
					return;
				}

				int diffLines = diff.split("\n").length;

				// Generate commit message
				String commitMessage = CommitMessageGenerator.generate(diff);

				Display.getDefault().asyncExec(() -> {
					// TODO: Remove debug dialog before publishing!
					if (SHOW_DEBUG_DIALOG) {
						String debugInfo = CommitMessageGenerator.getDebugInfo();
						if (debugInfo != null) {
							MessageDialog.openInformation(shell, "🔧 DEBUG INFO",
									"Diff lines: " + diffLines + "\n\n" + debugInfo);
						}
					}

					if (commitMessage == null || commitMessage.isEmpty() || commitMessage.startsWith("Error:")) {
						MessageDialog.openError(shell, "EclipseLlama Error",
								"Failed to generate commit message.\n\n" + "Please check Ollama is running.");
						return;
					}

					// Show result with copy option
					boolean copy = MessageDialog.openQuestion(shell, "🦙 Generated Commit Message",
							commitMessage + "\n\n─────────────────────────────\n" + "Copy to clipboard?");

					if (copy) {
						copyToClipboard(commitMessage);
						MessageDialog.openInformation(shell, "EclipseLlama", "✅ Copied to clipboard!");
					}
				});

			} catch (Exception e) {
				e.printStackTrace();
				Display.getDefault().asyncExec(() -> {
					MessageDialog.openError(shell, "EclipseLlama Error", "Failed: " + e.getMessage());
				});
			}
		}, "EclipseLlama-CommitGen").start();

		return null;
	}

	/**
	 * Get the selected project from Package Explorer.
	 */
	private IProject getSelectedProject(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);

		if (selection instanceof IStructuredSelection structuredSelection) {
			Object element = structuredSelection.getFirstElement();

			if (element instanceof IProject project) {
				return project;
			}

			if (element instanceof IResource resource) {
				return resource.getProject();
			}

			if (element instanceof IAdaptable adaptable) {
				IResource resource = adaptable.getAdapter(IResource.class);
				if (resource != null) {
					return resource.getProject();
				}
			}
		}

		return null;
	}

	/**
	 * Get git diff for the project directory.
	 */
	private String getGitDiff(String projectPath) {
		try {
			// Try staged changes first
			ProcessBuilder pb = new ProcessBuilder("git", "diff", "--cached");
			pb.directory(new java.io.File(projectPath));
			pb.redirectErrorStream(true);

			Process process = pb.start();
			StringBuilder output = new StringBuilder();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
				}
			}

			process.waitFor();

			// If no staged changes, try unstaged
			if (output.toString().trim().isEmpty()) {
				pb = new ProcessBuilder("git", "diff");
				pb.directory(new java.io.File(projectPath));
				pb.redirectErrorStream(true);

				process = pb.start();
				output = new StringBuilder();

				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						output.append(line).append("\n");
					}
				}

				process.waitFor();
			}

			return output.toString();

		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Copy text to system clipboard.
	 */
	private void copyToClipboard(String text) {
		Clipboard clipboard = new Clipboard(Display.getDefault());
		try {
			TextTransfer textTransfer = TextTransfer.getInstance();
			clipboard.setContents(new Object[] { text }, new Transfer[] { textTransfer });
		} finally {
			clipboard.dispose();
		}
	}
}
