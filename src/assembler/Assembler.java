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
import architecture.Architecture.CommandID;

public class Assembler {
	private ArrayList<String> lines;
	private ArrayList<String> objProgram;
	private ArrayList<String> execProgram;
	private ArrayList<String> labels;
	private ArrayList<Integer> labelsAdresses;
	private ArrayList<String> variables;
	private Architecture arch;
	int programOffset;

	public Assembler() {
		lines = new ArrayList<>();
		labels = new ArrayList<>();
		labelsAdresses = new ArrayList<>();
		variables = new ArrayList<>();
		objProgram = new ArrayList<>();
		execProgram = new ArrayList<>();
		arch = new Architecture();
		programOffset = 0;
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
	 * Read the lines of a String array into the assembler.
	 */
	public void readLines(String[] lines_) {
		for (String line : lines_)
			lines.add(line);
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
				labels.add(labelName);
				labelsAdresses.add(objProgram.size());
				i++;
			} else if ((command = Parser.parseCommand(currentLine.split(" "))) != null) {
				// this line is a command
				objProgram.add(Integer.toString(command.id.toInt()));
				for (String arg : command.args) {
					if (!arg.isEmpty())
						objProgram.add(arg);
				}

				i++;
			} else {
				throw new ParseException("could not parse line " + (i + 1) + ": " + currentLine);
			}
		}
	}

	public void buildExecutable() {
		checkProperDeclaration();

		// allocate memory space to store program and variables
		execProgram = new ArrayList<>();

		// add the initial stack pointer initialization
		int stackBottom = arch.getMemorySize() - variables.size();
		for (String rn : new String[] { "StkBOT", "StkTOP" }) {
			int rid = arch.getRegisterID(rn);
			if (rid < 0)
				throw new RuntimeException(String.format("could not find register with name '%s'\n", rn));

			execProgram.add(Integer.toString(CommandID.MOVE_IMM_REG.toInt()));
			execProgram.add(Integer.toString(stackBottom));
			execProgram.add(Integer.toString(rid));
		}

		// update the program offset
		programOffset = execProgram.size();

		// copy the object program data over to the executable
		for (String s : objProgram)
			execProgram.add(s);

		replaceAllVariables();
		replaceLabels();
		replaceRegisters();

		// add halt instruction
		execProgram.add(Integer.toString(-1));
	}

	/**
	 * Create the executable program from the object program.
	 *
	 * @param filename the file where the executable will be put over
	 * @throws IOException
	 */
	public void makeExecutable(String filename) throws IOException {
		buildExecutable();
		saveExecFile(filename);
	}

	/**
	 * Create the executable program from the object program, and return its lines.
	 */
	public String[] makeExecutableLines() {
		buildExecutable();
		String[] ret = new String[execProgram.size()];
		for (int i = 0; i < execProgram.size(); i++)
			ret[i] = execProgram.get(i);
		return ret;
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
				String regName = line.substring(1, line.length());
				int regID = arch.getRegisterID(regName);
				if (regID < 0)
					throw new RuntimeException("could not find register with name " + regName);
				String newLine = Integer.toString(regID);
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
		for (String varName : variables) { // scanning all variables
			replaceVariable(varName, position);
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
			int labelAddress = labelsAdresses.get(i);
			for (int j = 0; j < execProgram.size(); j++) {
				if (execProgram.get(j).equals(label)) { // this label must be replaced by the address
					execProgram.set(j, Integer.toString(programOffset + labelAddress));
				}
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
	protected void checkProperDeclaration() {
		for (String line : objProgram) {
			boolean found = false;
			if (line.startsWith("&")) { // if starts with "&", it is a label or a variable
				line = line.substring(1, line.length());
				if (!labels.contains(line) && !variables.contains(line))
					throw new RuntimeException(String.format("variable or label '%s' not declared!\n", line));
			}
		}
	}

	/**
	 * Parsing submodule with most of the parsing logic.
	 */
	private static class Parser {
		static private Pattern VARIABLE_PATT = Pattern.compile("^\\s*([a-zA-Z][a-zA-Z0-9]*)\\s*$");
		static private Pattern LABEL_PATT = Pattern.compile("^\\s*([a-zA-Z][a-zA-Z0-9]*)\\s*:\\s*$");
		static private Pattern NUMBER_PATT = Pattern.compile("^[-+]?[0-9]+$");
		static private Pattern REG_PATT = Pattern.compile("^%reg[0-9]+$");

		/**
		 * Attempt to parse a variable declaration.
		 *
		 * @return the variable name, or null if it's not a variable declaration
		 */
		static protected String parseVariableDecl(String s) {
			Matcher m = VARIABLE_PATT.matcher(s);
			return m.find() ? m.group(1) : null;
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
			return m.find() ? m.group(1) : null;
		}

		static protected boolean isSkippableLine(String line) {
			return line.length() == 0 || line.charAt(0) == ';';
		}

		/**
		 * Matcher for a specific command.
		 */
		static private class CmdMatch {
			CommandID id;
			String name;

			// Array with a collection of "reg" | "mem" | "imm"
			String[] signature;

			public CmdMatch(CommandID id, String name, String[] signature) {
				this.id = id;
				this.name = name;
				this.signature = signature;
			}

			@Override
			public String toString() {
				return String.format("CmdMatch[id=%s, name=%s, signature=%s]", id, name, arrayToString(signature));
			}
		}

		static private CmdMatch[] VALID_COMMANDS = makeValidCommands();

		static private CmdMatch[] makeValidCommands() {
			String[] arg_m = new String[] { "mem" };
			String[] arg_rr = new String[] { "reg", "reg" };
			String[] arg_mr = new String[] { "mem", "reg" };
			String[] arg_rm = new String[] { "reg", "mem" };
			String[] arg_rrm = new String[] { "reg", "reg", "mem" };

			return new CmdMatch[] {
				new CmdMatch(CommandID.ADD_REG_REG, "add", arg_rr),
				new CmdMatch(CommandID.ADD_MEM_REG, "add", arg_mr),
				new CmdMatch(CommandID.ADD_REG_MEM, "add", arg_rm),
				new CmdMatch(CommandID.SUB_REG_REG, "sub", arg_rr),
				new CmdMatch(CommandID.SUB_MEM_REG, "sub", arg_mr),
				new CmdMatch(CommandID.SUB_REG_MEM, "sub", arg_rm),
				new CmdMatch(CommandID.MOVE_MEM_REG, "move", arg_mr),
				new CmdMatch(CommandID.MOVE_REG_MEM, "move", arg_rm),
				new CmdMatch(CommandID.MOVE_REG_REG, "move", arg_rr),
				new CmdMatch(CommandID.MOVE_IMM_REG, "move", new String[] { "imm", "reg" }),
				new CmdMatch(CommandID.INC_REG, "inc", new String[] { "reg" }),
				new CmdMatch(CommandID.INC_MEM, "inc", arg_m),
				new CmdMatch(CommandID.JMP, "jmp", arg_m),
				new CmdMatch(CommandID.JN, "jn", arg_m),
				new CmdMatch(CommandID.JZ, "jz", arg_m),
				new CmdMatch(CommandID.JNZ, "jnz", arg_m),
				new CmdMatch(CommandID.JEQ, "jeq", arg_rrm),
				new CmdMatch(CommandID.JGT, "jgt", arg_rrm),
				new CmdMatch(CommandID.JLW, "jlw", arg_rrm),
				new CmdMatch(CommandID.CALL, "call", arg_m),
				new CmdMatch(CommandID.RET, "ret", new String[] {}),
			};
		}

		/**
		 * Attempts to parse `tokens` and match it with `candidate`, which
		 * should be one of the possible commands.
		 *
		 * @return a string array with the arguments (ready to be embedded into
		 * a Command instance) if it has succesfully matched, or null if it
		 * hasn't.
		 */
		static private String[] checkAndBuildArgs(String[] tokens, CmdMatch candidate) throws ParseException {
			if (tokens.length == 0)
				return null;

			String commandName = tokens[0];

			if (!commandName.equals(candidate.name) || tokens.length - 1 != candidate.signature.length)
				return null;

			String[] args = new String[candidate.signature.length];

			for (int i = 1; i < tokens.length; i++) {
				String token = tokens[i];
				String sig = candidate.signature[i - 1];

				if (sig.equals("mem")) {
					if (!isMemName(token))
						return null;
					args[i - 1] = "&" + token;
				} else if (sig.equals("reg")) {
					if (!isRegName(token))
						return null;
					args[i - 1] = token;
				} else if (sig.equals("imm")) {
					if (!isNumber(token))
						return null;
					args[i - 1] = Integer.toString(Integer.parseInt(token));
				} else {
					throw new ParseException(
						String.format("unexpected argument type: %s", sig));
				}
			}

			return args;
		}

		static protected Command parseCommand(String[] tokens) throws ParseException {
			if (tokens.length == 0)
				return null;

			for (CmdMatch candidate : VALID_COMMANDS) {
				String[] args = checkAndBuildArgs(tokens, candidate);
				if (args != null)
					return new Command(candidate.id, args);
			}

			return null;
		}
	}

	/**
	 * Error thrown when an unrecoverable parsing error is reached.
	 */
	public static class ParseException extends Exception {
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
			return String.format("Command[id=%s, args=%s]", id, arrayToString(args));
		}
	}

	private static <T> String arrayToString(T[] arr) {
		if (arr.length == 0)
			return "[]";

		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < arr.length - 1; i++) {
			sb.append(arr[i].toString());
			sb.append(", ");
		}
		sb.append(arr[arr.length - 1].toString());
		sb.append("]");
		return sb.toString();
	}

	private static <T> String arrayListToString(ArrayList<T> arr) {
		if (arr.size() == 0)
			return "[]";

		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < arr.size() - 1; i++) {
			sb.append(arr.get(i).toString());
			sb.append(", ");
		}
		sb.append(arr.get(arr.size() - 1).toString());
		sb.append("]");
		return sb.toString();
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
			System.exit(1);
		}
	}
}
