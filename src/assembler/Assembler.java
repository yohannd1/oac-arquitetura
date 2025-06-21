package assembler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

import components.Register;

import architecture.Architecture;

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

	/*
	 * An assembly program is always in the following template
	 * <variables>
	 * <commands>
	 * Obs.
	 * 		variables names are always started with alphabetical char
	 * 	 	variables names must contains only alphabetical and numerical chars
	 *      variables names never uses any command name
	 * 		names ended with ":" identifies labels i.e. address in the memory
	 * 		Commands are only that ones known in the architecture. No comments allowed
	 *
	 * 		The assembly file must have the extention .dsf
	 * 		The executable file must have the extention .dxf
	 */

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
	 * This method scans the strings in lines
	 * generating, for each one, the corresponding machine code
	 *
	 * @param lines
	 */
	public void parse() {
		for (String s:lines) {
			String tokens[] = s.split(" ");
			if (findCommandNumber(tokens)>=0) { //the line is a command
				proccessCommand(tokens);
			}
			else { //the line is not a command: so, it can be a variable or a label
				if (tokens[0].endsWith(":")){ //if it ends with : it is a label
					String label = tokens[0].substring(0, tokens[0].length()-1); //removing the last character
					labels.add(label);
					labelsAdresses.add(objProgram.size());
				}
				else //otherwise, it must be a variable
					variables.add(tokens[0]);
			}
		}

	}

	/**
	 * This method processes a command, putting it and its parameters (if they
	 * have) into the final array
	 *
	 * @param tokens
	 */
	protected void proccessCommand(String[] tokens) {
		String command = tokens[0];
		String parameter ="";
		String parameter2 = "";
		int commandNumber = findCommandNumber(tokens);
		if (commandNumber == 0) { //must to proccess an add command
			parameter = tokens[1];
			parameter = "&"+parameter; //this is a flag to indicate that is a position in memory
		}
		if (commandNumber == 1) { //must to proccess an sub command
			parameter = tokens[1];
			parameter = "&"+parameter;//this is a flag to indicate that is a position in memory
		}
		if (commandNumber == 2) { //must to proccess an jmp command
			parameter = tokens[1];
			parameter = "&"+parameter;//this is a flag to indicate that is a position in memory
		}
		if (commandNumber == 3) { //must to proccess an jz command
			parameter = tokens[1];
			parameter = "&"+parameter;//this is a flag to indicate that is a position in memory
		}
		if (commandNumber == 4) { //must to proccess an jn command
			parameter = tokens[1];
			parameter = "&"+parameter;//this is a flag to indicate that is a position in memory
		}
		if (commandNumber == 5) { //must to proccess an read command
			parameter = tokens[1];
			parameter = "&"+parameter;//this is a flag to indicate that is a position in memory
		}
		if (commandNumber == 6) { //must to proccess an store command
			parameter = tokens[1];
			parameter = "&"+parameter;//this is a flag to indicate that is a position in memory
		}
		if (commandNumber == 7) { //must to proccess an ldi command
			parameter = tokens[1];
		}
		if (commandNumber == 8) { //must to proccess an inc command

		}
		if (commandNumber == 9) { //must to proccess an moveRegReg command
			parameter = tokens[1];
			parameter2 = tokens[2];
		}
		objProgram.add(Integer.toString(commandNumber));
		if (!parameter.isEmpty()) {
			objProgram.add(parameter);
		}
		if (!parameter2.isEmpty()) {
			objProgram.add(parameter2);
		}
	}

	/**
	 * Use the tokens to search a command in the commands list and return its
	 * ID. Some commands (as move) can have multiple formats (reg reg, mem reg,
	 * reg mem) and multiple ids, one for each format.
	 *
	 * @param tokens
	 * @return
	 */
	private int findCommandNumber(String[] tokens) {
		int p = commands.indexOf(tokens[0]);
		if (p < 0) { // the command isn't in the list. So it must have multiple formats
			if ("move".equals(tokens[0])) //the command is a move
				p = processMove(tokens);
		}
		return p;
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
		File file = new File(filename+".dxf");
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		for (String l : execProgram)
			writer.write(l+"\n");
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
		System.out.println("Checking labels and variables");
		for (String line : objProgram) {
			boolean found = false;
			if (line.startsWith("&")) { // if starts with "&", it is a label or a variable
				line = line.substring(1, line.length());
				if (labels.contains(line))
					found = true;
				if (variables.contains(line))
					found = true;
				if (!found) {
					System.out.printf("FATAL ERROR! Variable or label %s not declared!\n", line);
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
			System.out.println("Usage: assembler <INPUT>");
			System.out.println("INPUT must be the name of a .dsf file, without the extension");
			System.exit(2);
		}

		String filename = args[0];

		Assembler assembler = new Assembler();

		System.out.printf("Reading source assembler file: %s.dsf\n", filename);
		assembler.read(filename);

		System.out.println("Generating the object program");
		assembler.parse();

		System.out.printf("Generating executable: %s.dxf\n", filename);
		assembler.makeExecutable(filename);

		System.out.println("Assembling finished!");
	}
}
