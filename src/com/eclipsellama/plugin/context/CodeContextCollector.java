package com.eclipsellama.plugin.context;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts structural context (package, class name, method name, imports,
 * surrounding lines) from a Java source snippet so handlers can build
 * context-aware prompts.
 */
public final class CodeContextCollector {

	private static final Pattern CLASS_PATTERN = Pattern.compile(
			"(?:public\\s+|private\\s+|protected\\s+|final\\s+|abstract\\s+)*(?:class|interface|enum|record)\\s+([A-Za-z0-9_]+)");
	private static final Pattern METHOD_PATTERN = Pattern.compile(
			"(?:(?:public|private|protected|static|final|synchronized|abstract|default)\\s+)*[A-Za-z0-9_<>,\\s\\[\\]]+\\s+([a-zA-Z][A-Za-z0-9_]*)\\s*\\([^)]*\\)\\s*\\{");
	private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+([A-Za-z0-9_.]+);");
	private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([A-Za-z0-9_.]+);");

	private CodeContextCollector() {
	}

	/**
	 * Extracts the structural context from the given source snippet. Never returns
	 * null.
	 */
	public static Context collect(String source) {
		if (source == null || source.isBlank()) {
			return new Context("", "", "", List.of(), "");
		}

		String pkg = firstGroup(PACKAGE_PATTERN, source);
		String className = firstGroup(CLASS_PATTERN, source);
		String methodName = firstGroup(METHOD_PATTERN, source);

		Set<String> imports = new LinkedHashSet<>();
		Matcher im = IMPORT_PATTERN.matcher(source);
		while (im.find()) {
			imports.add(im.group(1));
		}

		String selected = trimToWindow(source, 8, 3);

		return new Context(pkg, className, methodName, new ArrayList<>(imports), selected);
	}

	private static String firstGroup(Pattern pattern, String source) {
		Matcher m = pattern.matcher(source);
		return m.find() ? m.group(1) : "";
	}

	private static String trimToWindow(String source, int after, int before) {
		String[] lines = source.split("\\R");
		if (lines.length <= after + before) {
			return source;
		}
		StringBuilder sb = new StringBuilder();
		int start = Math.max(0, lines.length - after - before);
		int end = Math.min(lines.length, start + after + before);
		for (int i = start; i < end; i++) {
			sb.append(lines[i]).append('\n');
		}
		return sb.toString().trim();
	}

	/** Immutable structural snapshot of a Java source snippet. */
	public static final class Context {
		public final String packageName;
		public final String className;
		public final String methodName;
		public final List<String> imports;
		public final String snippet;

		Context(String packageName, String className, String methodName, List<String> imports, String snippet) {
			this.packageName = packageName;
			this.className = className;
			this.methodName = methodName;
			this.imports = imports;
			this.snippet = snippet;
		}

		/**
		 * Renders the context as a prompt preamble, omitting empty fields.
		 */
		public String toPromptPrefix() {
			StringBuilder sb = new StringBuilder();
			if (!packageName.isEmpty()) {
				sb.append("Package: ").append(packageName).append('\n');
			}
			if (!className.isEmpty()) {
				sb.append("Class: ").append(className).append('\n');
			}
			if (!methodName.isEmpty()) {
				sb.append("Method: ").append(methodName).append('\n');
			}
			if (!imports.isEmpty()) {
				sb.append("Imports: ").append(String.join(", ", imports)).append('\n');
			}
			if (sb.length() > 0) {
				sb.append("---\n");
			}
			return sb.toString();
		}
	}
}
