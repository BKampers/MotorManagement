/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.system;


import java.util.ArrayList;


public class Measurement {

    
    public enum Property {
        TABLE_ENABLED,
        SIMULATION_VALUE,
        SIMULATION_ENABLED  
    }
    
    
    public interface Listener {
        void valueUpdated();
        void simulationUpdated();
    }
    
    
    public static Measurement getInstance(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException();
        }
        Measurement instance = instances.get(name);
        if (instance == null) {
            instance = new Measurement(name);
            instances.put(name, instance);
        }
        return instance;
    }
    
    
    public String getName() {
        return name;
    }

    
    public String getFormat() {
        return format;
    }

    
    public void setFormat(String format) {
        this.format = format;
    }

    
    public float getMinimum() {
        return minimum;
    }
    
    
    public void setMinimum(float minimum) {
        this.minimum = minimum;
    }
    
    
    public float getMaximum() {
        return maximum;
    }

    
    public void setMaximum(float maximum) {
        this.maximum = maximum;
    }
    
    
    public Float getValue() {
        return value;
    }

    
    public void setValue(Float value) {
        if (! equal(value, this.value)) {
            this.value = value;    
            for (Listener listener : listeners) {
                listener.valueUpdated();
            }
        }
    }
    
    
    public boolean isSimulationEnabled() {
        return simulationEnabled;
    }

    
    public void setSimulationEnabled(boolean simulationEnabled) {
        if (this.simulationEnabled != simulationEnabled) {
            this.simulationEnabled = simulationEnabled;
            for (Listener listener : listeners) {
                listener.simulationUpdated();
            }
        }
    }

    
    public double getSimulationValue() {
        return simulationValue;
    }

    
    public void setSimulationValue(float simulationValue) {
        this.simulationValue = simulationValue;
        for (Listener listener : listeners) {
            listener.simulationUpdated();
        }
    }
    
    
    public void addListener(Listener listener) {
        if (! listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    

    @Override
    public String toString() {
        return name + " = " + value;
    }
    

    private Measurement(String name) {
        this.name = name;
    }

    
    private boolean equal(Object o1, Object o2) {
        return (o1 == null) ? o2 == null : o1.equals(o2); 
    }
    
    
    private final String name;
    
    private float minimum = 0.0f;
    private float maximum = 100.0f;
    
    private Float value;
    
    private String format;
    
    private boolean simulationEnabled = false;
    private float simulationValue = 0.0f;

    private final ArrayList<Listener> listeners = new ArrayList<>();
    
    private static final java.util.Map<String, Measurement> instances = new java.util.HashMap<>();
}
