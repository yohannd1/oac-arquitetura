package assembler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import components.Register;
import architecture.Architecture;
import architecture.CommandID;

public class Assembler {
	private ArrayList<String> lines;
	private ArrayList<String> objProgram;
	private ArrayList<String> execProgram;
	private ArrayList<String> commands;
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
		commands = arch.getCommandsList();
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
	 * Process a move command.
	 */
	private int processMove(String[] tokens) {
		String p1 = tokens[1];
		String p2 = tokens[2];
		int p = -1;

		// this is a moveRegReg comand
		if ((p1.startsWith("%")) && (p2.startsWith("%")))
			p = commands.indexOf("moveRegReg");

		return p;
	}

	/**
	 * Create the executable program from the object program.
	 *
	 * @param filename
	 * @throws IOException
	 */
	public void makeExecutable(String filename) throws IOException {
		if (!checkProperDeclaration())
			return;

		// allocate memory space to store program and variables
		execProgram = (ArrayList<String>) objProgram.clone();

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
		for (Register r : arch.getRegistersList()) {
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
}
