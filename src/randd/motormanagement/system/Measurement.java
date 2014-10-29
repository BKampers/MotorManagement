/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.system;


import java.util.ArrayList;


public class Measurement {

    
    public interface Listener {
        void valueUpdated();
        void simulationUpdated();
    }
    
    
    public static Measurement getInstance(String name) {
        assert name != null;
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
        this.value = value;    
        for (Listener listener : listeners) {
            listener.valueUpdated();
        }
    }
    
    
    public boolean isSimulationEnabled() {
        return simulationEnabled;
    }

    
    public void setSimulationEnabled(boolean simulationEnabled) {
        this.simulationEnabled = simulationEnabled;
        for (Listener listener : listeners) {
            listener.simulationUpdated();
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
    
    
    public String toString() {
        return name + " = " + value;
    }
    

    private Measurement(String name) {
        this.name = name;
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
