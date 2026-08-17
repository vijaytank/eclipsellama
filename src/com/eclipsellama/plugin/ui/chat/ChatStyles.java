package com.eclipsellama.plugin.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

/**
 * Centralized styling constants for the Chat UI. Modern, visually appealing
 * color scheme.
 */
public class ChatStyles {

	// Singleton instance
	private static ChatStyles instance;

	// Message bubble colors
	private Color userBubbleBackground;
	private Color userBubbleForeground;
	private Color assistantBubbleBackground;
	private Color assistantBubbleForeground;

	// Code block colors (dark theme)
	private Color codeBackground;
	private Color codeForeground;
	private Color codeBorder;

	// General colors
	private Color headerColor;
	private Color timestampColor;
	private Color errorColor;
	private Color successColor;
	private Color linkColor;

	// Fonts
	private Font codeFont;
	private Font headerFont;
	private Font normalFont;

	private final Display display;

	private ChatStyles(Display display) {
		this.display = display;
		initializeColors();
		initializeFonts();
	}

	/**
	 * Get or create the singleton instance.
	 */
	public static synchronized ChatStyles getInstance(Display display) {
		if (instance == null) {
			instance = new ChatStyles(display);
		}
		return instance;
	}

	private void initializeColors() {
		// User bubble - Light blue theme
		userBubbleBackground = new Color(display, 220, 237, 255); // #DCEDFF
		userBubbleForeground = new Color(display, 30, 30, 30); // Dark text

		// Assistant bubble - Light green theme
		assistantBubbleBackground = new Color(display, 228, 248, 228); // #E4F8E4
		assistantBubbleForeground = new Color(display, 30, 30, 30); // Dark text

		// Code blocks - Dark theme inspired by One Dark
		codeBackground = new Color(display, 40, 44, 52); // #282C34
		codeForeground = new Color(display, 171, 178, 191); // #ABB2BF
		codeBorder = new Color(display, 62, 68, 81); // #3E4451

		// Headers - EclipseLlama brand purple
		headerColor = new Color(display, 138, 43, 226); // BlueViolet

		// Utility colors
		timestampColor = new Color(display, 128, 128, 128); // Gray
		errorColor = new Color(display, 220, 53, 69); // Bootstrap red
		successColor = new Color(display, 40, 167, 69); // Bootstrap green
		linkColor = new Color(display, 0, 123, 255); // Bootstrap blue
	}

	private void initializeFonts() {
		// Code font - monospace
		codeFont = new Font(display,
				new FontData[] { new FontData("Consolas", 10, SWT.NORMAL), new FontData("Courier New", 10, SWT.NORMAL) // Fallback
				});

		// Header font - bold
		FontData[] systemFont = display.getSystemFont().getFontData();
		headerFont = new Font(display, systemFont[0].getName(), systemFont[0].getHeight(), SWT.BOLD);

		// Normal font
		normalFont = new Font(display, systemFont[0].getName(), systemFont[0].getHeight(), SWT.NORMAL);
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

	public Color getCodeBackground() {
		return codeBackground;
	}

	public Color getCodeForeground() {
		return codeForeground;
	}

	public Color getCodeBorder() {
		return codeBorder;
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

	public Font getCodeFont() {
		return codeFont;
	}

	public Font getHeaderFont() {
		return headerFont;
	}

	public Font getNormalFont() {
		return normalFont;
	}

	/**
	 * Dispose all resources. Call when plugin shuts down.
	 */
	public void dispose() {
		// Dispose colors
		if (userBubbleBackground != null) {
			userBubbleBackground.dispose();
		}
		if (userBubbleForeground != null) {
			userBubbleForeground.dispose();
		}
		if (assistantBubbleBackground != null) {
			assistantBubbleBackground.dispose();
		}
		if (assistantBubbleForeground != null) {
			assistantBubbleForeground.dispose();
		}
		if (codeBackground != null) {
			codeBackground.dispose();
		}
		if (codeForeground != null) {
			codeForeground.dispose();
		}
		if (codeBorder != null) {
			codeBorder.dispose();
		}
		if (headerColor != null) {
			headerColor.dispose();
		}
		if (timestampColor != null) {
			timestampColor.dispose();
		}
		if (errorColor != null) {
			errorColor.dispose();
		}
		if (successColor != null) {
			successColor.dispose();
		}
		if (linkColor != null) {
			linkColor.dispose();
		}

		// Dispose fonts
		if (codeFont != null) {
			codeFont.dispose();
		}
		if (headerFont != null) {
			headerFont.dispose();
		}
		if (normalFont != null) {
			normalFont.dispose();
		}

		instance = null;
	}
}
