/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.system;

import java.util.*;


public class Engine {

    
    public class Cogwheel {

        public int getCogTotal() {
            return cogTotal;
        }

        public int getGapSize() {
            return gapSize;
        }

        public int getOffset() {
            return offset;
        }

        private int cogTotal;
        private int gapSize;
        private int offset;
        
    }

    
    public enum Property { IS_RUNNING, COGWHEEL, CYLINDER_COUNT, DEAD_POINTS }
    
    
    public interface Listener {
        void propertyChanged(Engine engine, Property property);
    }
    
    
    public boolean isRunning() {
        return running;
    } 
    
    
    public void setRunning(boolean running) {
        if (this.running != running) {
            this.running = running;
            notifyPropertyChanged(Property.IS_RUNNING);
        }
    }
    
    
    public Cogwheel getCogwheel() {
        return cogwheel;
    }
    
    
    public void setCogwheel(int cogTotal, int gapSize, int offset) {
        if (cogwheel.cogTotal != cogTotal || cogwheel.gapSize != gapSize || cogwheel.offset != offset) {
            cogwheel.cogTotal = cogTotal;
            cogwheel.gapSize = gapSize;
            cogwheel.offset = offset;
            notifyPropertyChanged(Property.COGWHEEL);
        }
    }

    
    public int getCylinderCount() {
        return cylinderCount;
    }

    
    public void setCylinderCount(int cylinderCount) {
        if (this.cylinderCount != cylinderCount) {
            this.cylinderCount = cylinderCount;
            notifyPropertyChanged(Property.CYLINDER_COUNT);
        }
    }

    
    public List<Integer> getDeadPoints() {
        return (deadPoints != null) ? new ArrayList<>(deadPoints) : null;
    }

    
    public void setDeadPoints(List<Integer> deadPoints) {
        this.deadPoints = (deadPoints != null) ? new ArrayList<>(deadPoints) : null;
            notifyPropertyChanged(Property.DEAD_POINTS);
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
    
    
    private void notifyPropertyChanged(Property property) {
        synchronized (listeners) {
            for (Listener listener : listeners) {
                listener.propertyChanged(this, property);
            }        
        }
    }
    
    
    private boolean running;
    private final Cogwheel cogwheel = new Cogwheel();
    private int cylinderCount;
    private List<Integer> deadPoints;
    
    
    private final Collection<Listener> listeners = new ArrayList<>();
    
}
