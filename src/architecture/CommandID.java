package architecture;

public enum CommandID {
	ADD_REG_REG,  // add %<regA> %<regB>
	ADD_MEM_REG,  // add <mem> %<regA>
	ADD_REG_MEM,  // add %<regA> <mem>
	SUB_REG_REG,  // sub <regA> <regB>
	SUB_MEM_REG,  // sub <mem> %<regA>
	SUB_REG_MEM,  // sub %<regA> <mem>
	MOVE_MEM_REG, // move <mem> %<regA>
	MOVE_REG_MEM, // move %<regA> <mem>
	MOVE_REG_REG, // move %<regA> %<regB>
	MOVE_IMM_REG, // move imm %<regA>
	INC_REG,      // inc %<regA>
	INC_MEM,      // inc <mem>
	JMP,          // jmp <mem>
	JN,           // jn <mem>
	JZ,           // jz <mem>
	JNZ,          // jnz <mem>
	JEQ,          // jeq %<regA> %<regB> <mem>
	JGT,          // jgt %<regA> %<regB> <mem>
	JLW,          // jlw %<regA> %<regB> <mem>
	CALL,         // call <mem>
	RET;          // ret

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
