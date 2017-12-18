package xyz.cuttlefish.mcts;

import java.util.List;

/**
 * Represents the state of a two-player game.
 * 
 * @author jbaxter
 */
public interface GameState {
	/**
	 * The available actions in the GameState. A GameState with no available
	 * Actions is a terminal state.
	 * 
	 * @return the available actions
	 */
	List<Action> actions();

	/**
	 * Generate the new GameState resulting from taking the specified Action in
	 * the GameState.
	 * 
	 * @param a
	 *            the Action to take
	 * @return the new GameState
	 */
	GameState makeMove(Action a);

	/**
	 * The value of the GameState. This is usually the estimated expected value
	 * of the current position from the perspective of the first-player to move
	 * in the game.
	 * 
	 * @return the value of the GameState
	 */
	double value();

	/**
	 * The GameState encoded as a String.
	 * 
	 * @return the GameState encoded as a String
	 */
	String asString();
}
