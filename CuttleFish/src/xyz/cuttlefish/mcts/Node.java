package xyz.cuttlefish.mcts;

import static java.lang.Math.exp;
import static java.lang.Math.sqrt;
import static java.util.Comparator.comparingDouble;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A node in a monte-carlo tree. Holds the GameState and the MCTS statistics.Ï
 * 
 * @author jbaxter
 */
public class Node {
	private static final double DEFAULT_SOFTMAX_TEMP = 1.0;
	private static final double DEFAULT_VIRTUAL_LOSS = 3;
	
	// informational
	public static final AtomicInteger MAX_DEPTH = new AtomicInteger();
	public static final AtomicInteger NODE_COUNT = new AtomicInteger();

	// from: "Mastering the game of Go with deep neural networks and tree
	// search" - c_puct
	private static final double c = 5.0;

	private final GameState gameState;
	private Node parent;

	// index of this node in its parent's children
	private final int childIndex;
	private Node[] children;
	// statistics for each action:
	// visit count(N), total action(W), mean action(Q), prior (P)
	private int N[];
	private double[] W, Q, P;

	// sum of visit count N
	private int sumN;

	// softmax temperature when normalizing action values to probabilities
	private final double temp = DEFAULT_SOFTMAX_TEMP;
	
	private final double virtualLoss = DEFAULT_VIRTUAL_LOSS;
	
	// true if the Node has been expanded
	private boolean expanded;

	// informational
	private int depth;

	/**
	 * Create a root Node for the specified GameState.
	 * 
	 * @param gameState
	 *            the GameState
	 */
	public Node(GameState gameState) {
		this(null, gameState, -1);
	}

	/**
	 * Create a Node with the specified parent Node and with the specified
	 * GameState.
	 * 
	 * @param parent
	 *            parent Node
	 * @param gameState
	 *            GameState
	 * @param childIndex
	 *            the index of the new node in its parent's children. Ignored if
	 *            parent is null
	 */
	public Node(Node parent, GameState gameState, int childIndex) {
		this.parent = parent;
		this.gameState = gameState;
		this.childIndex = childIndex;
		expanded = false;
		depth = parent == null ? 0 : 1 + parent.depth;
		MAX_DEPTH.updateAndGet(d -> depth > d ? depth : d);
		NODE_COUNT.incrementAndGet();
	}

	public boolean isExpanded() {
		return expanded;
	}

	public boolean isTerminal() {
		return gameState.actions().isEmpty();
	}

	public void orphan() {
		parent = null;
	}

	public Node select() {
		// select action with max UCB (Upper Confidence Bound)
		// add 1 to ensure they're not all zero (alphago paper has just sumN)
		int topAction = IntStream.range(0, N.length).boxed().max(comparingDouble(this::UCB)).get();

		if (children == null) {
			children = new Node[N.length];
		}

		return children[topAction] == null ? (children[topAction] = new Node(this,
				gameState.makeMove(gameState.actions().get(topAction)), topAction)) : children[topAction];
	}

	/**
	 * The upper confidence bound for the action with the specified index
	 */
	private double UCB(Integer i) {
		return Q[i] + c * sqrt(sumN + 1) * P[i] / (1 + N[i]);
	}

	public void expand() {
		if (expanded)
			return;

		expanded = true;

		List<Action> actions = gameState.actions();
		int n = actions.size();
		N = new int[n];
		W = new double[n];
		Q = new double[n];

		// softmax: P ~ exp(v/temp)
		P = IntStream.range(0, n).mapToDouble(i -> exp(actions.get(i).value() / temp)).toArray();
		// normalize
		double sum = Arrays.stream(P).sum();
		IntStream.range(0, n).forEach(i -> P[i] /= sum);

	}

	public void backup() {
		if (parent != null) {
			parent.backup(-gameState.value(), this.childIndex);
		}
	}

	private void backup(double value, int childIndex) {
		++sumN;
		N[childIndex] += 1;
		W[childIndex] += value;
		Q[childIndex] = W[childIndex] / N[childIndex];
		if (parent != null) {
			parent.backup(-value, this.childIndex);
		}
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(String.format("%n%21s %10s %10s %10s %10s%n", "action(raw/exp)", "P", "Q", "N", "Q+U"));
		// sort actions in descending Q-value order
		String s = IntStream.range(0, N.length).boxed().sorted(comparingDouble(i -> -Q[i])).map(i -> String
				.format("%21s %,10.3f %,10.3f %,10d %,10.3f%n", gameState.actions().get(i), P[i], Q[i], N[i], UCB(i)))
				.collect(Collectors.joining());
		buf.append(s);
		return buf.toString();
	}
}
