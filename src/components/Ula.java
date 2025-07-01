package components;

public class Ula {
	private Bus ulaBus; // internal to the ULA
	private Bus extBus;
	private Bus intBus;
	private Register reg0;
	private Register reg1;

	public Ula(Bus extBus, Bus intBus) {
		super();

		this.extBus = extBus;
		this.intBus = intBus;
		ulaBus = new Bus();

		reg0 = new Register("Ula.Reg0", extBus, ulaBus);
		reg1 = new Register("Ula.Reg1", extBus, ulaBus);
	}

	public int debugGet(int reg) {
		return getRegister(reg).getData();
	}

	/**
	 * This method adds the reg0 and reg1 values, storing the result in reg1.
	 */
	public void add() {
		int res = 0;
		ulaBus.put(0);
		reg0.internalRead(); // puts its data into the internal bus
		res = ulaBus.get(); // stored for operation
		reg1.internalRead(); // puts the internal data into the internal bus
		res += ulaBus.get(); // the operation was performed
		ulaBus.put(res);
		reg1.internalStore(); // saves the result into internal store
	}

	/**
	 * This method sub the reg1 value from reg0 value, storing the result in reg1
	 * This processing uses a Ula's internal bus
	 */
	public void sub() {
		int res = 0;
		ulaBus.put(0);
		reg0.internalRead(); //puts its data into the internal bus
		res = ulaBus.get(); //stored for operation
		reg1.internalRead(); //puts the internal data into the internal bus
		res -= ulaBus.get(); //the operation was performed
		ulaBus.put(res);
		reg1.internalStore(); //saves the result into internal store
	}

	/**
	 * Increment the value in ula(1), inplace.
	 */
	public void inc() {
		reg1.internalRead();
		int result = ulaBus.get() + 1;
		ulaBus.put(result);
		reg1.internalStore();
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
		if (reg == 0) return reg0;
		else return reg1;
	}

	/**
	 * Store the value in the internal bus into a specific register.
	 */
	public void internalStore(int reg) {
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
