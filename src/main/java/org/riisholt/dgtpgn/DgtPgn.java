package org.riisholt.dgtpgn;

import com.fazecast.jSerialComm.SerialPort;

import org.riisholt.dgtdriver.DgtDriver;
import org.riisholt.dgtdriver.moveparser.Game;
import org.riisholt.dgtdriver.moveparser.MoveParser;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Date;

public class DgtPgn {
    public static void main(String[] argv) throws IOException, InterruptedException {
        String prefix = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date());
        String portName;
        if(argv.length == 1) {
            portName = argv[0];
        }
        else if(argv.length == 2) {
            portName = argv[0];
            prefix = argv[1];
        }
        else {
            System.err.println("Usage: dgtpgn PORT [PREFIX]");
            return;
        }

        new DgtPgn(portName, prefix).run();
    }

    private SerialPort port;
    private DgtDriver driver;
    private String outputPrefix;
    private int gameCount = 0;
    private DgtPgn(String portName, String prefix) {
        outputPrefix = prefix;
        System.out.printf("Connecting to %s\n", portName);
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(9600);
        port.setParity(SerialPort.NO_PARITY);
        port.setNumDataBits(8);
        port.setNumStopBits(1);
        if(!port.openPort())
            throw new RuntimeException("Failed to open port.");
        MoveParser parser = new MoveParser(this::gameComplete);
        driver = new DgtDriver(parser::gotMessage, (bytes) -> writeBytes(port, bytes));
    }

    private void run() throws IOException, InterruptedException {
        driver.board();
        driver.clock();
        driver.updateNice();
        InputStream in = port.getInputStream();
        byte[] buffer = new byte[128];
        while (true) {
            int read = in.read(buffer, 0, in.available());
            if(read == -1)
                break;
            else if(read == 0) {
                Thread.sleep(250);
            }
            else {
                driver.gotBytes(Arrays.copyOf(buffer, read));
            }
        }
    }

    private void gameComplete(Game g) {
        try {
            gameCount++;
            FileWriter writer = new FileWriter(String.format("%s-%d.pgn", outputPrefix, gameCount));
            writer.write(g.pgn(true));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeBytes(SerialPort port, byte[] bytes) {
        try {
            System.out.println(port.isOpen());
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
            System.err.printf("Failed to write: %s", e.getMessage());
        }
    }
}
