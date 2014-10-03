/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.communication;


import bka.communication.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;


public class SerialPort {

    
    public SerialPort(SerialPortChannel channel, String applicationName) {
        this.channel = channel;
        this.applicationName = applicationName;
    }
    
    
    public String getName() {
        return channel.toString();
    }
    
    
//    public static Collection<SerialPortChannel> findAll() {
//        Collection<SerialPortChannel> ports = new ArrayList<>();
//        Collection channels = SerialPortChannel.findAll();
//        Iterator en = channels.iterator();
//        while (en.hasNext()) {
//            Channel channel = (Channel) en.next();
//            if (channel instanceof SerialPortChannel) {
//                ports.add((SerialPortChannel) channel);
//            }
//        }
//        return ports;
//    }
    
    
    void open() throws ChannelException {
        channel.open(applicationName);
        channel.setBaud(115200);
        channelListener = new JsonChannelListener();
        channel.addListener(channelListener);
    }
    
    
    void close() {
        receivedObjects.add(new JSONObject()); // deblock thread waiting in nextReceivedObject
        channel.removeListener(channelListener);
        channelListener = null;
        channel.close();
    }
    
    
    void send(JSONObject message) {
        channel.send(message.toString().getBytes());
    }
    
    
    JSONObject nextReceivedObject() throws InterruptedException {
          return receivedObjects.take();
    }
    
    
    private class JsonChannelListener implements ChannelListener {

        public void receive(byte[] bytes) {
            for (int i = 0; i < bytes.length; ++i) {
                char character = (char) bytes[i];
                if (character == '{') {
                    bracketCount++;
                }
                if (bracketCount > 0) {
                    receivedData.append(character);
                    if (character == '}') {
                        bracketCount--;
                    }
                    if (bracketCount == 0) {
                        try {
                            receivedObjects.add(new JSONObject(receivedData.toString()));
                            receivedData = new StringBuilder();
                        }
                        catch (org.json.JSONException ex) {
                            handleException(ex);
                        }
                    }
                }
            }
        }

        public void handleException(Exception ex) {
            ex.printStackTrace(System.err);
        }
        
        private StringBuilder receivedData = new StringBuilder();
        private int bracketCount = 0;
        
    }
    
    
    private final SerialPortChannel channel;
    private final String applicationName;

    private ChannelListener channelListener = null;
    
    private final BlockingQueue<JSONObject> receivedObjects = new LinkedBlockingQueue<>();
   
}