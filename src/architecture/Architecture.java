package architecture;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import components.Bus;
import components.Demux;
import components.Memory;
import components.Register;
import components.Ula;

public class Architecture {
	public enum CommandID {
		ADD_REG_REG, // add %<regA> %<regB>
		ADD_MEM_REG, // add <mem> %<regA>
		ADD_REG_MEM, // add %<regA> <mem>
		SUB_REG_REG, // sub <regA> <regB>
		SUB_MEM_REG, // sub <mem> %<regA>
		SUB_REG_MEM, // sub %<regA> <mem>
		MOVE_MEM_REG, // move <mem> %<regA>
		MOVE_REG_MEM, // move %<regA> <mem>
		MOVE_REG_REG, // move %<regA> %<regB>
		MOVE_IMM_REG, // move imm %<regA>
		INC_REG, // inc %<regA>
		INC_MEM, // inc <mem>
		JMP, // jmp <mem>
		JN, // jn <mem>
		JZ, // jz <mem>
		JNZ, // jnz <mem>
		JEQ, // jeq %<regA> %<regB> <mem>
		JGT, // jgt %<regA> %<regB> <mem>
		JLW, // jlw %<regA> %<regB> <mem>
		CALL, // call <mem>
		RET; // ret

		static private CommandID[] variants = CommandID.values();

		static public CommandID fromInt(int x) {
			if (x < 0 || x >= variants.length)
				return null;
			return variants[x];
		}

		public int toInt() {
			return this.ordinal();
		}
	}

	private static int MAIN_MEMORY_SIZE = 256;

	private boolean simulation;
	private boolean halt;

	private Bus intBus;
	private Bus extBus;

	/**
	 * The memory unit connected to the external bus.
	 *
	 * Operation overview:
	 *   store(): on the first call, reads an address `a` from extBus. On the second, reads a word `b` from extBus and performs `memory[a] <- b`
	 *   read(): reads an address `a` from extBus and puts back on extBus `memory[a]`
	 */
	private Memory memory;

	/**
	 * The memory unit used for conditional jumps.
	 *
	 * Operation overview:
	 *   storeIn0(): statusMem[0] <- intBus
	 *   storeIn1(): statusMem[1] <- intBus
	 *   read(): reads a 0 or 1 from `a` from intBus and performs `intBus <- statusMem[a]`
	 */
	private Memory statusMem;

	private Register PC;
	private Register IR;
	private Register SP; // FIXME: remover esse (não tem na arquitetura)
	private Register StkTOP;
	private Register StkBOT;
	private Register Flags;
	private Register REG0;
	private Register REG1;
	private Register REG2;
	private Register REG3;
	public Register[] registerList;

	/**
	 * The arithmetic logic unit (ALU), a.k.a. ULA.
	 *
	 * Operation overview:
	 *   add(): ula(1) <- ula(0) + ula(1)
	 *   sub(): ula(1) <- ula(0) - ula(1)
	 *   inc(): ula(1) <- ula(1) + 1
	 * None of these methods interact with intBus or extBus.
	 *
	 * Data transfer:
	 *   store(id): store the data from extBus into register #`id`
	 *   read(id): put the data from register #`id` into extBus
	 *   internalStore(id): store the data from intBus into register #`id`
	 *   internalRead(id): put the data from register #`id` into intBus
	 */
	private Ula ula;

	/**
	 * The register demux.
	 *
	 * Operations:
	 *   getValue(): get the current register ID in the demux
	 *   setValue(id): set the register ID to `id`
	 */
	private Demux demux;

	public int getRegisterID(String name) {
		for (int i = 0; i < registerList.length; i++) {
			if (registerList[i].getRegisterName().toLowerCase().equals(name.toLowerCase()))
				return i;
		}
		return -1;
	}

	private void componentsInstances() {
		intBus = new Bus();
		extBus = new Bus();

		memory = new Memory(MAIN_MEMORY_SIZE, extBus);
		statusMem = new Memory(2, intBus);

		PC = new Register("PC", intBus, intBus);
		IR = new Register("IR", intBus, intBus);
		SP = new Register("SP", intBus, intBus); // FIXME: remover (não tem na arquitetura)
		StkTOP = new Register("StkTOP", intBus, intBus);
		StkBOT = new Register("StkBOT", intBus, intBus);

		Flags = new Register(2, intBus);

		REG0 = new Register("REG0", intBus, intBus);
		REG1 = new Register("REG1", intBus, intBus);
		REG2 = new Register("REG2", intBus, intBus);
		REG3 = new Register("REG3", intBus, intBus);

		registerList = new Register[] { IR, REG0, REG1, REG2, REG3, PC, SP, StkTOP, StkBOT, Flags };
		ula = new Ula(extBus, intBus);
		demux = new Demux();
	}

	public Architecture() {
		componentsInstances();
		simulation = false;
	}

	public Architecture(boolean sim) {
		componentsInstances();
		simulation = sim;
	}

	public void setStatusFlags(int result) {
		Flags.setBit(0, 0);
		Flags.setBit(1, 0);
		if (result == 0) Flags.setBit(0, 1);
		if (result < 0) Flags.setBit(1, 1);
	}

	private void registersRead() {
		registerList[demux.getValue()].read();
	}

	private void registersInternalRead() {
		registerList[demux.getValue()].internalRead();
	}

	private void registersStore() {
		registerList[demux.getValue()].store();
	}

	private void registersInternalStore() {
		registerList[demux.getValue()].internalStore();
	}

	public void add_rr(){			   // RegB <- RegA + RegB
		PC.read();                     // PC -> bus(int)
		ula.internalStore(1);          // ULA(1) <- bus(int)
		ula.inc();                     // ULA++
		ula.internalRead(1);           // ULA(1) -> bus (int)
		PC.store();                    // PC <- bus(int)
		ula.internalStore(0);          // ULA(0) <- bus(int)
		ula.read(0);                   // ULA(0) -> bus(ext)
		memory.read();                 // Mem(r) <- bus(ext)
		ula.store(0);                  // ULA(0) <- bus(ext)
		ula.internalRead(0);           // ULA(0) -> bus(int)
		demux.setValue(intBus.get());  // RegID <- bus(int)
		registersRead();               // Reg(x) -> bus(int) (demux)
		IR.store();                    // IR <- bus(int)
		PC.read();                     // PC -> bus(int)
		ula.internalStore(1);          // ULA(1) <- bus(int)
		ula.inc();                     // ULA++
		ula.internalRead(1);           // ULA(1) -> bus (int)
		PC.store();                    // PC <- bus(int)
		ula.internalStore(0);          // ULA(0) <- bus(int)
		ula.read(0);                   // ULA(0) -> bus(ext)
		memory.read();                 // Mem(r) <- bus(ext)
		ula.store(0);                  // ULA(0) <- bus(ext)
		ula.internalRead(0);           // ULA(0) -> bus(int)
		demux.setValue(intBus.get());  // RegID <- bus(int)
		registersRead();               // Reg(x) -> bus(int) (demux)
		ula.internalStore(1);          // ULA(1) <- bus (int)
		IR.read();                     // IR -> bus (int)
		ula.internalStore(0);          // ULA(0) <- bus(int)
		ula.add();                     // ULA+
		ula.internalRead(1);           // ULA(1) -> bus (int)
		setStatusFlags(intBus.get());  // Flags
		registersStore();              // RegX <- bus (int)
		PC.read();                     // PC -> bus(int)
		ula.internalStore(1);          // ULA(1) <- bus(int)
		ula.inc();                     // ULA++
		ula.internalRead(1);           // ULA(1) -> bus (int)
		PC.store();                    // PC <- bus(int)
	}

	public void add_mr() {			   //RegA <- memória[mem] + RegA
		PC.read();                     // PC -> bus(int)
		ula.internalStore(1);          // ULA(1) <- bus(int)
		ula.inc();                     // ULA++
		ula.internalRead(1);           // ULA(1) -> bus (int)
		PC.store();                    // PC <- bus(int)
		ula.internalStore(0);          // ULA(0) <- bus(int)
		ula.read(0);                   // ULA(0) -> bus(ext)
		memory.read();                 // Mem(r) <- bus(ext)
		memory.read();                 // Mem(r) <- bus(ext)
		ula.store(0);                  // ULA(0) <- bus(ext)
		ula.internalRead(0);           // ULA(0) -> bus(int)
		IR.store();                    // IR <- bus(int)
		PC.read();                     // PC -> bus(int)
		ula.internalStore(1);          // ULA(1) <- bus(int)
		ula.inc();                     // ULA++
		ula.internalRead(1);           // ULA(1) -> bus (int)
		PC.store();                    // PC <- bus(int)
		ula.internalStore(0);          // ULA(0) <- bus(int)
		ula.read(0);                   // ULA(0) -> bus(ext)
		memory.read();                 // Mem(r) <- bus(ext)
		ula.store(0);                  // ULA(0) <- bus(ext)
		ula.internalRead(0);           // ULA(0) -> bus(int)
		demux.setValue(intBus.get());  // RegID <- bus(int)
		registersRead();               // Reg(x) -> bus (int) (demux)
		ula.internalStore(1);          // ULA(1) <- bus (int)
		IR.read();                     // IR -> bus (int)
		ula.internalStore(0);          // ULA(0) <- bus(int)
		ula.add();                     // ULA+
		ula.internalRead(1);           // ULA(1) -> bus (int)
		setStatusFlags(intBus.get());  // Flags
		registersStore();              // RegX <- bus (int)
		PC.read();                     // PC -> bus(int)
		ula.internalStore(1);          // ULA(1) <- bus(int)
		ula.inc();                     // ULA++
		ula.internalRead(1);           // ULA(1) -> bus (int)
		PC.store();                    // PC <- bus(int)
	}


	public void add_rm() {			   // Memória[mem] <- RegA + memória[mem]
		PC.read();                     // PC -> bus(int)
		ula.internalStore(1);          // ULA(1) <- bus(int)
		ula.inc();                     // ULA++
		ula.internalRead(1);           // ULA(1) -> bus (int)
		PC.store();                    // PC <- bus(int)
		ula.internalStore(0);          // ULA(0) <- bus(int)
		ula.read(0);                   // ULA(0) -> bus(ext)
		memory.read();                 // Mem(r) <- bus(ext)
		ula.store(0);                  // ULA(0) <- bus(ext)
		ula.internalRead(0);           // ULA(0) -> bus(int)
		demux.setValue(intBus.get());  // RegID <- bus(int)
		registersRead();               // Reg(x) -> bus (int) (demux)
		IR.store();                    // IR <- bus(int)
		PC.read();                     // PC -> bus(int)
		ula.internalStore(1);          // ULA(1) <- bus(int)
		ula.inc();                     // ULA++
		ula.internalRead(1);           // ULA(1) -> bus (int)
		PC.store();                    // PC <- bus(int)
		ula.internalStore(0);          // ULA(0) <- bus(int)
		ula.read(0);                   // ULA(0) -> bus(ext)
		memory.read();                 // Mem(r) <- bus(ext)
		memory.read();                 // Mem(r) <- bus(ext)
		ula.store(1);                  // ULA(1) <- bus(ext)
		PC.read();                     // PC -> bus(int)
		ula.internalStore(0);          // ULA(0) <- bus(int)
		ula.read(0);                   // ULA(0) -> bus(ext)
		memory.read();                 // Mem(r) <- bus(ext)
		memory.store();                // Mem(store) <- bus(ext)
		IR.read();                     // IR -> bus (int)
		ula.internalStore(0);          // ULA(0) <- bus(int)
		ula.add();                     // ULA+
		ula.read(1);                   // ULA(1) -> bus (ext)
		setStatusFlags(intBus.get());  // Flags
		memory.store();                // Mem(store) <- bus(ext)
		PC.read();                     // PC -> bus(int)
		ula.internalStore(1);          // ULA(1) <- bus(int)
		ula.inc();                     // ULA++
		ula.internalRead(1);           // ULA(1) -> bus (int)
		PC.store();                    // PC <- bus(int)
	}

	public void sub_rr() {				   // RegB <- RegA - RegB
		PC.read();                         // PC -> bus(int)
		ula.internalStore(1);              // ULA(1) <- bus(int)
		ula.inc();                         // ULA++
		ula.internalRead(1);               // ULA(1) -> bus (int)
		PC.store();                        // PC <- bus(int)
		ula.internalStore(0);              // ULA(0) <- bus(int)
		ula.read(0);                       // ULA(0) -> bus(ext)
		memory.read();                     // Mem(r) <- bus(ext)
		ula.store(0);                      // ULA(0) <- bus(ext)
		ula.internalRead(0);               // ULA(0) -> bus(int)
		demux.setValue(intBus.get());      // RegID <- bus(int)
		registersRead();                   // Reg(x) -> bus(int) (demux)
		IR.store();                        // IR <- bus(int)
		PC.read();                         // PC -> bus(int)
		ula.internalStore(1);              // ULA(1) <- bus(int)
		ula.inc();                         // ULA++
		ula.internalRead(1);               // ULA(1) -> bus (int)
		PC.store();                        // PC <- bus(int)
		ula.internalStore(0);              // ULA(0) <- bus(int)
		ula.read(0);                       // ULA(0) -> bus(ext)
		memory.read();                     // Mem(r) <- bus(ext)
		ula.store(0);                      // ULA(0) <- bus(ext)
		ula.internalRead(0);               // ULA(0) -> bus(int)
		demux.setValue(intBus.get());      // RegID <- bus(int)
		registersRead();                   // Reg(x) -> bus (int) (demux)
		ula.internalStore(1);              // ULA(1) <- bus (int)
		IR.read();                         // IR -> bus (int)
		ula.internalStore(0);              // ULA(0) <- bus(int)
		ula.sub();                         // ULA-
		ula.internalRead(1);               // ULA(1) -> bus (int)
		setStatusFlags(intBus.get());      // Flags
		registersStore();                  // RegX <- bus (int)
		PC.read();                         // PC -> bus(int)
		ula.internalStore(1);              // ULA(1) <- bus(int)
		ula.inc();                         // ULA++
		ula.internalRead(1);               // ULA(1) -> bus (int)
		PC.store();                        // PC <- bus(int)
	}

	public void sub_mr() { 			  // RegA <- memória[mem] - RegA
		PC.read();                    // PC -> bus(int)
		ula.internalStore(1);         // ULA(1) <- bus(int)
		ula.inc();                    // ULA++
		ula.internalRead(1);          // ULA(1) -> bus (int)
		PC.store();                   // PC <- bus(int)
		ula.internalStore(0);         // ULA(0) <- bus(int)
		ula.read(0);                  // ULA(0) -> bus(ext)
		memory.read();                // Mem(r) <- bus(ext)
		memory.read();                // Mem(r) <- bus(ext)
		ula.store(0);                 // ULA(0) <- bus(ext)
		ula.internalRead(0);          // ULA(0) -> bus(int)
		IR.store();                   // IR <- bus(int)
		PC.read();                    // PC -> bus(int)
		ula.internalStore(1);         // ULA(1) <- bus(int)
		ula.inc();                    // ULA++
		ula.internalRead(1);          // ULA(1) -> bus (int)
		PC.store();                   // PC <- bus(int)
		ula.internalStore(0);         // ULA(0) <- bus(int)
		ula.read(0);                  // ULA(0) -> bus(ext)
		memory.read();                // Mem(r) <- bus(ext)
		ula.store(0);                 // ULA(0) <- bus(ext)
		ula.internalRead(0);          // ULA(0) -> bus(int)
		demux.setValue(intBus.get()); // RegID <- bus(int)
		registersRead();              // Reg(x) -> bus (int) (demux)
		ula.internalStore(1);         // ULA(1) <- bus (int)
		IR.read();                    // IR -> bus (int)
		ula.internalStore(0);         // ULA(0) <- bus(int)
		ula.sub();                    // ULA-
		ula.internalRead(1);          // ULA(1) -> bus (int)
		setStatusFlags(intBus.get()); // Flags
		registersStore();             // RegX <- bus (int)
		PC.read();                    // PC -> bus(int)
		ula.internalStore(1);         // ULA(1) <- bus(int)
		ula.inc();                    // ULA++
		ula.internalRead(1);          // ULA(1) -> bus (int)
		PC.store();                   // PC <- bus(int)
	}

	public void sub_rm() {			   // memória[mem] <- RegA - memória[mem]
		PC.read();                     // PC -> bus(int)
		ula.internalStore(1);          // ULA(1) <- bus(int)
		ula.inc();                     // ULA++
		ula.internalRead(1);           // ULA(1) -> bus (int)
		PC.store();                    // PC <- bus(int)
		ula.internalStore(0);          // ULA(0) <- bus(int)
		ula.read(0);                   // ULA(0) -> bus(ext)
		memory.read();                 // Mem(r) <- bus(ext)
		ula.store(0);                  // ULA(0) <- bus(ext)
		ula.internalRead(0);           // ULA(0) -> bus(int)
		demux.setValue(intBus.get());  // RegID <- bus(int)
		registersRead();               // Reg(x) -> bus (int) (demux)
		IR.store();                    // IR <- bus(int)
		PC.read();                     // PC -> bus(int)
		ula.internalStore(1);          // ULA(1) <- bus(int)
		ula.inc();                     // ULA++
		ula.internalRead(1);           // ULA(1) -> bus (int)
		PC.store();                    // PC <- bus(int)
		ula.internalStore(0);          // ULA(0) <- bus(int)
		ula.read(0);                   // ULA(0) -> bus(ext)
		memory.read();                 // Mem(r) <- bus(ext)
		memory.read();                 // Mem(r) <- bus(ext)
		ula.store(1);                  // ULA(1) <- bus(ext)
		PC.read();                     // PC -> bus(int)
		ula.internalStore(0);          // ULA(0) <- bus(int)
		ula.read(0);                   // ULA(0) -> bus(ext)
		memory.read();                 // Mem(r) <- bus(ext)
		memory.store();                // Mem(store) <- bus(ext)
		IR.read();                     // IR -> bus (int)
		ula.internalStore(0);          // ULA(0) <- bus(int)
		ula.sub();                     // ULA-
		ula.read(1);                   // ULA(1) -> bus (ext)
		setStatusFlags(intBus.get());  // Flags
		memory.store();                // Mem(store) <- bus(ext)
		PC.read();                     // PC -> bus(int)
		ula.internalStore(1);          // ULA(1) <- bus(int)
		ula.inc();                     // ULA++
		ula.internalRead(1);           // ULA(1) -> bus (int)
		PC.store();                    // PC <- bus(int)
	}

	public void move_mr() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int mem_addr = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		intBus.put(mem_addr);
		extBus.put(intBus.get());
        memory.read();
		memory.read();
		intBus.put(extBus.get());

		demux.setValue(regA_id);
		registersStore();
	}

	public void move_rm() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int mem_addr = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		intBus.put(mem_addr);
		extBus.put(intBus.get());
		memory.store();

		demux.setValue(regA_id);
		registersRead();
		extBus.put(intBus.get());
		memory.store();
	}

	public void move_rr() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regB_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		demux.setValue(regA_id);
		registersRead();

		demux.setValue(regB_id);
		registersStore();
	}

	public void move_ir() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int immediate = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		intBus.put(immediate);
		demux.setValue(regA_id);
		registersStore();
	}

	public void inc_r() {
		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();

		ula.internalStore(0);
		ula.read(0);
		memory.read();
		ula.store(0);
		ula.internalRead(0);
		demux.setValue(intBus.get());
		registersRead();

		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		setStatusFlags(intBus.get());
		registersStore();

		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();
	}

	public void inc_m() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int mem_addr = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		intBus.put(mem_addr);
		extBus.put(intBus.get());
        memory.read();
		memory.read();
		intBus.put(extBus.get());
		ula.store(0);
		ula.inc();

		intBus.put(mem_addr);
		extBus.put(intBus.get());
		memory.store();

		ula.read(0);
		setStatusFlags(intBus.get());
		extBus.put(intBus.get());
		memory.store();
	}

	public void jmp() {
		PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
		int addr = intBus.get();

		intBus.put(addr);
		PC.store();
	}

	public void jn() {
		PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
		int addr = intBus.get();
		PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		if (Flags.getBit(1) == 1) {
			intBus.put(addr);
			PC.store();
		}
	}

	public void jz() {
		PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
		int addr = intBus.get();
		PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		if (Flags.getBit(0) == 1) {
			intBus.put(addr);
			PC.store();
		}
	}

	public void jnz() {
		PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
		int addr = intBus.get();
		PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		if (Flags.getBit(0) == 0) {
			intBus.put(addr);
			PC.store();
		}
	}

	public void jeq() {
		// pc++
		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();

		// read regA id from memory and put it on intBus
		ula.read(1);
		memory.read();
		ula.store(0);
		ula.internalRead(0);

		// get the id and read the specified register's value, then store it into IR
		demux.setValue(intBus.get());
		registersRead();
		IR.store();

		// pc++
		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();

		// read regB id from memory and put it on intBus
		ula.read(1);
		memory.read();
		ula.store(0);
		ula.internalRead(0);

		// get the regB id and read the specified register's value, then store it into ula(1)
		demux.setValue(intBus.get());
		registersRead();
		ula.internalStore(1);

		// get regA's value (from IR) and put it into ula(0)
		IR.read();
		ula.internalStore(0);

		// perform a subtraction and update the flags register
		ula.sub();
		ula.internalRead(1);
		setStatusFlags(intBus.get());

		// pc++
		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();

		// get jump address from memory, and put it into ula(0)
		ula.read(1);
		memory.read();
		ula.store(0);

		// put the address in the status memory (slot 1, when the values were equal)
		ula.internalRead(0);
		statusMem.storeIn1();

		// pc++
		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();

		// put the address of the next instruction in the status memory (slot 0, when the values were different)
		PC.read();
		statusMem.storeIn0();

		// jump to the address (based on the zero flag)
		intBus.put(Flags.getBit(0));
		statusMem.read();
		PC.store();
	}

	public void jgt() {
		// pc++
		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();

		// read regA id from memory and put it on intBus
		ula.read(1);
		memory.read();
		ula.store(0);
		ula.internalRead(0);

		// get the id and read the specified register's value, then store it into IR
		demux.setValue(intBus.get());
		registersRead();
		IR.store();

		// pc++
		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();

		// read regB id from memory and put it on intBus
		ula.read(1);
		memory.read();
		ula.store(0);
		ula.internalRead(0);

		// get the regB id and read the specified register's value, then store it into ula(0)
		demux.setValue(intBus.get());
		registersRead();
		ula.internalStore(0);

		// get regA's value (from IR) and put it into ula(1)
		IR.read();
		ula.internalStore(1);

		// perform a subtraction and update the flags register
		ula.sub();
		ula.internalRead(1);
		setStatusFlags(intBus.get());

		// pc++
		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();

		// get jump address from memory, and put it into ula(0)
		ula.read(1);
		memory.read();
		ula.store(0);

		// put the address in the status memory (slot 1, when regA>regB)
		ula.internalRead(0);
		statusMem.storeIn1();

		// pc++
		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();

		// put the address of the next instruction in the status memory (slot 0, when regA<=regB)
		PC.read();
		statusMem.storeIn0();

		// jump to the address (based on the negative flag)
		intBus.put(Flags.getBit(1));
		statusMem.read();
		PC.store();
	}

	public void jlw() {
		// pc++
		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();

		// read regA id from memory and put it on intBus
		ula.read(1);
		memory.read();
		ula.store(0);
		ula.internalRead(0);

		// get the id and read the specified register's value, then store it into IR
		demux.setValue(intBus.get());
		registersRead();
		IR.store();

		// pc++
		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();

		// read regB id from memory and put it on intBus
		ula.read(1);
		memory.read();
		ula.store(0);
		ula.internalRead(0);

		// get the regB id and read the specified register's value, then store it into ula(1)
		demux.setValue(intBus.get());
		registersRead();
		ula.internalStore(1);

		// get regA's value (from IR) and put it into ula(0)
		IR.read();
		ula.internalStore(0);

		// perform a subtraction and update the flags register
		ula.sub();
		ula.internalRead(1);
		setStatusFlags(intBus.get());

		// pc++
		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();

		// get jump address from memory, and put it into ula(0)
		ula.read(1);
		memory.read();
		ula.store(0);

		// put the address in the status memory (slot 0, when regA>regB)
		ula.internalRead(0);
		statusMem.storeIn1();

		// pc++
		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();

		// put the address of the next instruction in the status memory (slot 1, when regA<=regB)
		PC.read();
		statusMem.storeIn0();

		// jump to the address (based on the negative flag)
		intBus.put(Flags.getBit(1));
		statusMem.read();
		PC.store();
	}

	public void call() {
		// put a -1 on ula(1)
		REG0.read();
		ula.internalStore(0);
		ula.internalStore(1);
		ula.inc();
		ula.sub();

		// decrement stktop
		StkTOP.read();
		ula.internalStore(0);
		ula.add();
		ula.internalRead(1);
		StkTOP.store();

		// pc++
		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();

		// get jump address from memory, store it into IR
		ula.read(1);
		memory.read();
		ula.store(1);
		ula.internalRead(1);
		IR.store();

		// send address in stktop to memory
		StkTOP.read();
		ula.internalStore(0);
		ula.read(0);
		memory.store();

		// pc++
		PC.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.store();

		// send address in pc to memory (and thus memory[stktop] <- pc)
		PC.read();
		ula.internalStore(0);
		ula.read(0);
		memory.store();

		// put the address from IR into PC
		IR.read();
		PC.store();
	}

	public void ret() {
		// pc <- memory[stktop]
		StkTOP.read();
		ula.internalStore(1);
		ula.read(1);
		memory.read();
		ula.store(1);
		ula.internalRead(1);
		PC.store();

		// stktop++
		StkTOP.read();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		StkTOP.store();
	}

	public void readExec(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename + ".dxf"));
		String linha;
		int i=0;
		while ((linha = br.readLine()) != null) {
			intBus.put(i);
			extBus.put(intBus.get());
			memory.store();
			intBus.put(Integer.parseInt(linha));
			extBus.put(intBus.get());
			memory.store();
			i++;
		}
		br.close();
	}

	public void readExecLines(String[] lines) {
		int i = 0;

		while (i < lines.length) {
			String linha = lines[i];

			intBus.put(i);
			extBus.put(intBus.get());
			memory.store();
			intBus.put(Integer.parseInt(linha));
			extBus.put(intBus.get());
			memory.store();

			i++;
		}
	}

	public void controlUnitEexec() {
		while (!halt)
			controlUnitCycle();
		if (simulation)
			System.out.println("--- EXECUTION HALTED ---");
	}

	public void controlUnitCycle() {
		if (halt) return;
		fetch();

		if (halt) return;
		decodeExecute();
	}

	private void decodeExecute() {
		IR.read();
		int command = intBus.get();

		if (simulation)
			simulationDecodeExecuteBefore();

		switch (CommandID.fromInt(command)) {
		case CommandID.ADD_REG_REG: add_rr(); break;
		case CommandID.ADD_MEM_REG: add_mr(); break;
		case CommandID.ADD_REG_MEM: add_rm(); break;
		case CommandID.SUB_REG_REG: sub_rr(); break;
		case CommandID.SUB_MEM_REG: sub_mr(); break;
		case CommandID.SUB_REG_MEM: sub_rm(); break;
		case CommandID.MOVE_MEM_REG: move_mr(); break;
		case CommandID.MOVE_REG_MEM: move_rm(); break;
		case CommandID.MOVE_REG_REG: move_rr(); break;
		case CommandID.MOVE_IMM_REG: move_ir(); break;
		case CommandID.INC_REG: inc_r(); break;
		case CommandID.INC_MEM: inc_m(); break;
		case CommandID.JMP: jmp(); break;
		case CommandID.JN: jn(); break;
		case CommandID.JZ: jz(); break;
		case CommandID.JNZ: jnz(); break;
		case CommandID.JEQ: jeq(); break;
		case CommandID.JGT: jgt(); break;
		case CommandID.JLW: jlw(); break;
		case CommandID.CALL: call(); break;
		case CommandID.RET: ret(); break;
		default:
			if (simulation)
				System.out.printf("Bad instruction %d encountered! Halting.\n", command);
			halt = true;
			break;
		}

		switch (command) {
			default:
				halt = true;
				break;
		}

		if (simulation) simulationDecodeExecuteAfter();
	}

	public void fetch() {
		PC.read(); // pc->intBus
		if (intBus.get() >= MAIN_MEMORY_SIZE) {
			halt = true;
			return;
		}

		extBus.put(intBus.get()); // intBus->extBus
		memory.read(); // mem(r)<-extBus
		intBus.put(extBus.get()); // extBus->intBus
		IR.store(); // IR<-intBus

		// pc++
		PC.read();
		ula.store(1);
		ula.inc();
		ula.read(1);
		PC.store();

		if (simulation)
			simulationFetch();
	}

	private void simulationPrintState() {
		CommandID id = CommandID.fromInt(IR.getData());
		String commandName = (id == null) ? "invalid command, will halt" : id.toString();

		System.out.printf("intBus: %d | extBus: %d\n", intBus.get(), extBus.get());
		System.out.printf("Status memory: [%d, %d]\n", statusMem.getDataList()[0], statusMem.getDataList()[1]);
		System.out.printf(
				"IR: %d (%s) | FLAGS: (Z=%d, N=%d)\n",
				IR.getData(), commandName, Flags.getBit(0), Flags.getBit(1));
		System.out.print("All registers: ");

		for (int i = 0; i < registerList.length; i++) {
			Register r = registerList[i];
			System.out.printf("%s: %s ", r.getRegisterName(), r.getData());
			if (i < registerList.length - 1)
				System.out.print("| ");
		}
		System.out.println();
	}

	private void simulationDecodeExecuteBefore() {
		System.out.println("--- BEFORE DECODE & EXECUTE ---");
		simulationPrintState();
		System.out.println();
	}

	private void simulationDecodeExecuteAfter() {
		System.out.println("--- AFTER DECODE & EXECUTE ---");
		simulationPrintState();
		System.out.println();

		waitForEnter();
	}

	private void waitForEnter() {
		Scanner sc = new Scanner(System.in);
		System.out.println("Press <Enter> to continue...");
		sc.nextLine();
	}

	private void simulationFetch() {
		if (simulation) {
			System.out.println("--- AFTER FETCH ---");
			simulationPrintState();
			System.out.println();
		}
	}

	public int getMemorySize() {
		return MAIN_MEMORY_SIZE;
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Usage: architecture <INPUT>");
			System.err.println("INPUT must be the name of a .dxf file, without the extension");
			System.exit(2);
		}

		String filename = args[0];
		Architecture arch = new Architecture(true);
		arch.readExec(filename);
		arch.controlUnitEexec();
	}

	// Functions prefixed with 't' should only be used in testing
	public Bus tGetIntBus() { return intBus; }
	public Bus tGetExtBus() { return extBus; }
	public Memory tGetMemory() { return memory; }
	public Memory tGetStatusMem() { return statusMem; }
	public Register tGetPC() { return PC; }
	public Register tGetIR() { return IR; }
	public Register tGetStkTOP() { return StkTOP; }
	public Register tGetStkBOT() { return StkBOT; }
	public Register tGetSP() { return SP; }
	public Register tGetFlags() { return Flags; }
	public Register tGetREG0() { return REG0; }
	public Register tGetREG1() { return REG1; }
	public Register tGetREG2() { return REG2; }
	public Register tGetREG3() { return REG3; }
	public Ula tGetUla() { return ula; }
	public Demux tGetDemux() { return demux; }
}
