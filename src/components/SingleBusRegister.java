package components;

public class SingleBusRegister {
	private String name;
	private int data;
	private Bus bus;

	public SingleBusRegister(String name, Bus bus) {
		this.name = name;
		this.bus = bus;
	}

	/**
	 * Get the register's value. Should not be used in
	 * microprograms.
	 */
	public int getData() {
		return data;
	}

	/**
	 * Set the register's value. Should not be used in
	 * microprograms.
	 */
	public void setData(int x) {
		data = x;
	}

	public String getName() {
		return name;
	}

	/**
	 * Store the data from the bus into the register.
	 */
	public void store() {
		data = bus.get();
	}

	/**
	 * Read the data from the register into the bus.
	 */
	public void read() {
		bus.put(data);
	}
}
