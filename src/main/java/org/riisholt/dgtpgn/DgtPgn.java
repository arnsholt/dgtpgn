package org.riisholt.dgtpgn;

import net.sourceforge.argparse4j.inf.*;

import org.riisholt.dgtdriver.BWTime;
import org.riisholt.dgtdriver.DgtDriver;
import org.riisholt.dgtdriver.DgtMessage;
import org.riisholt.dgtdriver.game.Piece;
import org.riisholt.dgtdriver.game.Role;
import org.riisholt.dgtdriver.game.Square;
import org.riisholt.dgtdriver.moveparser.Game;
import org.riisholt.dgtdriver.moveparser.MoveParser;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

public class DgtPgn extends Program {
    public static void main(String[] argv) throws IOException, InterruptedException {
        Namespace args;
        ArgumentParser parser = defaultArgumentParser("dgtpgn");
        try {
            args = parser.parseArgs(argv);
        }
        catch(ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
            /* The compiler doesn't realize System.exit() terminates the
             * program, so we need a throw here to silence the uninitialized
             * variable error on args we get without it.
             */
            throw new RuntimeException();
        }

        String prefix = args.get("prefix");
        String portName = args.get("port");
        boolean debug = args.getBoolean("debug");

        new DgtPgn(portName, prefix, debug).run();
    }

    private MoveParser parser;
    private int gameCount = 0;
    private DgtPgn(String portName, String prefix, boolean debug) throws IOException {
        super(portName, prefix, debug);
        parser = new MoveParser(this::gameComplete);
    }

    protected void initDriver(DgtDriver driver) {
        driver.board();
        driver.clock();
        driver.updateNice();
    }

    protected void gotMessage(DgtMessage msg) {
        parser.gotMessage(msg);
        Game game = parser.currentGame(null);

        if(!(game != null && game.moves.size() > 0))
            return;

        try {
            printBoard(parser.boardState());
            printMoves(game);
            BWTime clockInfo = game.moves.get(game.moves.size() - 1).clockInfo;
            if(clockInfo != null)
                printClocks(clockInfo);
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void shutdownHook() {
        parser.endGame();
    }

    private void gameComplete(Game g) {
        try {
            gameCount++;
            String filename = String.format("%s-%d.pgn", getOutputPrefix(), gameCount);
            if(isDebug())
                System.out.printf("Got game, writing to %s\n", filename);
            FileWriter writer = new FileWriter(filename);
            writer.write("[White \"White\"]\n");
            writer.write("[Black \"Black\"]\n\n");
            writer.write(g.pgn(true));
            writer.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void printBoard(org.riisholt.dgtdriver.game.Board board) throws IOException {
        clearScreen();

        // Board frame
        at(1, 1, "+--------+");
        at(2, 1, "|");
        at(3, 1, "|");
        at(4, 1, "|");
        at(5, 1, "|");
        at(6, 1, "|");
        at(7, 1, "|");
        at(8, 1, "|");
        at(9, 1, "|");
        at(2, 10, "|");
        at(3, 10, "|");
        at(4, 10, "|");
        at(5, 10, "|");
        at(6, 10, "|");
        at(7, 10, "|");
        at(8, 10, "|");
        at(9, 10, "|");
        at(10, 1, "+--------+");

        // Pieces
        for(Map.Entry<Integer, Piece> entry: board.pieceMap().entrySet()) {
            Integer square = entry.getKey();
            int rank = Square.rank(square);
            int file = Square.file(square);
            Piece piece = entry.getValue();
            String symbol = piece.role.symbol;
            if(piece.role == Role.PAWN)
                symbol = "P";
            at(9 - rank, file + 2, piece.white? symbol: symbol.toLowerCase());
        }
    }

    private void printMoves(Game game) throws IOException {
        ArrayList<String> moves = new ArrayList<>();
        for(int i = 0; 2*i < game.moves.size(); i++) {
            String white = game.moves.get(2*i).san;
            String black = 2*i + 1 < game.moves.size()? game.moves.get(2*i + 1).san : "";
            moves.add(String.format("%3d. %-7s %-7s", i + 1, white, black));
        }

        int curRow = 1;
        int curCol = 12;
        for(String s: moves) {
            at(curRow, curCol, s);
            curRow += 1;
        }
    }

    private void printClocks(BWTime clocks) throws IOException {
        String whiteTime = clocks.leftTimeString();
        String blackTime = clocks.rightTimeString();
        at(11, 1, whiteTime);
        at(12, 4, blackTime);
    }

    private void at(int x, int y, String s) {
        System.out.printf("\u001B[%d;%dH%s", x, y, s);
    }

    private void clearScreen() {
        System.out.print("\u001B[1,1H\u001B[2J");
    }
}
