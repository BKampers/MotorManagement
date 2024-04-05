package bka.communication;


import gnu.io.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;


public class SerialPortChannel extends Channel
{

    private SerialPortChannel(CommPortIdentifier commPortIdentifier) {
        this.commPortIdentifier = commPortIdentifier;
    }
    
    
    public static SerialPortChannel create(CommPortIdentifier commPortIdentifier) {
        return new SerialPortChannel(commPortIdentifier);
    }
        
    
    public static SerialPortChannel create(String portName) throws ChannelException {
        try {
            return create(CommPortIdentifier.getPortIdentifier(portName));
        }
        catch (NoSuchPortException ex) {
            Logger.getLogger(SerialPortChannel.class.getName()).log(Level.SEVERE, null, ex);
            throw new ChannelException(ex);
        }
    }
        
    
    @Override
    public void open(String name) throws ChannelException {
        try {
            if (port == null) {
                port = (SerialPort) commPortIdentifier.open(name, 2000);
            }
            port.setSerialPortParams(
                19200, 
                SerialPort.DATABITS_8, 
                SerialPort.STOPBITS_1, 
                SerialPort.PARITY_NONE);
            port.addEventListener(new Receiver());
            port.notifyOnDataAvailable(true);
            inputStream = port.getInputStream();
            outputStream = port.getOutputStream();
        }
        catch (PortInUseException | UnsupportedCommOperationException | TooManyListenersException | IOException ex) {
            throw new ChannelException(ex);
        }
    }
    
    
    public void setBaud(int baud) throws ChannelException {
        try {
            port.setSerialPortParams(
                baud, 
                SerialPort.DATABITS_8, 
                SerialPort.STOPBITS_1, 
                SerialPort.PARITY_NONE);
        }
        catch (Exception ex) {
            throw new ChannelException(ex);
        }
    }
    
    
    public int getBaud() throws ChannelException {
        try {
            return port.getBaudRate();
        }
        catch (NullPointerException ex) {
            throw new ChannelException(ex);
        }
    }
    
    
    @Override
    public void close() throws ChannelException {
        if (port != null) {
            port.removeEventListener();
            port.close();
            port = null;
        }
        inputStream = null;
        outputStream = null;
        super.close();
    }
    
    
    @Override
    public synchronized void send(byte[] bytes) {
        try {
            outputStream.write(bytes);
        }
	catch (IOException e) {
	    notifyListeners(e);
        }            
    }
    
    
    @Override
    public String toString() {
        return commPortIdentifier.getName();
    }
    
    
    public static Collection<SerialPortChannel> findAll() throws ChannelException {
        try {
            Collection<SerialPortChannel> all = new ArrayList<>();
            Enumeration portList = CommPortIdentifier.getPortIdentifiers();
            while (portList.hasMoreElements()) {
                CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
                if (portId.getName().startsWith("COM") || portId.getName().startsWith("/dev/cu.usbserial")) {
                    all.add(new SerialPortChannel(portId));
                }
            }
            return all;
        }
        catch (Throwable t) {
            throw new ChannelException(t);
        }
    }
    
    
    private class Receiver implements SerialPortEventListener {

        @Override
        public void serialEvent(SerialPortEvent evt) {
            try {
                int available = inputStream.available();
                byte[] received = new byte[available];
                inputStream.read(received, 0, available);
                notifyListeners(received);
            }
            catch (IOException ex) {
                notifyListeners(ex);
            }
        }
        
    }


    private final CommPortIdentifier commPortIdentifier;
    private SerialPort port;
    private InputStream inputStream;
    private OutputStream outputStream;
        
}
