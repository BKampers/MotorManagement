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
    
    
    public Double getValue() {
        return value;
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

    
    public void setSimulationValue(double simulationValue) {
        this.simulationValue = simulationValue;
        for (Listener listener : listeners) {
            listener.simulationUpdated();
        }
    }
    
    
    public void setValue(Double value) {
        this.value = value;    
        for (Listener listener : listeners) {
            listener.valueUpdated();
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
    private Double value = null;
    
    private boolean simulationEnabled = false;
    private double simulationValue = 0.0;

    private final ArrayList<Listener> listeners = new ArrayList<>();
    
    private static final java.util.Map<String, Measurement> instances = new java.util.HashMap<>();
}
