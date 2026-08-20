package com.eclipsellama.plugin.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * Shows a code change as an inline diff using Eclipse's compare framework. Left
 * pane is the original code, right pane is the LLM's suggested fix.
 */
public final class CodeDiffDialog {

	private CodeDiffDialog() {
	}

	/**
	 * Opens a modal compare dialog for the original vs. fixed code. Safe to call
	 * from any thread (automatically dispatches to the SWT UI thread).
	 *
	 * @param title    the dialog title.
	 * @param original the original code.
	 * @param fixed    the suggested replacement code.
	 */
	public static void show(String title, String original, String fixed) {
		Display display = Display.getDefault();
		if (display == null || display.isDisposed()) {
			return;
		}

		Runnable openTask = () -> {
			CompareConfiguration config = new CompareConfiguration();
			config.setLeftLabel("Original");
			config.setRightLabel("Suggested Fix");

			DiffNode diffNode = new DiffNode(null, Differencer.CHANGE, null,
					new StringElement("Original", original == null ? "" : original),
					new StringElement("Suggested Fix", fixed == null ? "" : fixed));

			CompareEditorInput input = new CompareEditorInput(config) {
				@Override
				protected Object prepareInput(IProgressMonitor monitor) {
					return diffNode;
				}
			};
			if (title != null && !title.isEmpty()) {
				input.setTitle(title);
			}

			CompareUI.openCompareDialog(input);
		};

		if (Display.getCurrent() != null) {
			openTask.run();
		} else {
			display.asyncExec(openTask);
		}
	}

	/**
	 * Computes a lightweight line diff between the two texts for unit-testing.
	 *
	 * @return a list of lines prefixed with ' ' (unchanged), '-' (removed) or '+'
	 *         (added).
	 */
	public static List<String> computeSimpleDiff(String original, String fixed) {
		List<String> result = new ArrayList<>();
		String[] oldLines = original == null ? new String[0] : original.split("\\R", -1);
		String[] newLines = fixed == null ? new String[0] : fixed.split("\\R", -1);
		int max = Math.max(oldLines.length, newLines.length);
		for (int i = 0; i < max; i++) {
			String o = i < oldLines.length ? oldLines[i] : "";
			String n = i < newLines.length ? newLines[i] : "";
			if (o.equals(n)) {
				result.add(" " + o);
			} else {
				if (!o.isEmpty()) {
					result.add("-" + o);
				}
				if (!n.isEmpty()) {
					result.add("+" + n);
				}
			}
		}
		return result;
	}

	/** ITypedElement + IStreamContentAccessor for a raw text chunk. */
	private static final class StringElement
			implements ITypedElement, IStreamContentAccessor, IEncodedStreamContentAccessor {
		private final String name;
		private final String content;

		StringElement(String name, String content) {
			this.name = name;
			this.content = content == null ? "" : content;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Image getImage() {
			return null;
		}

		@Override
		public String getType() {
			return "java";
		}

		@Override
		public InputStream getContents() {
			return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
		}

		@Override
		public String getCharset() {
			return StandardCharsets.UTF_8.name();
		}
	}
}
