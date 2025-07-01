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
	private boolean simulation;
	private boolean halt;

	private Bus intBus;
	private Bus extBus;

	private Memory memory;
	private int memorySize;

	private Register PC;
	private Register IR;
	private Register SP;
	private Register Flags;

	private Register REG0;
	private Register REG1;
	private Register REG2;
	private Register REG3;

	public Register[] registerList;

	private Ula ula;
	private Demux demux;

	private void componentsInstances() {
		intBus = new Bus();
		extBus = new Bus();

		memorySize = 256;
		memory = new Memory(memorySize, extBus);

		PC = new Register("PC", intBus, intBus);
		IR = new Register("IR", intBus, intBus);
		SP = new Register("SP", intBus, intBus); // TODO: adicionar StkTOP e StkBOT (não SP)
		// SP.setData(memorySize); // FIXME: não precisa inicializar SP aqui - o programa é responsável de fazer isso
		Flags = new Register("Flags", 2, intBus);

		REG0 = new Register("REG0", intBus, intBus);
		REG1 = new Register("REG1", intBus, intBus);
		REG2 = new Register("REG2", intBus, intBus);
		REG3 = new Register("REG3", intBus, intBus);

		registerList = new Register[] { IR, REG0, REG1, REG2, REG3, PC, SP };

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

	private void setStatusFlags(int result) {
		Flags.setBit(0, 0);
		Flags.setBit(1, 0);
		if (result == 0) Flags.setBit(0, 1);
		if (result < 0) Flags.setBit(1, 1);
	}

	/**
	 * Select a register from the demux.
	 */
	private Register selectRegister() {
		return registerList[demux.getValue()];
	}

	private void demuxRegisterRead() {
		selectRegister().read();
	}

	private void demuxRegisterStore() {
		selectRegister().store();
	}

	private void add_rr() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regB_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		demux.setValue(regA_id);
		demuxRegisterRead();
		ula.store(0);

		demux.setValue(regB_id);
		demuxRegisterRead();
		ula.store(1);

		ula.add();
		ula.read(1);
		setStatusFlags(intBus.get());

		demux.setValue(regB_id);
		demuxRegisterStore();
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
		demuxRegisterRead();
		ula.store(1);

		ula.add();
		ula.read(1);
		setStatusFlags(intBus.get());

		demux.setValue(regA_id);
		demuxRegisterStore();
	}

	private void add_rm() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int mem_addr = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		demux.setValue(regA_id);
		demuxRegisterRead();
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
		demuxRegisterRead();
		ula.store(0);

		demux.setValue(regB_id);
		demuxRegisterRead();
		ula.store(1);

		ula.sub();
		ula.read(1);
		setStatusFlags(intBus.get());

		demux.setValue(regB_id);
		demuxRegisterStore();
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
		demuxRegisterRead();
		ula.store(1);

		ula.sub();
		ula.read(1);
		setStatusFlags(intBus.get());

		demux.setValue(regA_id);
		demuxRegisterStore();
	}

	private void sub_rm() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int mem_addr = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		demux.setValue(regA_id);
		demuxRegisterRead();
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
		demuxRegisterStore();
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
		demuxRegisterRead();
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
		demuxRegisterRead();

		demux.setValue(regB_id);
		demuxRegisterStore();
	}

	/**
	 * Sub-microprogram for pc++
	 */
	private void submicr_pcInc() {
        PC.internalRead(); // pc->intBus
		ula.internalStore(1); // ula(1)<-intBus
		ula.inc(); // ula(1)++
		ula.internalRead(1); // ula(1)->intBus
		PC.internalStore(); // pc<-intBus
	}

	private void move_ir() {
		submicr_pcInc(); // pc++

		// read immediate value
        PC.read(); // pc->intBus
		extBus.put(intBus.get()); // intBus->extBus
		memory.read(); // mem(r)<-extBus, extBus<-mem[pc]
		intBus.put(extBus.get()); // extBus->intBus
		IR.store();

		submicr_pcInc(); // pc++

		// read register ID
        PC.read(); // pc->intBus
		extBus.put(intBus.get()); // intBus->extBus
		memory.read(); // mem(r)<-extBus, extBus<-mem[pc]
		intBus.put(extBus.get()); // extBus->intBus

		demux.setValue(intBus.get()); // TODO: make it read from the bus directly lol

		// store immediate value into the register
		IR.read(); // ir->intBus (ir is storing the immediate value)
		demuxRegisterStore(); // regXX<-intBus

		submicr_pcInc(); // pc++
	}

	private void inc_r() {
        PC.read(); extBus.put(intBus.get()); memory.read(); memory.read(); intBus.put(extBus.get());
        int regA_id = intBus.get();
        PC.read(); ula.store(0); ula.inc(); ula.read(0); PC.store();

		demux.setValue(regA_id);
		demuxRegisterRead();
		ula.store(0);
		ula.inc();
		ula.read(0);
		setStatusFlags(intBus.get());

		demux.setValue(regA_id);
		demuxRegisterStore();
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
		demuxRegisterRead();
		ula.store(0);

		demux.setValue(regB_id);
		demuxRegisterRead();
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
		demuxRegisterRead();
		ula.store(0);

		demux.setValue(regB_id);
		demuxRegisterRead();
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
		demuxRegisterRead();
		ula.store(0);

		demux.setValue(regB_id);
		demuxRegisterRead();
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
		BufferedReader br = new BufferedReader(new FileReader(filename + ".dxf"));
		String linha;
		int i = 0;
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

		// if (simulation) {
		// 	System.out.println("MEMORY BEFORE PROGRAM BEGINS");
		// 	memory.debugPrint();
		// }

		while (true) {
			if (halt) break;
			fetch();
			if (halt) break;
			decodeExecute();
		}
		System.out.println("--- EXECUTION HALTED ---");
	}

	private void decodeExecute() {
		IR.read();
		int command = intBus.get();

		if (simulation) {
			System.out.println("*** BEFORE Decode & Execute ***");
			simulationPrintAllRegisters();
		}

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

		if (simulation) {
			System.out.println("*** AFTER Decode & Execute ***");
			System.out.println("Internal Bus: " + intBus.get());
			System.out.println("External Bus: " + extBus.get());
			simulationPrintAllRegisters();
			waitForEnter();
		}
	}

	private void fetch() {
		if (simulation)
			System.out.printf("[Fetch begin] PC = %d\n", PC.getData());

		// halt if PC is invalid
		if (PC.getData() >= memorySize) {
			halt = true;
			return;
		}

		PC.read(); // pc->intBus
		extBus.put(intBus.get()); // intBus->extBus (FIXME: acho que não pode fazer isso)
		memory.read(); // mem(r)<-extBus, mem->extBus
		intBus.put(extBus.get()); // intBus<-extBus (FIXME: acho que não pode fazer isso)
		IR.store(); // ir<-intBus

		// pc++
		PC.read();
		ula.store(0);
		ula.inc();
		ula.read(0);
		PC.store();

		if (simulation)
			simulationFetch();
	}

	private boolean hasOperands(String instruction) {
		if ("inc".equals(instruction))
			return false;
		else
			return true;
	}

	private void simulationDecodeExecuteBefore() {
		System.out.println("----------BEFORE Decode and Execute phases--------------");
		simulationPrintAllRegisters();
	}

	private void simulationPrintAllRegisters() {
		CommandID commandId = CommandID.fromInt(IR.getData());
		String instName = (commandId == null) ? "invalid" : commandId.toString();

		System.out.printf("PC: %d | IR: %d (%s) | SP: %d\n", PC.getData(), IR.getData(), instName, SP.getData());
		System.out.print("REGISTERS: ");
		for (Register r : registerList)
			System.out.printf("%s: %d | ", r.getRegisterName(), r.getData());
		System.out.println("FLAGS (Z,N): " + Flags.getBit(0) + "," + Flags.getBit(1));
	}

	private void waitForEnter() {
		Scanner input = new Scanner(System.in);
		System.out.println("Press <Enter> to continue...");
		input.nextLine();
	}

	private void simulationFetch() {
		if (simulation) {
			System.out.println("------------------------------------------------------");
			System.out.println("-------After fetch phase------");
			System.out.printf("PC = %d | IR = %d\n", PC.getData(), IR.getData());
		}
	}

	public int getMemorySize() {
		return memorySize;
	}
}
