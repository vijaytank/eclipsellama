package com.eclipsellama.plugin.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * EclipseLlama perspective with chat view on the left.
 */
public class EclipseLlamaPerspective implements IPerspectiveFactory {

	public static final String ID = "com.eclipsellama.plugin.perspective";

	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();

		// Chat view on the left (25% width)
		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.25f, editorArea);
		left.addView("com.eclipsellama.plugin.view.chat");

		// Package Explorer and Outline on the right
		IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, 0.75f, editorArea);
		right.addView(IPageLayout.ID_OUTLINE);

		// Problems and Console at the bottom
		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.75f, editorArea);
		bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
		bottom.addPlaceholder("org.eclipse.ui.console.ConsoleView");

		// Add shortcuts
		layout.addShowViewShortcut("com.eclipsellama.plugin.view.chat");
		layout.addPerspectiveShortcut(ID);
	}
}
