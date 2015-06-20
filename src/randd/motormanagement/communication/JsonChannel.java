/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.communication;


import bka.communication.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
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
        StringBuilder builder = new StringBuilder(message.toString());
        builder.append(EOT);
        channel.send(builder.toString().getBytes());
    }
    
    
    JSONObject nextReceivedObject() throws InterruptedException {
          return receivedObjects.take();
    }
    
    
    private class ObjectReceiver implements ChannelListener {

        @Override
        public void receive(byte[] bytes) {
            logger.info(new String(bytes));
            for (int i = 0; i < bytes.length; ++i) {
                char character = (char) bytes[i];
                if (character != EOT) {
                    receivedData.append(character);
                }
                else {
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

        @Override
        public void handleException(Exception ex) {
            logger.log(Level.WARNING, "", ex);
        }
        
        private StringBuilder receivedData = new StringBuilder();
        
    }
    
    
    private final Channel channel;
    private final String applicationName;

    private ChannelListener objectReceiver = null;
    
    private final BlockingQueue<JSONObject> receivedObjects = new LinkedBlockingQueue<>();
    
    private static final Logger logger = Logger.getLogger(JsonChannel.class.getName());
    
    private static final char EOT = '~';
   
}