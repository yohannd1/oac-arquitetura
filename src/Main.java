import java.io.IOException;

import architecture.Architecture;
import assembler.Assembler;
import assembler.ParseException;
import assembler.AssemblerException;

public class Main {
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Usage: program <INPUT>");
			System.err.println("INPUT must be the name of a .dsf file, without the extension");
			System.err.println("This program will assemble the file into a .dxf executable, and then execute it.");
			System.exit(2);
		}

		String fileNameNoExt = args[0];

		Assembler assembler = new Assembler();

		try {
			System.err.printf("Reading source assembler file: %s.dsf\n", fileNameNoExt);
			assembler.read(fileNameNoExt);

			System.err.println("Generating the object program");
			assembler.parseAll();

			System.err.printf("Generating executable: %s.dxf\n", fileNameNoExt);
			assembler.makeExecutable(fileNameNoExt);

			System.err.println("Assembling finished!");
		} catch (ParseException ex) {
			System.err.println("Error while parsing: " + ex);
		} catch (AssemblerException ex) {
			System.err.println("Error while assembling: " + ex);
		}

		Architecture arch = new Architecture(true);
		arch.readExec(fileNameNoExt);
		arch.controlUnitEexec();
	}
}
