package bka.communication;


import java.util.*;

/**
 * This class sends commands through any channel to which an ASE system is connected 
 * and listens to it for receiving responses. ChannelListeners are notified when a 
 * command has arrived.
 * 
 * This class must be extended for different types of channels such as serial and 
 * parallel ports.
 */
public abstract class Channel
{
    
    abstract public void open(String name) throws ChannelException;
    abstract public void send(byte[] bytes);
     

    public void addListener(ChannelListener listener) {
        if (! listeners.contains(listener)) {
            listeners.addElement(listener);
        }
    }


    public void removeListener(ChannelListener listener) {
        listeners.removeElement(listener);
    }

    
    public void close() throws ChannelException {
        listeners.removeAllElements();
    }

   
    protected void notifyListeners(byte[] bytes) {
        Enumeration en = listeners.elements();
        while (en.hasMoreElements()) {
            ((ChannelListener) en.nextElement()).receive(bytes);
        }
    }
    
    
    protected void notifyListeners(Exception e) {
        Enumeration en = listeners.elements();
        while (en.hasMoreElements()) {
            ((ChannelListener) en.nextElement()).handleException(e);
        }
    }


    private Vector listeners = new Vector();

}