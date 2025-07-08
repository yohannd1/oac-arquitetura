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
	private Register SP;
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
	 *   store(regId): TODO
	 *   read(): TODO
	 *   internalStore(regId): TODO
	 *   internalRead(): TODO
	 */
	private Ula ula;

	private Demux demux;
	private ArrayList<String> commandsList;

	private void componentsInstances() {
		intBus = new Bus();
		extBus = new Bus();

		memory = new Memory(MAIN_MEMORY_SIZE, extBus);
		statusMem = new Memory(2, intBus); // TODO: fica conectada a intBus mesmo?

		PC = new Register("PC", intBus, intBus);
		IR = new Register("IR", intBus, intBus);
		SP = new Register("SP", intBus, intBus); // TODO: adicionar StkTOP e StkBOT (não SP)
		// SP.setData(MAIN_MEMORY_SIZE); // FIXME: não precisa inicializar SP aqui - o programa é responsável de fazer isso
		Flags = new Register(2, intBus);

		REG0 = new Register("REG0", intBus, intBus);
		REG1 = new Register("REG1", intBus, intBus);
		REG2 = new Register("REG2", intBus, intBus);
		REG3 = new Register("REG3", intBus, intBus);

		registerList = new Register[] { IR, REG0, REG1, REG2, REG3, PC, SP, Flags };

		ula = new Ula(intBus, intBus);

		demux = new Demux();
		fillCommandsList();
	}

	public Architecture() {
		componentsInstances();
		simulation = false;
	}

	public Architecture(boolean sim) {
		componentsInstances();
		simulation = sim;
	}

	protected void fillCommandsList() {
		commandsList = new ArrayList<String>();
		commandsList.add("add_rr");    // 0
		commandsList.add("add_mr");    // 1
		commandsList.add("add_rm");    // 2
		commandsList.add("sub_rr");    // 3
		commandsList.add("sub_mr");    // 4
		commandsList.add("sub_rm");    // 5
		commandsList.add("move_mr");   // 6
		commandsList.add("move_rm");   // 7
		commandsList.add("move_rr");   // 8
		commandsList.add("move_ir");   // 9
		commandsList.add("inc_r");     // 10
		commandsList.add("inc_m");     // 11
		commandsList.add("jmp");       // 12
		commandsList.add("jn");        // 13
		commandsList.add("jz");        // 14
		commandsList.add("jnz");       // 15
		commandsList.add("jeq");       // 16
		commandsList.add("jgt");       // 17
		commandsList.add("jlw");       // 18
		commandsList.add("call");      // 19
		commandsList.add("ret");       // 20
	}

	private void setStatusFlags(int result) {
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

	private void add_rr() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regB_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		demux.setValue(regA_id);
		registersRead();
		ula.store(0);

		demux.setValue(regB_id);
		registersRead();
		ula.store(1);

		ula.add();
		ula.read(1);
		setStatusFlags(intBus.get());

		demux.setValue(regB_id);
		registersStore();
	}

	private void add_mr() {
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
		ula.store(0);

		demux.setValue(regA_id);
		registersRead();
		ula.store(1);

		ula.add();
		ula.read(1);
		setStatusFlags(intBus.get());

		demux.setValue(regA_id);
		registersStore();
	}

	private void add_rm() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int mem_addr = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		demux.setValue(regA_id);
		registersRead();
		ula.store(0);

		intBus.put(mem_addr);
		extBus.put(intBus.get());
        memory.read();
		memory.read();
		intBus.put(extBus.get());
		ula.store(1);

		ula.add();
		ula.read(1);
		setStatusFlags(intBus.get());

		intBus.put(mem_addr);
		extBus.put(intBus.get());
		memory.store();

		ula.read(1);
		extBus.put(intBus.get());
		memory.store();
	}

	private void sub_rr() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regB_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		demux.setValue(regA_id);
		registersRead();
		ula.store(0);

		demux.setValue(regB_id);
		registersRead();
		ula.store(1);

		ula.sub();
		ula.read(1);
		setStatusFlags(intBus.get());

		demux.setValue(regB_id);
		registersStore();
	}

	private void sub_mr() {
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
		ula.store(0);

		demux.setValue(regA_id);
		registersRead();
		ula.store(1);

		ula.sub();
		ula.read(1);
		setStatusFlags(intBus.get());

		demux.setValue(regA_id);
		registersStore();
	}

	private void sub_rm() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int mem_addr = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		demux.setValue(regA_id);
		registersRead();
		ula.store(0);

		intBus.put(mem_addr);
		extBus.put(intBus.get());
        memory.read();
		memory.read();
		intBus.put(extBus.get());
		ula.store(1);

		ula.sub();
		ula.read(1);
		setStatusFlags(intBus.get());

		intBus.put(mem_addr);
		extBus.put(intBus.get());
		memory.store();

		ula.read(1);
		extBus.put(intBus.get());
		memory.store();
	}

	private void move_mr() {
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

	private void move_rm() {
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

	private void move_rr() {
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

	private void move_ir() {
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

	private void inc_r() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		demux.setValue(regA_id);
		registersRead();
		ula.store(0);
		ula.inc();
		ula.read(0);
		setStatusFlags(intBus.get());

		demux.setValue(regA_id);
		registersStore();
	}

	private void inc_m() {
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

	private void jmp() {
		PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
		int addr = intBus.get();

		intBus.put(addr);
		PC.store();
	}

	private void jn() {
		PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
		int addr = intBus.get();
		PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		if (Flags.getBit(1) == 1) {
			intBus.put(addr);
			PC.store();
		}
	}

	private void jz() {
		PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
		int addr = intBus.get();
		PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		if (Flags.getBit(0) == 1) {
			intBus.put(addr);
			PC.store();
		}
	}

	private void jnz() {
		PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
		int addr = intBus.get();
		PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		if (Flags.getBit(0) == 0) {
			intBus.put(addr);
			PC.store();
		}
	}

	private void jeq() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regB_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int mem_addr = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		demux.setValue(regA_id);
		registersRead();
		ula.store(0);

		demux.setValue(regB_id);
		registersRead();
		ula.store(1);

		ula.sub();
		ula.read(1);

		if (intBus.get() == 0) {
			intBus.put(mem_addr);
			PC.store();
		}
	}

	private void jgt() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regB_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int mem_addr = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		demux.setValue(regA_id);
		registersRead();
		ula.store(0);

		demux.setValue(regB_id);
		registersRead();
		ula.store(1);

		ula.sub();

		// FIXME: não podemos usar if aqui!
		// if (ula.getInternal(1) > 0) {
		// 	intBus.put(mem_addr);
		// 	PC.store();
		// }
	}

	private void jlw() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regB_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int mem_addr = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		demux.setValue(regA_id);
		registersRead();
		ula.store(0);

		demux.setValue(regB_id);
		registersRead();
		ula.store(1);

		ula.sub();

		// FIXME: não podemos usar if aqui!
		// if (ula.getInternal(1) < 0) {
		// 	intBus.put(mem_addr);
		// 	PC.store();
		// }
	}

	private void call() {
		PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
		int jumpAddress = intBus.get();

		int returnAddress = PC.getData();

		SP.read();
		ula.store(0);
		// ula.dec(); // FIXME: a ULA não tem dec. talvez ler um -1 da memória e somar?
		ula.read(0);
		SP.store();

		SP.read();
		intBus.put(SP.getData());
		extBus.put(intBus.get());
		memory.store();

		intBus.put(returnAddress);
		extBus.put(intBus.get());
		memory.store();

		intBus.put(jumpAddress);
		PC.store();
	}

	private void ret() {
		SP.read();
		intBus.put(SP.getData());
		extBus.put(intBus.get());
		memory.read();
		memory.read();
		intBus.put(extBus.get());
		PC.store();

		SP.read();
		ula.store(0);
		ula.inc();
		ula.read(0);
		SP.store();
	}

	public void readExec(String filename) throws IOException {
		   BufferedReader br = new BufferedReader(new FileReader(filename+".dxf"));
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

	public void controlUnitEexec() {
		halt = false;
		while (!halt) {
			fetch();
			if (!halt) {
				decodeExecute();
			}
		}
		System.out.println("--- EXECUTION HALTED ---");
	}

	private void decodeExecute() {
		IR.read();
		int command = intBus.get();

		if (simulation) simulationDecodeExecuteBefore(command);

		switch (command) {
			case 0: add_rr(); break;
			case 1: add_mr(); break;
			case 2: add_rm(); break;
			case 3: sub_rr(); break;
			case 4: sub_mr(); break;
			case 5: sub_rm(); break;
			case 6: move_mr(); break;
			case 7: move_rm(); break;
			case 8: move_rr(); break;
			case 9: move_ir(); break;
			case 10: inc_r(); break;
			case 11: inc_m(); break;
			case 12: jmp(); break;
			case 13: jn(); break;
			case 14: jz(); break;
			case 15: jnz(); break;
			case 16: jeq(); break;
			case 17: jgt(); break;
			case 18: jlw(); break;
			case 19: call(); break;
			case 20: ret(); break;
			default:
				halt = true;
				break;
		}

		if (simulation) simulationDecodeExecuteAfter();
	}

	private void fetch() {
		PC.read();
		if (PC.getData() >= MAIN_MEMORY_SIZE) {
			halt = true; return;
		}
		extBus.put(intBus.get());
		memory.read();
		memory.read();
		intBus.put(extBus.get());
		IR.store();

		PC.read();
		ula.store(0);
		ula.inc();
		ula.read(0);
		PC.store();

		if (simulation) simulationFetch();
	}

	private boolean hasOperands(String instruction) {
		if ("inc".equals(instruction))
			return false;
		else
			return true;
	}

	private void simulationDecodeExecuteBefore(int command) {
		System.out.println("----------BEFORE Decode and Execute phases--------------");
		String instruction = (command >= 0 && command < commandsList.size()) ? commandsList.get(command) : "HALT";

		System.out.println("PC: " + PC.getData() + " | IR: " + IR.getData() + " ("+instruction+") | SP: " + SP.getData());
		System.out.print("REGISTERS: ");
		for (Register r : registerList) {
			System.out.print(r.getRegisterName() + ": " + r.getData() + " | ");
		}
		System.out.println("FLAGS (Z,N): " + Flags.getBit(0) + "," + Flags.getBit(1));
		System.out.println("Instruction: "+instruction);
	}

	private void simulationDecodeExecuteAfter() {
		System.out.println("-----------AFTER Decode and Execute phases--------------");
		System.out.println("Internal Bus: " + intBus.get());
		System.out.println("External Bus: " + extBus.get());
		System.out.println("PC: " + PC.getData() + " | SP: " + SP.getData());
		System.out.print("REGISTERS: ");
		for (Register r : registerList) {
			System.out.print(r.getRegisterName() + ": " + r.getData() + " | ");
		}
		System.out.println("FLAGS (Z,N): " + Flags.getBit(0) + "," + Flags.getBit(1));

		Scanner entrada = new Scanner(System.in);
		System.out.println("Press <Enter> to continue...");
		entrada.nextLine();
	}

	private void simulationFetch() {
		if (simulation) {
			System.out.println("------------------------------------------------------");
			System.out.println("-------Fetch Phase------");
			System.out.println("PC was pointing to: " + (PC.getData() -1) );
			System.out.println("IR loaded with opcode: " + IR.getData());
		}
	}

	public int getMemorySize() {
		return MAIN_MEMORY_SIZE;
	}

	public static void main(String[] args) throws IOException {
		Architecture arch = new Architecture(true);
		arch.readExec("program");
		arch.controlUnitEexec();
	}
}
