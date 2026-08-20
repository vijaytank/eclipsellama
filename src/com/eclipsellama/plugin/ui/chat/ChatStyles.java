package com.eclipsellama.plugin.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Centralized, theme-adaptive styling constants for EclipseLlama. Automatically
 * detects Eclipse Dark vs. Light theme and provides a modern, high-contrast,
 * beautiful color palette and typography.
 */
public class ChatStyles {

	private static ChatStyles instance;

	// Theme mode
	private final boolean isDarkTheme;

	// Message bubble colors
	private Color userBubbleBackground;
	private Color userBubbleForeground;
	private Color assistantBubbleBackground;
	private Color assistantBubbleForeground;
	private Color bubbleBorderColor;

	// Code block colors (Modern Dark Editor Theme)
	private Color codeBackground;
	private Color codeForeground;
	private Color codeBorder;
	private Color codeHeaderBackground;

	// General & Brand Colors
	private Color headerColor;
	private Color timestampColor;
	private Color errorColor;
	private Color successColor;
	private Color linkColor;
	private Color chipBackground;
	private Color chipHoverBackground;
	private Color chipForeground;
	private Color containerBackground;

	// Fonts
	private Font codeFont;
	private Font headerFont;
	private Font subHeaderFont;
	private Font boldFont;
	private Font normalFont;
	private Font smallFont;

	private final Display display;

	private ChatStyles(Display display) {
		this.display = display;
		this.isDarkTheme = detectDarkTheme(display);
		initializeColors();
		initializeFonts();
	}

	public static synchronized ChatStyles getInstance(Display display) {
		if (instance == null) {
			instance = new ChatStyles(display);
		}
		return instance;
	}

	public static synchronized void disposeInstance() {
		if (instance != null) {
			instance.dispose();
			instance = null;
		}
	}

	/**
	 * Detects whether the current Eclipse workspace is running a dark theme by
	 * calculating the luminance of the widget background color.
	 */
	private boolean detectDarkTheme(Display display) {
		try {
			Color bg = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
			if (bg != null) {
				double luminance = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255.0;
				return luminance < 0.5;
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	public boolean isDarkTheme() {
		return isDarkTheme;
	}

	private void initializeColors() {
		if (isDarkTheme) {
			// --- Dark Theme Palette (JetBrains / VS Code inspired) ---
			userBubbleBackground = new Color(display, new RGB(37, 50, 72)); // #253248 Slate Blue
			userBubbleForeground = new Color(display, new RGB(241, 245, 249)); // Crisp White
			assistantBubbleBackground = new Color(display, new RGB(30, 34, 42)); // #1E222A Deep Charcoal
			assistantBubbleForeground = new Color(display, new RGB(226, 232, 240)); // Soft White
			bubbleBorderColor = new Color(display, new RGB(48, 54, 66));

			codeBackground = new Color(display, new RGB(21, 24, 30)); // #15181E Deep Dark
			codeForeground = new Color(display, new RGB(230, 237, 243));
			codeBorder = new Color(display, new RGB(48, 54, 61));
			codeHeaderBackground = new Color(display, new RGB(28, 32, 40));

			headerColor = new Color(display, new RGB(168, 85, 247)); // #A855F7 Modern Purple
			timestampColor = new Color(display, new RGB(148, 163, 184)); // Slate Gray
			errorColor = new Color(display, new RGB(248, 113, 113)); // Coral Red
			successColor = new Color(display, new RGB(52, 211, 153)); // Emerald Green
			linkColor = new Color(display, new RGB(56, 189, 248)); // Sky Blue

			chipBackground = new Color(display, new RGB(40, 46, 58));
			chipHoverBackground = new Color(display, new RGB(55, 65, 81));
			chipForeground = new Color(display, new RGB(226, 232, 240));
			containerBackground = new Color(display, new RGB(24, 26, 32));
		} else {
			// --- Light Theme Palette (Clean Modern Card UI) ---
			userBubbleBackground = new Color(display, new RGB(235, 245, 255)); // Soft Sky Blue
			userBubbleForeground = new Color(display, new RGB(15, 23, 42)); // Deep Slate
			assistantBubbleBackground = new Color(display, new RGB(248, 250, 252)); // Crisp Light Gray
			assistantBubbleForeground = new Color(display, new RGB(30, 41, 59));
			bubbleBorderColor = new Color(display, new RGB(226, 232, 240));

			codeBackground = new Color(display, new RGB(30, 34, 42)); // Dark code block in light mode
			codeForeground = new Color(display, new RGB(241, 245, 249));
			codeBorder = new Color(display, new RGB(203, 213, 225));
			codeHeaderBackground = new Color(display, new RGB(40, 44, 52));

			headerColor = new Color(display, new RGB(126, 34, 206)); // Deep Purple
			timestampColor = new Color(display, new RGB(100, 116, 139));
			errorColor = new Color(display, new RGB(220, 38, 38));
			successColor = new Color(display, new RGB(22, 163, 74));
			linkColor = new Color(display, new RGB(2, 132, 199));

			chipBackground = new Color(display, new RGB(241, 245, 249));
			chipHoverBackground = new Color(display, new RGB(226, 232, 240));
			chipForeground = new Color(display, new RGB(30, 41, 59));
			containerBackground = new Color(display, new RGB(255, 255, 255));
		}
	}

	private void initializeFonts() {
		FontData[] sysFont = display.getSystemFont().getFontData();
		String fontName = sysFont.length > 0 ? sysFont[0].getName() : "Segoe UI";
		int baseSize = sysFont.length > 0 ? sysFont[0].getHeight() : 9;

		headerFont = new Font(display, fontName, baseSize + 2, SWT.BOLD);
		subHeaderFont = new Font(display, fontName, baseSize + 1, SWT.BOLD);
		boldFont = new Font(display, fontName, baseSize, SWT.BOLD);
		normalFont = new Font(display, fontName, baseSize, SWT.NORMAL);
		smallFont = new Font(display, fontName, Math.max(7, baseSize - 1), SWT.NORMAL);

		// Modern coding monospace font selection with graceful fallbacks
		codeFont = new Font(display, new FontData[] { new FontData("JetBrains Mono", baseSize, SWT.NORMAL),
				new FontData("Cascadia Code", baseSize, SWT.NORMAL), new FontData("Fira Code", baseSize, SWT.NORMAL),
				new FontData("Consolas", baseSize, SWT.NORMAL), new FontData("Courier New", baseSize, SWT.NORMAL) });
	}

	// ===== Getters =====

	public Color getUserBubbleBackground() {
		return userBubbleBackground;
	}

	public Color getUserBubbleForeground() {
		return userBubbleForeground;
	}

	public Color getAssistantBubbleBackground() {
		return assistantBubbleBackground;
	}

	public Color getAssistantBubbleForeground() {
		return assistantBubbleForeground;
	}

	public Color getBubbleBorderColor() {
		return bubbleBorderColor;
	}

	public Color getCodeBackground() {
		return codeBackground;
	}

	public Color getCodeForeground() {
		return codeForeground;
	}

	public Color getCodeBorder() {
		return codeBorder;
	}

	public Color getCodeHeaderBackground() {
		return codeHeaderBackground;
	}

	public Color getHeaderColor() {
		return headerColor;
	}

	public Color getTimestampColor() {
		return timestampColor;
	}

	public Color getErrorColor() {
		return errorColor;
	}

	public Color getSuccessColor() {
		return successColor;
	}

	public Color getLinkColor() {
		return linkColor;
	}

	public Color getChipBackground() {
		return chipBackground;
	}

	public Color getChipHoverBackground() {
		return chipHoverBackground;
	}

	public Color getChipForeground() {
		return chipForeground;
	}

	public Color getContainerBackground() {
		return containerBackground;
	}

	public Font getCodeFont() {
		return codeFont;
	}

	public Font getHeaderFont() {
		return headerFont;
	}

	public Font getSubHeaderFont() {
		return subHeaderFont;
	}

	public Font getBoldFont() {
		return boldFont;
	}

	public Font getNormalFont() {
		return normalFont;
	}

	public Font getSmallFont() {
		return smallFont;
	}

	/**
	 * Dispose all allocated SWT resources on workbench/bundle shutdown.
	 */
	public void dispose() {
		disposeColor(userBubbleBackground);
		disposeColor(userBubbleForeground);
		disposeColor(assistantBubbleBackground);
		disposeColor(assistantBubbleForeground);
		disposeColor(bubbleBorderColor);
		disposeColor(codeBackground);
		disposeColor(codeForeground);
		disposeColor(codeBorder);
		disposeColor(codeHeaderBackground);
		disposeColor(headerColor);
		disposeColor(timestampColor);
		disposeColor(errorColor);
		disposeColor(successColor);
		disposeColor(linkColor);
		disposeColor(chipBackground);
		disposeColor(chipHoverBackground);
		disposeColor(chipForeground);
		disposeColor(containerBackground);

		disposeFont(codeFont);
		disposeFont(headerFont);
		disposeFont(subHeaderFont);
		disposeFont(boldFont);
		disposeFont(normalFont);
		disposeFont(smallFont);

		instance = null;
	}

	private void disposeColor(Color c) {
		if (c != null && !c.isDisposed()) {
			c.dispose();
		}
	}

	private void disposeFont(Font f) {
		if (f != null && !f.isDisposed()) {
			f.dispose();
		}
	}
}
