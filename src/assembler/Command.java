package assembler;

import architecture.CommandID;

public class Command {
	CommandID id;
	String[] args;

	public Command(CommandID id, String[] args) {
		this.id = id;
		this.args = args;
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

	@Override
	public String toString() {
		return String.format("Command[id=(%s, code=%d), args=%s]", id, id.toInt(), Command.arrayToString(args));
	}
}
