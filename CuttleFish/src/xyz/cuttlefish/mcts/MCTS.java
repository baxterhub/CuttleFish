package xyz.cuttlefish.mcts;

import xyz.cuttlefish.game.chess.ChessBoard;
import xyz.cuttlefish.game.chess.ChessEngine;
import xyz.cuttlefish.game.chess.UCIEngine;

public class MCTS {
	// the move time in milliseconds
	private long moveTimeMillis;

	private int simulations;

	public MCTS() {
	}

	public void setMoveTimeMillis(long moveTimeMillis) {
		this.moveTimeMillis = moveTimeMillis;
	}

	public long getMoveTimeMillis() {
		return moveTimeMillis;
	}

	/**
	 * Search the specified Node.
	 * 
	 * @param node
	 *            the Node to search
	 */
	public void search(Node node) {
		// discard the parent tree, if any
		node.orphan();
		simulations = 0;
		long end = System.currentTimeMillis() + moveTimeMillis;
		while (System.currentTimeMillis() < end) {
			simulate(node);
			++simulations;
		}
		log(node, simulations);
	}

	private void log(Node node, int simulations) {
		System.out.format("%10s %10s %10s %10s %10s %10s%n", "sims", "millis", "sims/sec", "nodes", "nodes/sec",
				"max depth");
		System.out.format("%,10d %,10d %,10d %,10d %,10d %,10d%n", simulations, getMoveTimeMillis(),
				1000 * simulations / getMoveTimeMillis(), Node.NODE_COUNT.get(),
				1000 * Node.NODE_COUNT.get() / getMoveTimeMillis(), Node.MAX_DEPTH.get());
		System.out.println(node);
	}

	/**
	 * Perform one MCTS simulation, starting at the specified Node
	 * 
	 * @param node
	 *            the Node to simulate
	 */
	private void simulate(Node node) {
		while (node != null && node.isExpanded() && !node.isTerminal()) {
			node = node.select();
		}
		if (!node.isExpanded()) {
			node.expand();
		}
		node.backup();
	}

	public static void main(String[] args) {
		ChessEngine engine = new UCIEngine();
		engine.start();
		//String fen = "2q1rr1k/3bbnnp/p2p1pp1/2pPp3/PpP1P1P1/1P2BNNP/2BQ1PRK/7R b - -";
		String fen = "rn3r1k/pn1p1ppq/bpp4p/7P/4N1Q1/6P1/PP3PB1/R1B1R1K1 w - - 3 21";
		// String fen = engine.reset();
		ChessBoard board = new ChessBoard(engine, fen);
		Node node = new Node(board);
		MCTS mcts = new MCTS();
		mcts.setMoveTimeMillis(20000);
		mcts.search(node);
		engine.stop();
	}
}
