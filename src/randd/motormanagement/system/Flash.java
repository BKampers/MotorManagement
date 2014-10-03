/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.system;

import java.util.*;


public class Flash {

    
    public interface Listener {
        void refreshed();
    }
    
    
    public static class Element {
        
        public Element(int typeId, int reference, int size) {
            this.typeId = typeId;
            this.reference = reference;
            this.size = size;
        }
        
        public int getTypeId() {
            return typeId;
        }
        
        public int getReference() {
            return reference;
        }
        
        public int getSize() {
            return size;
        }
        
        public String toString() {
            return String.format("Type: %d, Address %X - %X, Size: %d", typeId, reference, reference + size -1, size);
        }

        private final int typeId;
        private final int reference;
        private final int size;
    }
    
    
    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
        synchronized (listeners) {
            for (Listener listener : listeners) {
                listener.refreshed();
            }
        }
    }
    
    
    public boolean hasBytes() {
        return bytes != null;
    }
    
    
    public int getSize() {
        return (bytes != null) ? bytes.length : 0;
    }
    
    
    public byte getByteAt(int address) {
        return bytes[address];
    }
    
    
    public void setElements(Element[] elements) {
        this.elements = elements;
        synchronized (listeners) {
            for (Listener listener : listeners) {
                listener.refreshed();
            }
        }
    }

    
    public int getElementCount() {
        return (elements != null) ? elements.length : 0;
    }
    
    
    public Element getElement(int index) {
        return elements[index];
    }
    
    
    public void addListener(Listener listener) {
        synchronized (listeners) {
            if (! listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }
    
    
    public void removeListener(Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    
    private byte[] bytes;
    private Element[] elements;
    
    
    private final List<Listener> listeners = new ArrayList<>();
    
}
