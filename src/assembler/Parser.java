package assembler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import architecture.CommandID;

public class Parser {
	static private Pattern VARIABLE_PATT = Pattern.compile("^\\s*([a-zA-Z][a-zA-Z0-9]*)\\s*$");
	static private Pattern LABEL_PATT = Pattern.compile("^\\s*([a-zA-Z][a-zA-Z0-9]*)\\s*:\\s*$");

	/**
	 * Attempt to parse a variable declaration.
	 *
	 * @return the variable name, or null if it's not a variable declaration
	 */
	static protected String parseVariableDecl(String s) {
		Matcher m = VARIABLE_PATT.matcher(s);
		if (m.find()) {
			return m.group(0);
		} else {
			return null;
		}
	}

	/**
	 * Attempt to parse a label declaration.
	 *
	 * @return the label name, or null if it's not a label declaration
	 */
	static protected String parseLabelDecl(String s) {
		Matcher m = LABEL_PATT.matcher(s);
		return m.find() ? m.group(0) : null;
	}

	static protected boolean isSkippableLine(String line) {
		return line.length() == 0 || line.charAt(0) == ';';
	}

	static protected Command parseCommand(String[] tokens)  {
		if (tokens.length == 0)
			return null;

		String commandName = tokens[0];
		int paramCount = tokens.length - 1;

		if (commandName.equals("add") && paramCount == 2) {
			// TODO: ADD_REG_REG, // add %<regA> %<regB>
			// TODO: ADD_MEM_REG, // add <mem> %<regA>
			// TODO: ADD_REG_MEM, // add %<regA> <mem>
			return new Command(CommandID.ADD_REG_REG, new String[] {});
		} else if (commandName.equals("sub") && paramCount == 2) {
			// TODO: SUB_REG_REG, // sub <regA> <regB>
			// TODO: SUB_MEM_REG, // sub <mem> %<regA>
			// TODO: SUB_REG_MEM, // sub %<regA> <mem>
			return new Command(CommandID.ADD_REG_REG, new String[] {});
		} else if (commandName.equals("move") && paramCount == 2) {
			// TODO: MOVE_MEM_REG, // move <mem> %<regA>
			// TODO: MOVE_REG_MEM, // move %<regA> <mem>
			// TODO: MOVE_REG_REG, // move %<regA> %<regB>
			// TODO: MOVE_IMM_REG, // move imm %<regA>
			return new Command(CommandID.ADD_REG_REG, new String[] {});
		} else if (commandName.equals("inc") && paramCount == 1) {
			// TODO: INC_REG, // inc %<regA>
			// TODO: INC_MEM, // inc <mem>
			return new Command(CommandID.ADD_REG_REG, new String[] {});
		} else if (commandName.equals("jmp") && paramCount == 1) {
			// TODO: JMP, // jmp <mem>
			return new Command(CommandID.ADD_REG_REG, new String[] {});
		} else if (commandName.equals("jn") && paramCount == 1) {
			// TODO: JN, // jn <mem>
			return new Command(CommandID.ADD_REG_REG, new String[] {});
		} else if (commandName.equals("jz") && paramCount == 1) {
			// TODO: JZ, // jz <mem>
			return new Command(CommandID.ADD_REG_REG, new String[] {});
		} else if (commandName.equals("jnz") && paramCount == 1) {
			// TODO: JNZ, // jnz <mem>
			return new Command(CommandID.ADD_REG_REG, new String[] {});
		} else if (commandName.equals("jeq") && paramCount == 3) {
			// TODO: JEQ, // jeq %<regA> %<regB> <mem>
			return new Command(CommandID.ADD_REG_REG, new String[] {});
		} else if (commandName.equals("jgt") && paramCount == 3) {
			// TODO: JGT, // jgt %<regA> %<regB> <mem>
			return new Command(CommandID.ADD_REG_REG, new String[] {});
		} else if (commandName.equals("jlw") && paramCount == 3) {
			// TODO: JLW, // jlw %<regA> %<regB> <mem>
			return new Command(CommandID.ADD_REG_REG, new String[] {});
		} else if (commandName.equals("call") && paramCount == 1) {
			// TODO: CALL, // call <mem>
			return new Command(CommandID.ADD_REG_REG, new String[] {});
		} else if (commandName.equals("ret") && paramCount == 0) {
			// TODO: RET; // ret
			return new Command(CommandID.ADD_REG_REG, new String[] {});
		} else {
			return null;
		}
	}
}
