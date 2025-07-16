package assembler;

import java.util.ArrayList;

import static org.junit.Assert.*;
import org.junit.Test;

import assembler.Assembler.ParseException;

public class TestAssembler {
	static private Assembler parseCode(String[] codeLines) {
		try {
			Assembler assembler = new Assembler();
			assembler.readLines(codeLines);
			assembler.parseAll();
			return assembler;
		} catch (ParseException ex) {
			throw new RuntimeException("Failed to run assembler: " + ex);
		}
	}

	static private String[] assembleCode(String[] codeLines) {
		return parseCode(codeLines).makeExecutableLines();
	}

	static private void assertCompiles(String[] codeLines) {
		assertTrue(parseCode(codeLines).makeExecutableLines() != null);
	}

	static private void compileAndExpectStarts(String[] codeLines, int[] expectedStart, int skipN) {
		String[] exec = assembleCode(codeLines);

		if (exec.length - skipN < expectedStart.length)
			throw new RuntimeException("expected start does not match (it's longer than the input)");

		for (int i = 0; i < expectedStart.length; i++) {
			String execVal = exec[i + skipN];
			String expectedVal = Integer.toString(expectedStart[i]);

			if (!exec[i + skipN].equals(expectedVal))
				throw new RuntimeException(String.format("(at #%d) expected:<%s> but was:<%s>", i, exec[i], expectedVal));
		}
	}

	/**
	 * Test function for the elemental program constructs - variables, instructions and labels.
	 */
	@Test
	public void testConstructs() {
		assertCompiles(new String[] {
			"move 10 %reg0",
		});

		assertCompiles(new String[] {
			"foo",
			"move %reg1 foo",
		});

		assertCompiles(new String[] {
			"var1",
			"var2",
			"move %reg1 %reg0",
			"jmp bar",
			"inc var1",
			"bar:",
			"move var2 %reg1",
		});
	}

	static private int minimumLength = assembleCode(new String[] {}).length;

	@Test
	public void testSimpleCode() {
		String[] program = new String[] {
			"move 135 %reg0",
		};
		int[] bytes = new int[] { 9, 135, 1 };
		compileAndExpectStarts(program, bytes, minimumLength);
	}

	@Test
	public void testVariableCode() {
		String[] program = new String[] {
			"var1",
			"move %reg0 var1",
		};
		int[] bytes = new int[] { 7, 1, 255 };
		compileAndExpectStarts(program, bytes, minimumLength);
	}

	@Test
	public void testLabelCode() {
		String[] program = new String[] {
			"jmp b",
			"move 5 %reg0",
			"b:",
			"move 5 %reg0",
		};
		int[] bytes = new int[] { 12, 5, 9, 5, 1, 9, 5, 1 };
		compileAndExpectStarts(program, bytes, minimumLength);
	}

	@Test
	public void testImmSign() {
		String[] program = new String[] {
			"move -8 %reg0",
			"move 5 %reg0",
			"move +10 %reg0",
		};
		int[] bytes = new int[] { 9, -8, 1, 9, 5, 1, 9, 10, 1 };
		compileAndExpectStarts(program, bytes, minimumLength);
	}
}
