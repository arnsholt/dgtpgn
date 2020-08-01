package org.riisholt.dgtpgn;

import net.sourceforge.argparse4j.inf.*;

import org.riisholt.dgtdriver.DgtDriver;
import org.riisholt.dgtdriver.DgtMessage;
import org.riisholt.dgtdriver.moveparser.Game;
import org.riisholt.dgtdriver.moveparser.MoveParser;

import java.io.*;

public class DgtPgn extends Program {
    public static void main(String[] argv) throws IOException, InterruptedException {
        Namespace args;
        ArgumentParser parser = defaultArgumentParser("dgtpdgn");
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
            writer.write(g.pgn(true));
            writer.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
