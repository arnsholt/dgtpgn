package org.riisholt.dgtpgn;

import com.fazecast.jSerialComm.SerialPort;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.riisholt.dgtdriver.DgtDriver;
import org.riisholt.dgtdriver.DgtMessage;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

public abstract class Program {
    private SerialPort port;
    private FileWriter debugOut;
    private String outputPrefix;
    private DgtDriver driver;
    private long startNanos;

    public Program(String portName, String prefix, boolean debug) throws IOException {
        System.out.printf("Connecting to %s\n", portName);
        outputPrefix = prefix;
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(9600);
        port.setParity(SerialPort.NO_PARITY);
        port.setNumDataBits(8);
        port.setNumStopBits(1);
        if(!port.openPort())
            throw new RuntimeException(String.format("Failed to open port %s.", portName));

        driver = new DgtDriver(this::gotMessage, this::writeBytes);

        if(debug) {
            debugOut = new FileWriter(String.format("%s.debug", prefix));//new FileOutputStream(String.format("%s.debug", outputPrefix));
            startNanos = System.nanoTime();
        }
    }

    protected String getOutputPrefix() { return outputPrefix; }
    protected boolean isDebug() { return debugOut != null; }

    protected abstract void initDriver(DgtDriver driver);
    protected abstract void gotMessage(DgtMessage msg);
    protected void shutdownHook() {}

    protected void run() throws IOException, InterruptedException {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));
        this.initDriver(driver);
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

    protected void writeBytes(byte[] bytes) {
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

    protected void debugWrite(boolean isInput, byte[] bytes) throws IOException {
        if(debugOut == null) return;

        long timeDelta = System.nanoTime() - startNanos;
        char direction = isInput ? '<' : '>';
        String byteString = new String(Base64.getEncoder().encode(bytes));
        debugOut.write(String.format("%d %c %s", timeDelta, direction, byteString));
        debugOut.flush();
    }

    protected static ArgumentParser defaultArgumentParser(String forName) {
        ArgumentParser parser = ArgumentParsers.newFor(forName).build()
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

        return parser;
    }
}
