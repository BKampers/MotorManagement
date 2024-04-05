/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.system;


public class TimerSettings {

    
    public int getPrescaler() {
        return prescaler;
    }

    
    public void setPrescaler(int prescaler) {
        this.prescaler = prescaler;
    }

    
    public int getPeriod() {
        return period;
    }

    
    public void setPeriod(int period) {
        this.period = period;
    }

    
    public int getCounter() {
        return counter;
    }

    
    public void setCounter(int counter) {
        this.counter = counter;
    }

    
    private int prescaler;
    private int period;
    private int counter;
    
}
