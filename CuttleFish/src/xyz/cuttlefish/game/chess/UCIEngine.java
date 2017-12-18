package xyz.cuttlefish.game.chess;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.signum;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import xyz.cuttlefish.mcts.Action;

/**
 * UCI-compatible {@linkplain ChessEngine}.
 * 
 * TODO: more generic UCI command and response parsing.
 * 
 * @author jbaxter
 */
public class UCIEngine implements ChessEngine {

	private static final String DEFAULT_EXECUTABLE_PATH = System.getProperty("user.home")
			+ "/git/Stockfish/src/stockfish";
	private static final long DEFAULT_MOVE_TIME_MILLIS = 25;
	private static final int DEFAULT_HASH_MB = 128;

	// prob(win) = 1 / (1+ exp(-scale * cp)) where cp is the position eval in
	// centipawns. scale set so that p(win | cp=200) = 0.8. See stockfish.png
	// for a
	// plot
	private static final double DEFAULT_LOGISTIC_SCALE = -log(0.25) / 200.0;

	@SuppressWarnings("unused")
	private static final class FEN {
		private String board;
		private boolean whiteToMove;
		private String castling;
		private String enPassant;
		private int halfMoveClock;
		private int moveNumber;

		private FEN(String fen) {
			String[] fields = fen.split("\\s+");
			board = fields[0];
			whiteToMove = "w".equals(fields[1]);
			castling = fields[2];
			enPassant = fieldExists(fields, 3) ? fields[3] : null;
			halfMoveClock = fieldExists(fields, 4) ? Integer.parseInt(fields[4]) : 0;
			moveNumber = fieldExists(fields, 5) ? Integer.parseInt(fields[5]) : 0;
		}

		private boolean fieldExists(String[] fields, int num) {
			return fields.length > num && fields[num] != null && fields[num].length() > 0;
		}
	}

	private Process engine;
	private BufferedReader engineIn;
	private BufferedWriter engineOut;

	private String executablePath = DEFAULT_EXECUTABLE_PATH;
	private long moveTimeMillis = DEFAULT_MOVE_TIME_MILLIS;
	private double logisticScale = DEFAULT_LOGISTIC_SCALE;
	private int hashMB = DEFAULT_HASH_MB;

	@Override
	public void start() {
		try {
			engine = Runtime.getRuntime().exec(executablePath);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		engineIn = new BufferedReader(new InputStreamReader(engine.getInputStream()));
		engineOut = new BufferedWriter(new OutputStreamWriter(engine.getOutputStream()));
		sendUCIOption("hash", hashMB);
	}

	@Override
	public void stop() {
		try {
			sendUCICommand("quit");
			engineIn.close();
			engineOut.close();
			engine.destroy();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public String reset() {
		sendUCICommand("position startpos");
		return displayFen();
	}

	@Override
	public List<Action> bestMoves(String fenString, int numMoves) {
		// check 50 move rule
		if (new FEN(fenString).halfMoveClock == 100) {
			return Collections.emptyList();
		}
		waitForReady();
		sendUCIOption("multipv", numMoves);
		waitForReady();
		sendUCICommand("position fen " + fenString);
		waitForReady();
		sendUCICommand("go movetime " + moveTimeMillis);
		List<String> response = readResponse("bestmove");
		return bestMoves(response, numMoves);
	}

	@Override
	public double eval(String fenString) {
		FEN fen = new FEN(fenString);
		// check 50 move rule
		if (fen.halfMoveClock == 100) {
			return 0;
		}
		waitForReady();
		sendUCICommand("position fen " + fenString);
		sendUCICommand("go movetime " + moveTimeMillis);
		List<String> response = readResponse("bestmove");
		for (int i = response.size() - 1; i >= 0; i--) {
			String line = response.get(i);
			String[] fields = line.split("\\s+");
			if ("bestmove".equals(fields[0]) && !"(none)".equals(fields[1])) {
				// not stalemate or checkmate: return eval of the best move
				return bestMoves(response, 1).get(0).value();
			}
			if (fields.length >= 6 && "score".equals(fields[3])) {
				int rawScore = Integer.valueOf(fields[5]);
				if (rawScore != 0) {
					throw new IllegalArgumentException("raw score must be zero: " + rawScore);
				}
				String scoreType = fields[4];
				return "mate".equals(scoreType) ? fen.whiteToMove ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY
						: 0;
			}
		}
		throw new IllegalStateException("no valid score");
	}

	@Override
	public String makeMove(String fen, String pgn) {
		waitForReady();
		sendUCICommand("position fen " + fen + " moves " + pgn);
		return displayFen();
	}

	@Override
	public String prettyPrint(String fen) {
		waitForReady();
		sendUCICommand("position fen " + fen);
		waitForReady();
		sendUCICommand("d");
		return readResponse("Checkers:").stream().collect(Collectors.joining("\n"));
	}

	/**
	 * Display the current position in FEN notation.
	 * 
	 * @return the current position in FEN notation
	 */
	public String displayFen() {
		waitForReady();
		sendUCICommand("d");
		String fen = "";
		List<String> response = readResponse("Checkers:");
		for (int i = response.size() - 1; i >= 0; i--) {
			String line = response.get(i);
			if (line.startsWith("Fen: ")) {
				fen = line.substring("Fen: ".length());
				break;
			}
		}
		return fen;
	}

	public void setExecutablePath(String executablePath) {
		this.executablePath = executablePath;
	}

	public String getExecutablePath() {
		return executablePath;
	}

	public void setMoveTimeMillis(long moveTimeMillis) {
		this.moveTimeMillis = moveTimeMillis;
	}

	public long getMoveTimeMillis() {
		return moveTimeMillis;
	}

	private List<Action> bestMoves(List<String> response, int numMoves) {
		// @formatter:off
		// e.g:
		// info depth 10 seldepth 17 multipv 1 score cp 0 nodes 35812 nps 1377384 tbhits 0 time 26 pv e4c3 d7d5 b2b4 a6c4 c1f4 b7d8 f4d6 f8g8
		// info depth 10 seldepth 17 multipv 1 score cp 0 upperbound nodes 35812 nps 1377384 tbhits 0 time 26 pv e4c3 d7d5 b2b4 a6c4 c1f4 b7d8 f4d6 f8g8
		// @formatter:on
		List<Action> moves = new ArrayList<>(numMoves);
		int lastRank = Integer.MAX_VALUE;
		for (int i = response.size() - 1; i >= 0; i--) {
			String line = response.get(i);
			if (line.indexOf("multipv") >= 0) {
				String[] fields = line.split("\\s+");
				// multipv rank
				int rank = Integer.valueOf(fields[6]);
				if (rank >= lastRank) {
					// previous iteration
					break;
				}
				// score in centipawns (cp), or distance to mate (mate)
				String scoreType = fields[8];
				int rawScore = Integer.valueOf(fields[9]);
				// convert to win prob
				double pwin = "cp".equals(scoreType) ? 1 / (1 + exp(-logisticScale * rawScore))
						: (1 + signum(rawScore)) / 2;
				// score is expected value. note that we don't explicitly model
				// draw prob
				double score = 2 * pwin - 1;
				// move in pgn
				String pgn = "upperbound".equals(fields[10]) ? fields[20] : fields[19];
				moves.add(new Action(pgn, score, rawScore / 100.0));
				lastRank = rank;
			}
		}
		Collections.reverse(moves);
		return moves;
	}

	private void sendUCIOption(String name, int value) {
		sendUCICommand("setoption name " + name + " value " + value);
	}

	private void sendUCICommand(String command) {
		try {
			engineOut.write(command + "\n");
			engineOut.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private List<String> readResponse(String expected) {
		try {
			List<String> lines = new ArrayList<String>();
			while (true) {
				String line = engineIn.readLine();
				lines.add(line);
				if (line.startsWith(expected))
					break;
			}
			return lines;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void waitForReady() {
		sendUCICommand("isready");
		readResponse("readyok");
	}
}
