package xyz.cuttlefish.mcts;

/**
 * An available action in a {@linkplain GameState}.
 * 
 * @author jbaxter
 */
public class Action {
	private final String stringRep;
	private final double value;
	private final double rawValue;

	public Action(String stringRep, double value) {
		this(stringRep, value, value);
	}

	public Action(String stringRep, double value, double rawValue) {
		this.stringRep = stringRep;
		this.value = value;
		this.rawValue = rawValue;
	}

	public double rawValue() {
		return rawValue;
	}

	/**
	 * The value of the Action: the expected value of the game if the Action is
	 * taken.
	 * 
	 * @return the Action value
	 */
	public double value() {
		return value;
	}

	/**
	 * The Action encoded as a String.
	 * 
	 * @return the Action encoded as a String
	 */
	public String asString() {
		return stringRep;
	}

	@Override
	public String toString() {
		return String.format("%4s(%+3.2f/%+3.2f)", stringRep, rawValue, value);
	}
}
