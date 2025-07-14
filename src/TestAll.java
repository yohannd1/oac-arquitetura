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
		// The strategy here is, beginning with reg0 = 3:
		//   { double(); add() } yields reg0 = 11,
		//   while { add(); double() } yields reg0 = 16
		//
		// So let's try to run the first case and the result can only be 11.
		Architecture arch = runCode(new String[] {
			"jmp main",
			"double:",
			"  add %reg0 %reg0",
			"  ret",
			"add:",
			"  move 5 %reg1",
			"  add %reg1 %reg0",
			"  ret",
			"main:",
			"  move 3 %reg0",
			"  call double",
			"  call add",
		});
		assertEquals(11, arch.tGetREG0().getData());
	}
}
