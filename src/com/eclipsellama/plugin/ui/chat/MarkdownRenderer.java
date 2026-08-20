package com.eclipsellama.plugin.ui.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

/**
 * Rich Markdown renderer for EclipseLlama. Parses code blocks, headers, bullet
 * lists, bold, italics, and inline code pills for SWT StyledText widgets.
 */
public class MarkdownRenderer {

	private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(\\w*)\\R?([\\s\\S]*?)```");
	private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");
	private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*([^*]+)\\*\\*");
	private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*([^*]+)\\*");
	private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,3})\\s+(.+)$", Pattern.MULTILINE);

	private final ChatStyles styles;

	public MarkdownRenderer(ChatStyles styles) {
		this.styles = styles;
	}

	public enum SegmentType {
		NORMAL, CODE_BLOCK, INLINE_CODE, BOLD, ITALIC, HEADER_1, HEADER_2, HEADER_3, LIST_ITEM
	}

	public static class StyledSegment {
		public final String text;
		public final SegmentType type;
		public final String language;

		public StyledSegment(String text, SegmentType type) {
			this(text, type, null);
		}

		public StyledSegment(String text, SegmentType type, String language) {
			this.text = text;
			this.type = type;
			this.language = language;
		}
	}

	/**
	 * Normalises line endings to \n to keep offsets synchronized across platforms
	 * (e.g. Windows SWT styled text).
	 */
	public List<StyledSegment> parse(String markdown) {
		List<StyledSegment> segments = new ArrayList<>();
		if (markdown == null || markdown.isEmpty()) {
			return segments;
		}

		String normalized = markdown.replace("\r\n", "\n").replace("\r", "\n");
		tokenize(normalized, segments);
		return segments;
	}

	private void tokenize(String text, List<StyledSegment> segments) {
		if (text.isEmpty()) {
			return;
		}

		Matcher cb = CODE_BLOCK_PATTERN.matcher(text);
		int lastIdx = 0;
		while (cb.find()) {
			if (cb.start() > lastIdx) {
				tokenizeTextBlocks(text.substring(lastIdx, cb.start()), segments);
			}
			String lang = cb.group(1);
			String code = cb.group(2);
			segments.add(new StyledSegment(code, SegmentType.CODE_BLOCK, lang));
			lastIdx = cb.end();
		}
		if (lastIdx < text.length()) {
			tokenizeTextBlocks(text.substring(lastIdx), segments);
		}
	}

	private void tokenizeTextBlocks(String text, List<StyledSegment> segments) {
		if (text.isEmpty()) {
			return;
		}

		String[] lines = text.split("\n", -1);
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			String lineWithBreak = (i < lines.length - 1) ? line + "\n" : line;

			if (line.startsWith("### ")) {
				segments.add(new StyledSegment(line.substring(4) + ((i < lines.length - 1) ? "\n" : ""),
						SegmentType.HEADER_3));
			} else if (line.startsWith("## ")) {
				segments.add(new StyledSegment(line.substring(3) + ((i < lines.length - 1) ? "\n" : ""),
						SegmentType.HEADER_2));
			} else if (line.startsWith("# ")) {
				segments.add(new StyledSegment(line.substring(2) + ((i < lines.length - 1) ? "\n" : ""),
						SegmentType.HEADER_1));
			} else if (line.startsWith("- ") || line.startsWith("* ")) {
				// Bullet list item
				segments.add(new StyledSegment("  • ", SegmentType.NORMAL));
				tokenizeInline(line.substring(2) + ((i < lines.length - 1) ? "\n" : ""), segments);
			} else {
				tokenizeInline(lineWithBreak, segments);
			}
		}
	}

	private void tokenizeInline(String text, List<StyledSegment> segments) {
		if (text.isEmpty()) {
			return;
		}

		Pattern inlinePattern = Pattern.compile("(`([^`]+)`)|(\\*\\*([^*]+)\\*\\*)|(\\*([^*]+)\\*)");
		Matcher m = inlinePattern.matcher(text);
		int lastIdx = 0;
		while (m.find()) {
			if (m.start() > lastIdx) {
				segments.add(new StyledSegment(text.substring(lastIdx, m.start()), SegmentType.NORMAL));
			}
			if (m.group(2) != null) {
				segments.add(new StyledSegment(m.group(2), SegmentType.INLINE_CODE));
			} else if (m.group(4) != null) {
				segments.add(new StyledSegment(m.group(4), SegmentType.BOLD));
			} else if (m.group(6) != null) {
				segments.add(new StyledSegment(m.group(6), SegmentType.ITALIC));
			}
			lastIdx = m.end();
		}
		if (lastIdx < text.length()) {
			segments.add(new StyledSegment(text.substring(lastIdx), SegmentType.NORMAL));
		}
	}

	public StyleRange createStyleRange(StyledSegment segment, int startOffset) {
		if (segment.type == SegmentType.NORMAL) {
			return null;
		}

		StyleRange range = new StyleRange();
		range.start = startOffset;
		range.length = segment.text.length();

		switch (segment.type) {
		case CODE_BLOCK:
		case INLINE_CODE:
			range.background = styles.getCodeBackground();
			range.foreground = styles.getCodeForeground();
			range.font = styles.getCodeFont();
			break;
		case BOLD:
			range.fontStyle = SWT.BOLD;
			range.font = styles.getBoldFont();
			break;
		case ITALIC:
			range.fontStyle = SWT.ITALIC;
			break;
		case HEADER_1:
			range.font = styles.getHeaderFont();
			range.foreground = styles.getHeaderColor();
			range.fontStyle = SWT.BOLD;
			break;
		case HEADER_2:
			range.font = styles.getSubHeaderFont();
			range.foreground = styles.getHeaderColor();
			range.fontStyle = SWT.BOLD;
			break;
		case HEADER_3:
			range.font = styles.getBoldFont();
			range.foreground = styles.getLinkColor();
			range.fontStyle = SWT.BOLD;
			break;
		default:
			return null;
		}
		return range;
	}

	// --- Helper methods ---
	public boolean hasCodeBlocks(String text) {
		return text != null && CODE_BLOCK_PATTERN.matcher(text).find();
	}

	public List<CodeBlock> extractCodeBlocks(String text) {
		List<CodeBlock> blocks = new ArrayList<>();
		if (text == null) {
			return blocks;
		}
		Matcher m = CODE_BLOCK_PATTERN.matcher(text);
		while (m.find()) {
			blocks.add(new CodeBlock(m.group(1), m.group(2), m.start(), m.end()));
		}
		return blocks;
	}

	public static class CodeBlock {
		public final String language, code;
		public final int startIndex, endIndex;

		public CodeBlock(String l, String c, int s, int e) {
			this.language = l == null || l.isBlank() ? "code" : l.trim();
			this.code = c;
			this.startIndex = s;
			this.endIndex = e;
		}
	}
}