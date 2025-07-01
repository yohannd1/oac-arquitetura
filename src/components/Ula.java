package components;

public class Ula {
	private Bus ulaBus; // bus internal to the ULA
	private Bus extBus;
	private Bus intBus;
	private Register reg1;
	private Register reg2;

	public Ula(Bus extBus, Bus intBus) {
		super();
		this.extBus = extBus;
		this.intBus = intBus;
		ulaBus = new Bus();
		reg1 = new Register("UlaReg0", extBus, ulaBus);
		reg2 = new Register("UlaReg1", extBus, ulaBus);
	}

	public int debugGet(int reg) {
		return getRegister(reg).getData();
	}

	/**
	 * This method adds the reg1 and reg2 values, storing the result in reg2.
	 */
	public void add() {
		int res = 0;
		ulaBus.put(0);
		reg1.internalRead(); // puts its data into the internal bus
		res = ulaBus.get(); // stored for operation
		reg2.internalRead(); // puts the internal data into the internal bus
		res += ulaBus.get(); // the operation was performed
		ulaBus.put(res);
		reg2.internalStore(); // saves the result into internal store
	}

	/**
	 * This method sub the reg2 value from reg1 value, storing the result in reg2
	 * This processing uses a Ula's internal bus
	 */
	public void sub() {
		int res = 0;
		ulaBus.put(0);
		reg1.internalRead(); //puts its data into the internal bus
		res = ulaBus.get(); //stored for operation
		reg2.internalRead(); //puts the internal data into the internal bus
		res -= ulaBus.get(); //the operation was performed
		ulaBus.put(res);
		reg2.internalStore(); //saves the result into internal store
	}

	/**
	 * Increment the value in ula(1), inplace.
	 */
	public void inc() {
		reg2.internalRead();
		int result = ulaBus.get() + 1;
		ulaBus.put(result);
		reg2.internalStore();
	}

	/**
	 * Store the value in the external bus into an ULA register.
	 */
	public void store(int reg) {
		getRegister(reg).store(); // extBus->regXX
	}

	/**
	 * Read a value from an ULA register and store it into the external bus.
	 */
	public void read(int reg) {
		getRegister(reg).read(); // regXX->extBus
	}

	private Register getRegister(int reg) {
		if (reg == 0) return reg1;
		else return reg2;
	}

	/**
	 * Store the value in the internal bus into a specific register.
	 */
	public void internalStore(int reg) {
		System.out.printf("$1 %d\n", intBus.get());
		ulaBus.put(intBus.get()); // ulaBus<-intBus
		getRegister(reg).internalStore(); // regXX<-ulaBus
	}

	/**
	 * Read the value from a register into the internal bus.
	 */
	public void internalRead(int reg) {
		getRegister(reg).internalRead(); // regXX->ulaBus
		intBus.put(ulaBus.get()); // ulaBus->intBus
	}
}
