package xyz.cuttlefish.game.chess;

import java.util.List;

import xyz.cuttlefish.mcts.Action;
import xyz.cuttlefish.mcts.GameState;

/**
 * GameState backed by a {@linkplain ChessEngine}.
 * 
 * @author jbaxter
 */
public class ChessBoard implements GameState {
	private static final int DEFAULT_NUM_MOVES = 20;
	private List<Action> actions;
	private final ChessEngine engine;
	private final String fen;
	private final int numMoves;

	public ChessBoard(ChessEngine engine, String fen) {
		this(engine, fen, DEFAULT_NUM_MOVES);
	}

	public ChessBoard(ChessEngine engine, String fen, int numMoves) {
		this.engine = engine;
		this.fen = fen;
		this.numMoves = numMoves;
	}

	@Override
	public List<Action> actions() {
		if (actions == null) {
			actions = engine.bestMoves(fen, numMoves);
		}
		return actions;
	}

	@Override
	public ChessBoard makeMove(Action a) {
		return new ChessBoard(engine, engine.makeMove(fen, a.asString()), numMoves);
	}

	@Override
	public double value() {
		// position value is the value of the best move, unless it is a terminal
		// position
		return actions().size() > 0 ? actions().get(0).value() : engine.eval(fen);
	}

	@Override
	public String asString() {
		return fen;
	}

	@Override
	public String toString() {
		return engine.prettyPrint(fen);
	}
}
