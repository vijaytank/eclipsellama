package com.eclipsellama.plugin.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * Unit tests for the diff-computation logic in {@link CodeDiffDialog}. The
 * actual compare dialog requires the Eclipse runtime, so only the pure
 * line-diff helper is tested here.
 */
public class CodeDiffDialogTest {

	@Test
	public void testIdenticalLinesAreContext() {
		List<String> diff = CodeDiffDialog.computeSimpleDiff("a\nb", "a\nb");
		assertEquals(2, diff.size());
		assertTrue(diff.get(0).startsWith(" "));
	}

	@Test
	public void testDetectsAddedAndRemoved() {
		List<String> diff = CodeDiffDialog.computeSimpleDiff("a\nb", "a\nc");
		assertTrue(diff.contains("-b"));
		assertTrue(diff.contains("+c"));
	}

	@Test
	public void testNullHandling() {
		List<String> diff = CodeDiffDialog.computeSimpleDiff(null, "x");
		assertEquals(1, diff.size());
		assertEquals("+x", diff.get(0));
	}
}
