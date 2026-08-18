package com.eclipsellama.plugin.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.eclipsellama.plugin.context.CodeContextCollector.Context;

/**
 * Unit tests for {@link CodeContextCollector}. Runs as a plain JUnit test.
 */
public class CodeContextCollectorTest {

	@Test
	public void testCollectsStructuralContext() {
		String source = "package com.example;\n" + "import java.util.List;\n" + "import java.util.Map;\n"
				+ "public class MyService {\n" + "  public String process(List<String> items) {\n"
				+ "    return \"done\";\n" + "  }\n" + "}\n";
		Context ctx = CodeContextCollector.collect(source);
		assertEquals("com.example", ctx.packageName);
		assertEquals("MyService", ctx.className);
		assertEquals("process", ctx.methodName);
		assertEquals(2, ctx.imports.size());
		assertTrue(ctx.imports.contains("java.util.List"));
		assertTrue(ctx.imports.contains("java.util.Map"));
	}

	@Test
	public void testPromptPrefixContainsFragments() {
		String source = "package com.example;\npublic class MyService {\n  public void run() {}\n}\n";
		Context ctx = CodeContextCollector.collect(source);
		String prefix = ctx.toPromptPrefix();
		assertTrue(prefix.contains("com.example"));
		assertTrue(prefix.contains("MyService"));
		assertTrue(prefix.contains("run"));
	}

	@Test
	public void testBlankReturnsEmpty() {
		Context ctx = CodeContextCollector.collect("   ");
		assertEquals("", ctx.packageName);
		assertEquals("", ctx.className);
	}
}
