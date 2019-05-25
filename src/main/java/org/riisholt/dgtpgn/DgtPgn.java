package org.riisholt.dgtpgn;

import org.openmuc.jrxtx.*;

import org.riisholt.dgtdriver.DgtDriver;
import org.riisholt.dgtdriver.moveparser.Game;
import org.riisholt.dgtdriver.moveparser.MoveParser;

public class DgtPgn {
    public static void main(String[] argv) throws java.io.IOException {
        // TODO: Validate args.

        new DgtPgn(argv[0]).run();
    }

    //private SerialPort port;
    //private DgtDriver driver;
    private DgtPgn(String portName) throws java.io.IOException {
        System.out.println(portName);
        SerialPort port = SerialPortBuilder.newBuilder(portName)
                                .setDataBits(DataBits.DATABITS_8)
                                .setBaudRate(9600)
                                .setStopBits(StopBits.STOPBITS_1)
                                .setParity(Parity.NONE)
                                .setFlowControl(FlowControl.NONE)
                                .build();
        MoveParser parser = new MoveParser(this::gameComplete);
        DgtDriver driver = new DgtDriver(parser::gotMove, (bytes) -> writeBytes(port, bytes));
    }

    private void run() {
        System.out.println("Hello world?");
    }

    public void gameComplete(Game g) {
        // TODO: Write PGN to file.
    }

    public void writeBytes(SerialPort port, byte[] bytes) {
        try {
            port.getOutputStream().write(bytes);
        }
        catch(java.io.IOException e) {
            System.err.println("Failed to write!"); // XXX: Improve
        }
    }
}
