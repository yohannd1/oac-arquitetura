package assembler;

import architecture.CommandID;

public class Command {
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
