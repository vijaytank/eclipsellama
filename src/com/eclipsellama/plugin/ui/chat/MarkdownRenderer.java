package com.eclipsellama.plugin.ui.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

public class MarkdownRenderer {

    // Regex: \\R fängt \r\n, \n und \r ab.
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(\\w*)\\R?([\\s\\S]*?)```");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*([^*]+)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*([^*]+)\\*");

    private final ChatStyles styles;

    public MarkdownRenderer(ChatStyles styles) {
        this.styles = styles;
    }

    public enum SegmentType { NORMAL, CODE_BLOCK, INLINE_CODE, BOLD, ITALIC }

    public static class StyledSegment {
        public final String text;
        public final SegmentType type;
        public final String language;

        public StyledSegment(String text, SegmentType type) { this(text, type, null); }
        public StyledSegment(String text, SegmentType type, String language) {
            this.text = text;
            this.type = type;
            this.language = language;
        }
    }

    /**
     * WICHTIG: Diese Methode normalisiert alle Zeilenumbrüche auf \n.
     * Das ist die einzige Chance, dass Offsets in Java und SWT (Windows) synchron bleiben.
     */
    public List<StyledSegment> parse(String markdown) {
        List<StyledSegment> segments = new ArrayList<>();
        if (markdown == null || markdown.isEmpty()) return segments;

        // Schritt 1: Windows-Leck fixen. Alles auf Standard-\n bringen.
        String normalized = markdown.replace("\r\n", "\n").replace("\r", "\n");
        
        tokenize(normalized, segments);
        return segments;
    }

    private void tokenize(String text, List<StyledSegment> segments) {
        if (text.isEmpty()) return;

        Matcher cb = CODE_BLOCK_PATTERN.matcher(text);
        if (cb.find()) {
            if (cb.start() > 0) tokenize(text.substring(0, cb.start()), segments);
            
            // Code-Inhalt extrahieren
            String lang = cb.group(1);
            String code = cb.group(2);
            segments.add(new StyledSegment(code, SegmentType.CODE_BLOCK, lang));

            if (cb.end() < text.length()) tokenize(text.substring(cb.end()), segments);
            return;
        }

        Matcher ic = INLINE_CODE_PATTERN.matcher(text);
        if (ic.find()) {
            if (ic.start() > 0) tokenize(text.substring(0, ic.start()), segments);
            segments.add(new StyledSegment(ic.group(1), SegmentType.INLINE_CODE));
            if (ic.end() < text.length()) tokenize(text.substring(ic.end()), segments);
            return;
        }

        Matcher b = BOLD_PATTERN.matcher(text);
        if (b.find()) {
            if (b.start() > 0) tokenize(text.substring(0, b.start()), segments);
            segments.add(new StyledSegment(b.group(1), SegmentType.BOLD));
            if (b.end() < text.length()) tokenize(text.substring(b.end()), segments);
            return;
        }

        Matcher i = ITALIC_PATTERN.matcher(text);
        if (i.find()) {
            if (i.start() > 0) tokenize(text.substring(0, i.start()), segments);
            segments.add(new StyledSegment(i.group(1), SegmentType.ITALIC));
            if (i.end() < text.length()) tokenize(text.substring(i.end()), segments);
            return;
        }

        segments.add(new StyledSegment(text, SegmentType.NORMAL));
    }

    public StyleRange createStyleRange(StyledSegment segment, int startOffset) {
        if (segment.type == SegmentType.NORMAL) return null;

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
                break;
            case ITALIC:
                range.fontStyle = SWT.ITALIC;
                break;
            default: return null;
        }
        return range;
    }

    // --- Originale Hilfsmethoden ---
    public boolean hasCodeBlocks(String text) {
        return CODE_BLOCK_PATTERN.matcher(text).find();
    }

    public List<CodeBlock> extractCodeBlocks(String text) {
        List<CodeBlock> blocks = new ArrayList<>();
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
            this.language = l; this.code = c; this.startIndex = s; this.endIndex = e;
        }
    }
}