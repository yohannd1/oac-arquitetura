package assembler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import architecture.CommandID;

public class Parser {
	static private Pattern VARIABLE_PATT = Pattern.compile("^\\s*([a-zA-Z][a-zA-Z0-9]*)\\s*$");
	static private Pattern LABEL_PATT = Pattern.compile("^\\s*([a-zA-Z][a-zA-Z0-9]*)\\s*:\\s*$");
	static private Pattern NUMBER_PATT = Pattern.compile("^[0-9]+$");
	static private Pattern REG_PATT = Pattern.compile("^%reg[0-9]+$");

	/**
	 * Attempt to parse a variable declaration.
	 *
	 * @return the variable name, or null if it's not a variable declaration
	 */
	static protected String parseVariableDecl(String s) {
		Matcher m = VARIABLE_PATT.matcher(s);
		return m.find() ? m.group(0) : null;
	}

	static protected boolean isMemName(String s) {
		return parseVariableDecl(s) != null;
	}

	static protected boolean isNumber(String s) {
		return NUMBER_PATT.matcher(s).find();
	}

	static protected boolean isRegName(String s) {
		return REG_PATT.matcher(s).find();
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
			String arg0 = tokens[1];
			String arg1 = tokens[2];

			if (isRegName(arg0) && isRegName(arg1))
				return new Command(CommandID.ADD_REG_REG, new String[] { arg0, arg1 });
			else if (isMemName(arg0) && isRegName(arg1))
				return new Command(CommandID.ADD_MEM_REG, new String[] { "&" + arg0, arg1 });
			else if (isRegName(arg0) && isMemName(arg1))
				return new Command(CommandID.ADD_REG_MEM, new String[] { arg0, "&" + arg1 });
			else
				return null;
		} else if (commandName.equals("sub") && paramCount == 2) {
			String arg0 = tokens[1];
			String arg1 = tokens[2];

			if (isRegName(arg0) && isRegName(arg1))
				return new Command(CommandID.SUB_REG_REG, new String[] { arg0, arg1 });
			else if (isMemName(arg0) && isRegName(arg1))
				return new Command(CommandID.SUB_MEM_REG, new String[] { "&" + arg0, arg1 });
			else if (isRegName(arg0) && isMemName(arg1))
				return new Command(CommandID.SUB_REG_MEM, new String[] { arg0, "&" + arg1 });
			else
				return null;
		} else if (commandName.equals("move") && paramCount == 2) {
			String arg0 = tokens[1];
			String arg1 = tokens[2];

			if (isMemName(arg0) && isRegName(arg1))
				return new Command(CommandID.MOVE_MEM_REG, new String[] { "&" + arg0, arg1 });
			else if (isRegName(arg0) && isMemName(arg1))
				return new Command(CommandID.MOVE_REG_MEM, new String[] { arg0, "&" + arg1 });
			else if (isRegName(arg0) && isRegName(arg1))
				return new Command(CommandID.MOVE_REG_REG, new String[] { arg0, arg1 });
			else if (isNumber(arg0) && isRegName(arg1))
				return new Command(CommandID.MOVE_IMM_REG, new String[] { arg0, arg1 });
			else
				return null;
		} else if (commandName.equals("inc") && paramCount == 1) {
			String arg = tokens[1];

			if (isRegName(arg))
				return new Command(CommandID.INC_REG, new String[] { arg });
			else if (isMemName(arg))
				return new Command(CommandID.INC_MEM, new String[] { "&" + arg });
			else
				return null;
		} else if (commandName.equals("jmp") && paramCount == 1) {
			String arg = tokens[1];
			return isMemName(arg) ? new Command(CommandID.JMP, new String[] { "&" + arg }) : null;
		} else if (commandName.equals("jn") && paramCount == 1) {
			String arg = tokens[1];
			return isMemName(arg) ? new Command(CommandID.JN, new String[] { "&" + arg }) : null;
		} else if (commandName.equals("jz") && paramCount == 1) {
			String arg = tokens[1];
			return isMemName(arg) ? new Command(CommandID.JZ, new String[] { "&" + arg }) : null;
		} else if (commandName.equals("jnz") && paramCount == 1) {
			String arg = tokens[1];
			return isMemName(arg) ? new Command(CommandID.JZ, new String[] { "&" + arg }) : null;
		} else if (commandName.equals("jeq") && paramCount == 3) {
			return (isRegName(tokens[1]) && isRegName(tokens[2]) && isMemName(tokens[3]))
				? new Command(CommandID.JEQ, new String[] { tokens[1], tokens[2], "&" + tokens[3] })
				: null;
		} else if (commandName.equals("jgt") && paramCount == 3) {
			return (isRegName(tokens[1]) && isRegName(tokens[2]) && isMemName(tokens[3]))
				? new Command(CommandID.JGT, new String[] { tokens[1], tokens[2], "&" + tokens[3] })
				: null;
		} else if (commandName.equals("jlw") && paramCount == 3) {
			return (isRegName(tokens[1]) && isRegName(tokens[2]) && isMemName(tokens[3]))
				? new Command(CommandID.JLW, new String[] { tokens[1], tokens[2], "&" + tokens[3] })
				: null;
		} else if (commandName.equals("call") && paramCount == 1) {
			return (isMemName(tokens[1]))
				? new Command(CommandID.CALL, new String[] { "&" + tokens[1] })
				: null;
		} else if (commandName.equals("ret") && paramCount == 0) {
			return new Command(CommandID.RET, new String[] {});
		} else {
			return null;
		}
	}
}
