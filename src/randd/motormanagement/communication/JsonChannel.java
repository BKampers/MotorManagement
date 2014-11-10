/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.communication;


import bka.communication.*;
import java.util.concurrent.*;
import org.json.*;


public class JsonChannel {

    
    public JsonChannel(Channel channel, String applicationName) {
        this.channel = channel;
        this.applicationName = applicationName;
    }
    
    
    public String getName() {
        return channel.toString();
    }
    
    
    void open() throws ChannelException {
        channel.open(applicationName);
        if (channel instanceof SerialPortChannel) {
            ((SerialPortChannel) channel).setBaud(115200);
        }
        objectReceiver = new ObjectReceiver();
        channel.addListener(objectReceiver);
    }
    
    
    void close() throws ChannelException {
        receivedObjects.add(new JSONObject()); // deblock thread waiting in nextReceivedObject
        channel.removeListener(objectReceiver);
        objectReceiver = null;
        channel.close();
    }
    
    
    void send(JSONObject message) {
        channel.send(message.toString().getBytes());
    }
    
    
    JSONObject nextReceivedObject() throws InterruptedException {
          return receivedObjects.take();
    }
    
    
    private class ObjectReceiver implements ChannelListener {

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
    
    
    private final Channel channel;
    private final String applicationName;

    private ChannelListener objectReceiver = null;
    
    private final BlockingQueue<JSONObject> receivedObjects = new LinkedBlockingQueue<>();
   
}