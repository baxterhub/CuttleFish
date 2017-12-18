package xyz.cuttlefish.game.chess;

import java.util.List;

import xyz.cuttlefish.mcts.Action;

public interface ChessEngine {
	/**
	 * Start the engine
	 */
	void start();

	/**
	 * Stop the engine
	 */
	void stop();

	/**
	 * Initialize the game to the starting position and return the starting
	 * position in FEN notation.
	 * 
	 * @return the starting position
	 */
	String reset();

	/**
	 * The n-best moves for the specified position, and their scores.
	 * 
	 * @param fen
	 *            the position in fen notation
	 * @param numMoves
	 *            the number of moves to return
	 * @return the n-best moves for the specified position, in pgn
	 */
	List<Action> bestMoves(String fen, int numMoves);

	/**
	 * The evaluation of the specified position.
	 * 
	 * @param fen
	 *            the position in fen notation.
	 * @return the evaluation of the specified position
	 */
	double eval(String fen);

	/**
	 * Make the specified move in the specified position and return the new
	 * position.
	 * 
	 * @param fen
	 *            the position in fen notation
	 * @param pgn
	 *            the move in pgn notation
	 * @return the new position in fen notation
	 */
	String makeMove(String fen, String pgn);

	/**
	 * Return a pretty representation of the specified position.
	 * 
	 * @param fen
	 *            position in FEN notation
	 * @return a pretty representation of the board
	 */
	String prettyPrint(String fen);
}
