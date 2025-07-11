package architecture;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import static org.junit.Assert.*;

import components.Memory;
import architecture.Architecture.CommandID;

public class TestArchitecture {
	static private void copyIntoMemory(Memory mem, int start, int[] data) {
		int[] memData = mem.getDataList();
		for (int i = 0; i < data.length; i++)
			memData[start + i] = data[i];
	}

	static private Architecture makeArchWithProgram(int[] program) {
		Architecture arch = new Architecture(false);
		copyIntoMemory(arch.tGetMemory(), 0, program);
		return arch;
	}

	@Test
	public void testFetch() {
		Architecture arch = makeArchWithProgram(new int[] { 100 });
		arch.fetch();
		assertEquals(100, arch.tGetIR().getData());
	}

	@Test
	public void testAdd_rr() {
		Architecture arch;

		arch = makeArchWithProgram(new int[] {
			CommandID.ADD_REG_REG.toInt(), 1, 2,
		});
		arch.tGetIntBus().put(25);
		arch.tGetREG0().store();

		arch.tGetIntBus().put(10);
		arch.tGetREG1().store();

		arch.controlUnitCycle();
		assertEquals(35, arch.tGetREG1().getData());
	}

	@Test
	public void testAdd_mr() {
		Architecture arch;

		arch = makeArchWithProgram(new int[] {
			CommandID.ADD_MEM_REG.toInt(), 150, 1,
		});
		arch.tGetIntBus().put(25);
		arch.tGetREG0().store();

		arch.tGetExtBus().put(150);
		arch.tGetMemory().store();

		arch.tGetExtBus().put(90);
		arch.tGetMemory().store();

		arch.controlUnitCycle();
		assertEquals(115, arch.tGetREG0().getData());
	}

	@Test
	public void testAdd_rm() {
		Architecture arch;

		arch = makeArchWithProgram(new int[] {
			CommandID.ADD_REG_MEM.toInt(), 1, 150,
		});
		arch.tGetIntBus().put(30);
		arch.tGetREG0().store();

		arch.tGetExtBus().put(150);
		arch.tGetMemory().store();

		arch.tGetExtBus().put(10);
		arch.tGetMemory().store();

		arch.controlUnitCycle();
		assertEquals(40, arch.tGetMemory().getDataList()[150]);
	}

	@Test
	public void testSub_rr() {
		Architecture arch;

		arch = makeArchWithProgram(new int[] {
			CommandID.SUB_REG_REG.toInt(), 1, 2,
		});
		arch.tGetIntBus().put(25);
		arch.tGetREG0().store();

		arch.tGetIntBus().put(10);
		arch.tGetREG1().store();

		arch.controlUnitCycle();
		assertEquals(15, arch.tGetREG1().getData());
	}

	@Test
	public void testSub_mr() {
		Architecture arch;

		arch = makeArchWithProgram(new int[] {
			CommandID.SUB_MEM_REG.toInt(), 150, 1,
		});
		arch.tGetIntBus().put(10);
		arch.tGetREG0().store();

		arch.tGetExtBus().put(150);
		arch.tGetMemory().store();

		arch.tGetExtBus().put(100);
		arch.tGetMemory().store();

		arch.controlUnitCycle();
		assertEquals(90, arch.tGetREG0().getData());
	}

	@Test
	public void testSub_rm() {
		Architecture arch;

		arch = makeArchWithProgram(new int[] {
			CommandID.SUB_REG_MEM.toInt(), 1, 150,
		});
		arch.tGetIntBus().put(30);
		arch.tGetREG0().store();

		arch.tGetExtBus().put(150);
		arch.tGetMemory().store();

		arch.tGetExtBus().put(10);
		arch.tGetMemory().store();

		arch.controlUnitCycle();
		assertEquals(20, arch.tGetMemory().getDataList()[150]);
	}

	@Test
	public void testMoveMemReg() {
		Architecture arch = makeArchWithProgram(new int[] {
			CommandID.MOVE_MEM_REG.toInt(), 150, 1,
		});
		arch.tGetMemory().getDataList()[150] = 15;
		arch.controlUnitCycle();
		assertEquals(15, arch.tGetREG0().getData());
	}

	@Test
	public void testMoveRegMem() {
		Architecture arch = makeArchWithProgram(new int[] {
			CommandID.MOVE_REG_MEM.toInt(), 1, 80,
		});
		arch.tGetIntBus().put(125);
		arch.tGetREG0().store();
		arch.controlUnitCycle();
		assertEquals(125, arch.tGetMemory().getDataList()[80]);
	}

	@Test
	public void testMoveRegReg() {
		Architecture arch = makeArchWithProgram(new int[] {
			CommandID.MOVE_REG_REG.toInt(), 1, 2,
		});
		arch.tGetIntBus().put(125);
		arch.tGetREG0().store();
		arch.controlUnitCycle();
		assertEquals(125, arch.tGetREG1().getData());
	}

	@Test
	public void testMoveImmReg() {
		Architecture arch = makeArchWithProgram(new int[] {
			CommandID.MOVE_IMM_REG.toInt(), 35, 2,
		});
		arch.controlUnitCycle();
		assertEquals(35, arch.tGetREG1().getData());
	}

	@Test
	public void testIncReg() {
		Architecture arch = makeArchWithProgram(new int[] {
			CommandID.INC_REG.toInt(), 1,
		});
		arch.tGetIntBus().put(125);
		arch.tGetREG0().store();
		arch.controlUnitCycle();
		assertEquals(126, arch.tGetREG0().getData());
	}

	@Test
	public void testIncMem() {
		Architecture arch = makeArchWithProgram(new int[] {
			CommandID.INC_MEM.toInt(), 150,
		});
		arch.tGetMemory().getDataList()[150] = 15;
		arch.controlUnitCycle();
		assertEquals(16, arch.tGetMemory().getDataList()[150]);
	}

	@Test
	public void testJmp() {
		Architecture arch = makeArchWithProgram(new int[] {
			CommandID.JMP.toInt(), 100,
		});
		arch.controlUnitCycle();
		assertEquals(100, arch.tGetPC().getData());
	}

	@Test
	public void testJn() {
		Architecture arch;

		// Case 1: simulated negative result
		arch = makeArchWithProgram(new int[] {
			CommandID.JN.toInt(), 200
		});
		arch.setStatusFlags(-5);
		arch.controlUnitCycle();
		assertEquals(200, arch.tGetPC().getData());

		// Case 2: simulated non-negative result
		arch = makeArchWithProgram(new int[] {
			CommandID.JN.toInt(), 200,
		});
		arch.setStatusFlags(5);
		arch.controlUnitCycle();
		assertNotEquals(200, arch.tGetPC().getData());
	}

	@Test
	public void testJz() {
		Architecture arch;

		// Case 1: simulated zero result
		arch = makeArchWithProgram(new int[] {
			CommandID.JZ.toInt(), 200
		});
		arch.setStatusFlags(0);
		arch.controlUnitCycle();
		assertEquals(200, arch.tGetPC().getData());

		// Case 2: simulated non-zero result
		arch = makeArchWithProgram(new int[] {
			CommandID.JZ.toInt(), 200,
		});
		arch.setStatusFlags(5);
		arch.controlUnitCycle();
		assertNotEquals(200, arch.tGetPC().getData());
	}

	@Test
	public void testJnz() {
		Architecture arch;

		// Case 1: simulated non-zero result
		arch = makeArchWithProgram(new int[] {
			CommandID.JNZ.toInt(), 200
		});
		arch.setStatusFlags(12);
		arch.controlUnitCycle();
		assertEquals(200, arch.tGetPC().getData());

		// Case 2: simulated zero result
		arch = makeArchWithProgram(new int[] {
			CommandID.JNZ.toInt(), 200,
		});
		arch.setStatusFlags(0);
		arch.controlUnitCycle();
		assertNotEquals(200, arch.tGetPC().getData());
	}

	@Test
	public void testJeq() {
		Architecture arch;

		// Case 1: equal values
		arch = makeArchWithProgram(new int[] {
			CommandID.JEQ.toInt(), 1, 1, 200,
		});
		arch.controlUnitCycle();
		assertEquals(200, arch.tGetPC().getData());

		// Case 2: different values
		arch = makeArchWithProgram(new int[] {
			CommandID.JEQ.toInt(), 1, 2, 200,
		});
		arch.tGetIntBus().put(25);
		arch.tGetREG0().store();
		assertNotEquals(arch.tGetREG0().getData(), arch.tGetREG1().getData());
		arch.controlUnitCycle();
		assertNotEquals(200, arch.tGetPC().getData());
	}

	@Test
	public void testJgt() {
		Architecture arch;

		// Case 1: REG0 > REG1
		arch = makeArchWithProgram(new int[] {
			CommandID.JGT.toInt(), 1, 2, 200,
		});
		arch.tGetIntBus().put(25);
		arch.tGetREG0().store();
		arch.controlUnitCycle();
		assertEquals(200, arch.tGetPC().getData());

		// Case 2: REG0 = REG1 = 0
		arch = makeArchWithProgram(new int[] {
			CommandID.JGT.toInt(), 1, 2, 200,
		});
		arch.controlUnitCycle();
		assertNotEquals(200, arch.tGetPC().getData());
	}

	@Test
	public void testJlw() {
		Architecture arch;

		// Case 1: REG0 < REG1
		arch = makeArchWithProgram(new int[] {
			CommandID.JLW.toInt(), 1, 2, 200,
		});
		arch.tGetIntBus().put(5);
		arch.tGetREG0().store();
		arch.tGetIntBus().put(30);
		arch.tGetREG1().store();
		arch.controlUnitCycle();
		assertEquals(200, arch.tGetPC().getData());

		// Case 2: REG0 = REG1 = 0
		arch = makeArchWithProgram(new int[] {
			CommandID.JLW.toInt(), 1, 2, 200,
		});
		arch.controlUnitCycle();
		assertNotEquals(200, arch.tGetPC().getData());
	}

	@Test
	public void testCall() {
		Architecture arch;

		arch = makeArchWithProgram(new int[] {
			CommandID.CALL.toInt(), 100,
		});
		arch.tGetIntBus().put(200);
		arch.tGetStkBOT().store();
		arch.tGetStkTOP().store();
		arch.controlUnitCycle();

		assertEquals(199, arch.tGetStkTOP().getData());
		assertEquals(100, arch.tGetPC().getData());
		arch.tGetExtBus().put(199);
		arch.tGetMemory().read();
		assertEquals(2, arch.tGetExtBus().get());
	}

	@Test
	public void testRet() {
		Architecture arch;

		arch = makeArchWithProgram(new int[] {
			CommandID.RET.toInt(),
		});
		arch.tGetExtBus().put(199);
		arch.tGetMemory().store();
		arch.tGetExtBus().put(115);
		arch.tGetMemory().store();
		arch.tGetIntBus().put(199);
		arch.tGetStkTOP().store();
		arch.tGetIntBus().put(200);
		arch.tGetStkBOT().store();
		arch.controlUnitCycle();

		assertEquals(200, arch.tGetStkTOP().getData());
		assertEquals(115, arch.tGetPC().getData());
	}
}
