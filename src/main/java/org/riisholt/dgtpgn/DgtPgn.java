package org.riisholt.dgtpgn;

import com.fazecast.jSerialComm.SerialPort;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.*;

import org.riisholt.dgtdriver.DgtDriver;
import org.riisholt.dgtdriver.moveparser.Game;
import org.riisholt.dgtdriver.moveparser.MoveParser;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

public class DgtPgn {
    public static void main(String[] argv) throws IOException, InterruptedException {
        ArgumentParser parser = ArgumentParsers.newFor("dgtpgn").build()
                .defaultHelp(true)
                .description("Record games from DGT board to PGN");
        parser.addArgument("--debug")
                .help("Write debug data to file")
                .action(Arguments.storeConst())
                .setConst(true)
                .setDefault(false);
        parser.addArgument("--prefix")
                .type(String.class)
                .setDefault(new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date()))
                .help("Filename prefix for PGN output");
        parser.addArgument("--port")
                .type(String.class)
                .help("Name of serial port to connect to")
                .required(true);

        Namespace args;
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

    private SerialPort port;
    private MoveParser parser;
    private DgtDriver driver;
    private String outputPrefix;
    private int gameCount = 0;
    private FileWriter debugOut;
    private long startNanos;
    private DgtPgn(String portName, String prefix, boolean debug) throws IOException {
        outputPrefix = prefix;
        System.out.printf("Connecting to %s\n", portName);
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(9600);
        port.setParity(SerialPort.NO_PARITY);
        port.setNumDataBits(8);
        port.setNumStopBits(1);
        if(!port.openPort())
            throw new RuntimeException(String.format("Failed to open port %s.", portName));
        parser = new MoveParser(this::gameComplete);
        driver = new DgtDriver(parser::gotMessage, this::writeBytes);
        if(debug) {
            debugOut = new FileWriter(String.format("%s.debug", outputPrefix));//new FileOutputStream(String.format("%s.debug", outputPrefix));
            startNanos = System.nanoTime();
        }
    }

    private void run() throws IOException, InterruptedException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Write out possible partial game on shutdown.
            parser.endGame();
        }));
        driver.board();
        driver.clock();
        driver.updateNice();
        InputStream in = port.getInputStream();
        byte[] buffer = new byte[128];
        if(debugOut != null)
            System.out.printf("Starting read loop, outputPrefix=%s\n", outputPrefix);
        while (true) {
            int read = in.read(buffer, 0, in.available());
            if(read == -1)
                break;
            else if(read == 0) {
                Thread.sleep(200);
            }
            else {
                byte[] bytes = Arrays.copyOf(buffer, read);
                debugWrite(true, bytes);
                driver.gotBytes(bytes);
            }
        }
    }

    private void gameComplete(Game g) {
        try {
            gameCount++;
            String filename = String.format("%s-%d.pgn", outputPrefix, gameCount);
            if(debugOut != null)
                System.out.printf("Got game, writing to %s\n", filename);
            FileWriter writer = new FileWriter(filename);
            writer.write(g.pgn(true));
            writer.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeBytes(byte[] bytes) {
        try {
            debugWrite(false, bytes);
            OutputStream s = port.getOutputStream();
            if(s != null) {
                s.write(bytes);
                s.close();
            }
            else {
                System.err.println("Failed to get output stream from port");
            }
        }
        catch(IOException e) {
            System.err.printf("Failed to write: %s\n", e.getMessage());
        }
    }

    private void debugWrite(boolean isInput, byte[] bytes) throws IOException {
        if(debugOut == null) return;

        long timeDelta = System.nanoTime() - startNanos;
        char direction = isInput ? '<' : '>';
        String byteString = new String(Base64.getEncoder().encode(bytes));
        debugOut.write(String.format("%d %c %s", timeDelta, direction, byteString));
        debugOut.flush();
    }
}
