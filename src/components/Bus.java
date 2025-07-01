package components;

/**
 * Represents a bus, storing data and being connected to multiple components.
 */
public class Bus {
	private int data;

	public Bus() {
		data = 0;
	}

	/**
	 * Put the data in the bus.
	 */
	public void put(int data) {
		this.data = data;
	}

	/**
	 * Get the data from the bus.
	 */
	public int get() {
		return this.data;
	}
}
