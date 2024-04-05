/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.system;


public class Notification {

    
    public Notification(String name, String value) {
        this.timestamp = System.currentTimeMillis();
        this.name = name;
        this.value = value;
    }
    
    
    public long getTimestamp() {
        return timestamp;
    }

    
    public String getName() {
        return name;
    }

    
    public String getValue() {
        return value;
    }

    
    private final long timestamp;
    private final String name;
    private final String value;
    
}
