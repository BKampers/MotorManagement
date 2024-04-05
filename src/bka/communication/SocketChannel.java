/*
** Copyright © Bart Kampers
*/


package bka.communication;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SocketChannel extends Channel {

    
    private SocketChannel(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    
    public static SocketChannel create(String host, int port) {
        return new SocketChannel(host, port);
    }
    
    
    @Override
    public void open(String name) throws ChannelException {
        try {
            Socket socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            receiver = new Receiver();
            Thread receiverThread = new Thread(receiver);
            receiverThread.start();
        }
        catch (IOException ex) {
            throw new ChannelException(ex);
        }
    }

    
    @Override
    public void send(byte[] bytes) {
        out.println(new String(bytes));
    }
    
    
    @Override
    public void close() throws ChannelException {
        try {
            receiver.stop();
            out.close();
            in.close();
            super.close();
        }
        catch (IOException ex) {
            throw new ChannelException(ex);
        }
    }
    
    
    public String getHost() {
        return host;
    }
    
    
    public int getPort() {
        return port;
    }
    
    
    @Override
    public String toString() {
        return host;
    }
    
    
    private class Receiver implements Runnable {
        
        @Override
        public void run() {
            while (running) {
                try {
                    int count = in.read(buffer);
                    if (count > 0) {
                        byte[] bytes = new byte[count];
                        for (int i = 0; i < count && i < buffer.length; ++i) {
                            bytes[i] = (byte) buffer[i];
                        }
                        notifyListeners(bytes);
                    }
                }
                catch (IOException ex) {
                    Logger.getLogger(SocketChannel.class.getName()).log(Level.WARNING, Receiver.class.getName(), ex);
                    running = false;
                }
            }
        }
        
        void stop() {
            running = false;
        }
        
        private final char[] buffer = new char[1024];
        private volatile boolean running = true;
        
    }
    
    
    private final String host;
    private final int port;

    private PrintWriter out;
    private BufferedReader in;
    
    private Receiver receiver;
    
}
