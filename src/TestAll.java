package architecture;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import static org.junit.Assert.*;

import components.Memory;
import architecture.Architecture;
import assembler.Assembler;
import assembler.Assembler.ParseException;

public class TestAll {
	static private Architecture runCode(String[] codeLines) {
		return runCode(codeLines, false);
	}

	static private Architecture runCode(String[] codeLines, boolean sim) {
		Assembler assembler = new Assembler();

		try {
			assembler.readLines(codeLines);
			assembler.parseAll();
		} catch (ParseException ex) {
			throw new RuntimeException("Failed to run assembler: " + ex);
		}

		String[] executable = assembler.makeExecutableLines();

		if (sim)
			System.out.printf("Program: %s\n", arrayToString(executable));

		Architecture architecture = new Architecture(sim);
		architecture.readExecLines(executable);
		architecture.controlUnitEexec();
		return architecture;
	}

	static private Architecture runFile(String path) {
		try {
			Assembler assembler = new Assembler();
			assembler.read(path);
			assembler.parseAll();
			assembler.makeExecutable(path);

			Architecture architecture = new Architecture(false);
			architecture.readExec(path);
			architecture.controlUnitEexec();
			return architecture;
		} catch (ParseException ex) {
			throw new RuntimeException("Failed to run assembler: " + ex);
		} catch (IOException ex) {
			throw new RuntimeException("Failed to load file: " + ex);
		}
	}

	@Test
	public void testImmMove() {
		Architecture arch = runCode(new String[] {
			"move 15 %reg0",
		});
		assertEquals(15, arch.tGetREG0().getData());
	}

	@Test
	public void testCompareJump() {
		Architecture arch = runCode(new String[] {
			"move 10 %reg0",
			"move 15 %reg1",
			"jgt %reg1 %reg0 end",
			"move 20 %reg1",
			"end:",
		});
		assertNotEquals(20, arch.tGetREG1().getData());
	}

	@Test
	public void testCall() {
		Architecture arch = runFile("examples/ex03-call");
		assertEquals(11, arch.tGetREG0().getData());
	}

	private static <T> String arrayToString(T[] arr) {
		if (arr.length == 0)
			return "[]";

		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < arr.length - 1; i++) {
			sb.append(arr[i].toString());
			sb.append(", ");
		}
		sb.append(arr[arr.length - 1].toString());
		sb.append("]");
		return sb.toString();
	}
}
