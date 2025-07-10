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
		Assembler assembler = new Assembler();

		try {
			assembler.readLines(codeLines);
			assembler.parseAll();
		} catch (ParseException ex) {
			throw new RuntimeException("Failed to run assembler: " + ex);
		}

		String[] executable = assembler.makeExecutableLines();

		Architecture architecture = new Architecture(false);
		architecture.readExecLines(executable);
		architecture.controlUnitEexec();
		return architecture;
	}

	@Test
	public void test1() {
		Architecture arch = runCode(new String[] {
			"move 15 %reg0",
		});
		assertEquals(15, arch.tGetREG0().getData());
	}

	@Test
	public void test2() {
		Architecture arch = runCode(new String[] {
			"move 10 %reg0",
			"move 15 %reg1",
			"jgt %reg1 %reg0 end",
			"move 20 %reg1",
			"end:",
		});
		assertNotEquals(20, arch.tGetREG1().getData());
	}
}
