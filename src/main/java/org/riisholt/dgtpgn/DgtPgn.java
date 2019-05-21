package org.riisholt.dgtpgn;

import org.openmuc.jrxtx.*;

import org.riisholt.dgtdriver.*;

public class DgtPgn {
    public static void main(String[] argv) throws java.io.IOException {
        // TODO: Validate args.

        new DgtPgn(argv[0]).run();
    }

    private SerialPort port;
    private DgtDriver driver;
    private DgtPgn(String portName) throws java.io.IOException {
        System.out.println(portName);
        port = SerialPortBuilder.newBuilder(portName)
                                .setDataBits(DataBits.DATABITS_8)
                                .setBaudRate(9600)
                                .setStopBits(StopBits.STOPBITS_1)
                                .setParity(Parity.NONE)
                                .setFlowControl(FlowControl.NONE)
                                .build();
        driver = new DgtDriver((msg) -> gotMessage(msg), (bytes) -> writeBytes(bytes));
    }

    private void run() {
        System.out.println("Hello world?");
    }

    public void gotMessage(DgtMessage msg) {
        // TODO
    }

    public void writeBytes(byte[] bytes) {
        try {
            port.getOutputStream().write(bytes);
        }
        catch(java.io.IOException e) {
            System.err.println("Failed to write!"); // XXX: Improve
        }
    }
}
