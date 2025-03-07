/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.communication;


import bka.communication.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.*;


public class Transporter {

    
    public Transporter(Channel channel, String applicationName) {
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
        builder.append(TRANSMISSION_END);
        channel.send(builder.toString().getBytes());
    }
    
    
    JSONObject nextReceivedObject() throws InterruptedException {
          return receivedObjects.take();
    }
    
    
    private class ObjectReceiver implements ChannelListener {

        @Override
        public void receive(byte[] bytes) {
            LOGGER.log(Level.FINE, new String(bytes));
            for (int i = 0; i < bytes.length; ++i) {
                char character = (char) bytes[i];
                if (character != TRANSMISSION_END) {
                    receivedCharacters.append(character);
                }
                else {
                    processReceivedCharacters();
                }
            }
        }

        private void processReceivedCharacters() {
            try {
                receivedObjects.add(new JSONObject(receivedCharacters.toString()));
                receivedCharacters.setLength(0);
            }
            catch (org.json.JSONException ex) {
                handleException(ex);
            }
        }

        @Override
        public void handleException(Exception ex) {
            LOGGER.log(Level.WARNING, "ObjectReceiver", ex);
        }
        
        private final StringBuilder receivedCharacters = new StringBuilder();
        
    }
    
    
    private final Channel channel;
    private final String applicationName;

    private ChannelListener objectReceiver = null;
    
    private final BlockingQueue<JSONObject> receivedObjects = new LinkedBlockingQueue<>();
    
    private static final Logger LOGGER = Logger.getLogger(Transporter.class.getName());
    
    private static final char TRANSMISSION_END = '\n';
   
}