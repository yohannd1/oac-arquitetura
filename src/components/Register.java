package components;

public class Register {

	private String registerName;

	private int flagBits[];
	private int numFlags;

	private int data;
	private Bus busExt, busInt;

	/**
	 * Default constructor.
	 */
	public Register(String name, Bus extBus, Bus intBus) {
		this.registerName = name;
		this.busExt = extBus;
		this.busInt = intBus;
	}

	/**
	 * Get the register's value. Should not be used in
	 * microprograms.
	 */
	public int getData() {
		return data;
	}

	/**
	 * Constructor for the Flags register (dedicated bits for
	 * specific flags).
	 */
	public Register(String name, int numberOfBits, Bus bus) {
		super();
		this.registerName = name;
		this.numFlags = numberOfBits;
		this.flagBits = new int[numFlags];
		for (int i=0;i<numFlags;i++) {
			flagBits[i] = 0;
		}
		this.busExt = bus;
	}

	public String getRegisterName() {
		return registerName;
	}

	/**
	 * This method allows the UC or the ULA to access any special
	 * bit
	 * @param pos
	 */
	public int getBit(int pos) {
		return flagBits[pos];

	}

	/**
	 * This method allows the UC or the ULA to set any special bit
	 * @param pos
	 */
	public void setBit(int pos, int bit) {
		flagBits[pos] = bit;
	}



	/**
	 * This method stores the data from the bus into this register
	 */
	public void store() {
		data = busExt.get();
	}

	/**
	 * This method reads the data from this register and stores it into the bus
	 */
	public void read() {
		busExt.put(data);
	}

	/**
	 * This method copies the data from this register to the internalbus
	 */
	public void internalRead() {
		busInt.put(data);
	}

	/**
	 * This method sopies the data from the internalbus to this register
	 */
	public void internalStore() {
		data = busInt.get();
	}


}
