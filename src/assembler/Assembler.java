package assembler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import components.Register;
import architecture.Architecture;
import architecture.CommandID;

public class Assembler {
	private ArrayList<String> lines;
	private ArrayList<String> objProgram;
	private ArrayList<String> execProgram;
	private ArrayList<String> labels;
	private ArrayList<Integer> labelsAdresses;
	private ArrayList<String> variables;
	private Architecture arch;

	public Assembler() {
		lines = new ArrayList<>();
		labels = new ArrayList<>();
		labelsAdresses = new ArrayList<>();
		variables = new ArrayList<>();
		objProgram = new ArrayList<>();
		execProgram = new ArrayList<>();
		arch = new Architecture();
	}

	public ArrayList<String> getObjProgram() {
		return objProgram;
	}

	/**
	 * These methods getters and set below are used only for TDD purposes
	 *
	 * @param lines
	 */
	protected ArrayList<String> getLabels() {
		return labels;
	}

	protected ArrayList<Integer> getLabelsAddresses() {
		return labelsAdresses;
	}

	protected ArrayList<String> getVariables() {
		return variables;
	}

	protected ArrayList<String> getExecProgram() {
		return execProgram;
	}

	protected void setLines(ArrayList<String> lines) {
		this.lines = lines;
	}

	protected void setExecProgram(ArrayList<String> lines) {
		this.execProgram = lines;
	}

	/**
	 * Read the lines of a file into the assembler.
	 *
	 * @param filename
	 * @throws IOException
	 */
	public void read(String filename) throws IOException {
		FileReader fr = new FileReader(filename + ".dsf");
		BufferedReader br = new BufferedReader(fr);

		while (true) {
			String line = br.readLine();
			if (line == null)
				break;
			lines.add(line);
		}

		br.close();
	}

	/**
	 * Scan the lines from the loaded file, attributing meaning to each line.
	 *
	 * @param lines
	 */
	public void parseAll() throws ParseException {
		int i = 0;

		// parse variable declarations first
		while (i < lines.size()) {
			String currentLine = lines.get(i).trim();
			String varName;

			if (Parser.isSkippableLine(currentLine)) {
				// skip empty line
				i++;
			} else if ((varName = Parser.parseVariableDecl(currentLine)) != null) {
				System.err.println("Got VARIABLE " + varName);
				// this line is a variable declaration
				variables.add(varName);
				i++;
			} else {
				break;
			}
		}

		// parse the rest
		while (i < lines.size()) {
			String currentLine = lines.get(i).trim();
			String labelName;
			Command command;

			if (Parser.isSkippableLine(currentLine)) {
				// skip empty line
				i++;
			} else if ((labelName = Parser.parseLabelDecl(currentLine)) != null) {
				// this line is a label declaration
				System.err.println("Got LABEL " + labelName + " at addr " + objProgram.size());
				labels.add(labelName);
				labelsAdresses.add(objProgram.size());
				i++;
			} else if ((command = Parser.parseCommand(currentLine.split(" "))) != null) {
				// this line is a command
				System.err.println("Got " + command);

				objProgram.add(Integer.toString(command.id.toInt()));
				for (String arg : command.args) {
					if (!arg.isEmpty())
						objProgram.add(arg);
				}

				i++;
			} else {
				throw new ParseException("Could not parse line " + (i + 1) + ": " + currentLine);
			}
		}
	}

	/**
	 * Create the executable program from the object program.
	 *
	 * @param filename the file where the executable will be put over
	 * @throws IOException
	 */
	public void makeExecutable(String filename) throws IOException {
		if (!checkProperDeclaration())
			return;

		// allocate memory space to store program and variables,
		// and copy the object program data over there
		execProgram = new ArrayList<>();
		for (String s : objProgram)
			execProgram.add(s);

		replaceAllVariables();
		replaceLabels();
		replaceRegisters();

		// save to file
		saveExecFile(filename);
	}

	/**
	 * Replace all the register names in the executable program with its
	 * corresponding IDs.
	 */
	protected void replaceRegisters() {
		int p = 0;
		for (String line : execProgram) {
			// A % on the start of the line indicates a register name
			if (line.startsWith("%")) {
				int regId = searchRegisterId(line.substring(1, line.length()));
				String newLine = Integer.toString(regId);
				execProgram.set(p, newLine);
			}
			p++;
		}
	}

	/**
	 * Replace all variables names by their respective addresses.
	 *
	 * The address of the first variable is at the end of the memory and
	 * successive variables are on the addresses immediately before.
	 */
	protected void replaceAllVariables() {
		int position = arch.getMemorySize() - 1; // starting from the end of the memory
		for (String var : this.variables) { // scanning all variables
			replaceVariable(var, position);
			position--;
		}
	}

	/**
	 * Save the execFile collection into the output file.
	 *
	 * @param filename
	 * @throws IOException
	 */
	private void saveExecFile(String filename) throws IOException {
		File file = new File(filename + ".dxf");
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		for (String l : execProgram)
			writer.write(l + "\n");
		writer.write("-1"); //-1 is a flag indicating that the program is finished
		writer.close();
	}

	/**
	 * Replace each label in the executable program by the corresponding
	 * address it refers to.
	 */
	protected void replaceLabels() {
		int i = 0;
		for (String label : labels) { // searching all labels
			label = "&" + label;
			int labelPointTo = labelsAdresses.get(i);
			int lineNumber = 0;
			for (String l : execProgram) {
				if (l.equals(label)) { // this label must be replaced by the address
					String newLine = Integer.toString(labelPointTo); // the address
					execProgram.set(lineNumber, newLine);
				}
				lineNumber++;
			}
			i++;
		}
	}

	/**
	 * Replace all ocurrences of a variable name found in the program by its
	 * address in the executable program.
	 *
	 * @param var
	 * @param position
	 */
	protected void replaceVariable(String var, int position) {
		var = "&" + var;
		int i = 0;
		for (String s : execProgram) {
			if (s.equals(var))
				execProgram.set(i, Integer.toString(position));
			i++;
		}
	}

	/**
	 * Check if all labels and variables in the object program were in the
	 * source program.
	 *
	 * The `labels` and `variables` collections are used for this.
	 */
	protected boolean checkProperDeclaration() {
		System.err.println("Checking labels and variables");
		for (String line : objProgram) {
			boolean found = false;
			if (line.startsWith("&")) { // if starts with "&", it is a label or a variable
				line = line.substring(1, line.length());
				if (labels.contains(line))
					found = true;
				if (variables.contains(line))
					found = true;
				if (!found) {
					System.err.printf("FATAL ERROR! Variable or label %s not declared!\n", line);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Search for a register in the architecture's register list by its name.
	 *
	 * @return register id (>= 0) on success, -1 on failure
	 */
	private int searchRegisterId(String line) {
		int i = 0;
		for (Register r : arch.registerList) {
			if (line.equals(r.getRegisterName()))
				return i;
			i++;
		}
		return -1;
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Usage: assembler <INPUT>");
			System.err.println("INPUT must be the name of a .dsf file, without the extension");
			System.exit(2);
		}

		String filename = args[0];

		Assembler assembler = new Assembler();

		try {
			System.err.printf("Reading source assembler file: %s.dsf\n", filename);
			assembler.read(filename);

			System.err.println("Generating the object program");
			assembler.parseAll();

			System.err.printf("Generating executable: %s.dxf\n", filename);
			assembler.makeExecutable(filename);

			System.err.println("Assembling finished!");
		} catch (ParseException ex) {
			System.err.println("Error while parsing: " + ex);
		}
	}

	/**
	 * Parsing submodule with most of the parsing logic.
	 */
	private static class Parser {
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
				String[] args = new String[] { arg0, arg1 };

				if (isRegName(arg0) && isRegName(arg1))
					return new Command(CommandID.ADD_REG_REG, args);
				else if (isMemName(arg0) && isRegName(arg1))
					return new Command(CommandID.ADD_MEM_REG, args);
				else if (isRegName(arg0) && isMemName(arg1))
					return new Command(CommandID.ADD_REG_MEM, args);
				else
					return null;
			} else if (commandName.equals("sub") && paramCount == 2) {
				String arg0 = tokens[1];
				String arg1 = tokens[2];
				String[] args = new String[] { arg0, arg1 };

				if (isRegName(arg0) && isRegName(arg1))
					return new Command(CommandID.SUB_REG_REG, args);
				else if (isMemName(arg0) && isRegName(arg1))
					return new Command(CommandID.SUB_MEM_REG, args);
				else if (isRegName(arg0) && isMemName(arg1))
					return new Command(CommandID.SUB_REG_MEM, args);
				else
					return null;
			} else if (commandName.equals("move") && paramCount == 2) {
				String arg0 = tokens[1];
				String arg1 = tokens[2];
				String[] args = new String[] { arg0, arg1 };

				if (isMemName(arg0) && isRegName(arg1))
					return new Command(CommandID.MOVE_MEM_REG, args);
				else if (isRegName(arg0) && isMemName(arg1))
					return new Command(CommandID.MOVE_REG_MEM, args);
				else if (isRegName(arg0) && isRegName(arg1))
					return new Command(CommandID.MOVE_REG_REG, args);
				else if (isNumber(arg0) && isRegName(arg1))
					return new Command(CommandID.MOVE_IMM_REG, args);
				else
					return null;
			} else if (commandName.equals("inc") && paramCount == 1) {
				String arg = tokens[1];
				String[] args = new String[] { arg };

				if (isRegName(arg))
					return new Command(CommandID.INC_REG, args);
				else if (isMemName(arg))
					return new Command(CommandID.INC_MEM, args);
				else
					return null;
			} else if (commandName.equals("jmp") && paramCount == 1) {
				String arg = tokens[1];
				return isMemName(arg) ? new Command(CommandID.JMP, new String[] { arg }) : null;
			} else if (commandName.equals("jn") && paramCount == 1) {
				String arg = tokens[1];
				return isMemName(arg) ? new Command(CommandID.JN, new String[] { arg }) : null;
			} else if (commandName.equals("jz") && paramCount == 1) {
				String arg = tokens[1];
				return isMemName(arg) ? new Command(CommandID.JZ, new String[] { arg }) : null;
			} else if (commandName.equals("jnz") && paramCount == 1) {
				String arg = tokens[1];
				return isMemName(arg) ? new Command(CommandID.JZ, new String[] { arg }) : null;
			} else if (commandName.equals("jeq") && paramCount == 3) {
				String[] args = new String[] { tokens[1], tokens[2], tokens[3] };
				return (isRegName(tokens[1]) && isRegName(tokens[2]) && isMemName(tokens[3]))
					? new Command(CommandID.JEQ, args)
					: null;
			} else if (commandName.equals("jgt") && paramCount == 3) {
				String[] args = new String[] { tokens[1], tokens[2], tokens[3] };
				return (isRegName(tokens[1]) && isRegName(tokens[2]) && isMemName(tokens[3]))
					? new Command(CommandID.JGT, args)
					: null;
			} else if (commandName.equals("jlw") && paramCount == 3) {
				String[] args = new String[] { tokens[1], tokens[2], tokens[3] };
				return (isRegName(tokens[1]) && isRegName(tokens[2]) && isMemName(tokens[3]))
					? new Command(CommandID.JLW, args)
					: null;
			} else if (commandName.equals("call") && paramCount == 1) {
				String[] args = new String[] { tokens[1] };
				return (isMemName(tokens[1])) ? new Command(CommandID.CALL, args) : null;
			} else if (commandName.equals("ret") && paramCount == 0) {
				return new Command(CommandID.RET, new String[] {});
			} else {
				return null;
			}
		}
	}

	/**
	 * Error thrown when an unrecoverable parsing error is reached.
	 */
	private static class ParseException extends Exception {
		public ParseException(String msg) {
			super(msg);
		}
	}

	/**
	 * Data class used for representing commands.
	 */
	private static class Command {
		CommandID id;
		String[] args;

		public Command(CommandID id, String[] args) {
			this.id = id;
			this.args = args;
		}

		@Override
		public String toString() {
			return String.format("Command[id=%s, args=%s]", id, args);
		}
	}
}
