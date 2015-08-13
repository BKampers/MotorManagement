/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package randd.motormanagement.system;

import java.util.*;

import org.junit.Test;
import static org.junit.Assert.*;


public class EngineTest {
        
    @Test
    public void testIsRunning() {
        Engine instance = new Engine();
        Boolean expResult = null;
        Boolean result = instance.isRunning();
        assertEquals(expResult, result);
    }

    
    @Test
    public void testSetRunning() {
        boolean running = false;
        Engine instance = new Engine();
        instance.setRunning(running);
        assertFalse(instance.isRunning());
    }

    
    @Test
    public void testGetCogwheel() {
        Engine instance = new Engine();
        Engine.Cogwheel expResult = null;
        Engine.Cogwheel result = instance.getCogwheel();
        assertEquals(expResult, result);
    }

    
    @Test
    public void testSetCogwheel() {
        int cogTotal = 60;
        int gapSize = 2;
        int offset = 20;
        Engine instance = new Engine();
        instance.setCogwheel(cogTotal, gapSize, offset);
        Engine.Cogwheel cogwheel = instance.getCogwheel();
        assertEquals(cogTotal, cogwheel.getCogTotal());
        assertEquals(gapSize, cogwheel.getGapSize());
        assertEquals(offset, cogwheel.getOffset());
    }

    
    @Test
    public void testGetCylinderCount() {
        Engine instance = new Engine();
        assertNull(instance.getCylinderCount());
    }

    
    @Test
    public void testSetCylinderCount() {
        int cylinderCount = 4;
        Engine instance = new Engine();
        instance.setCylinderCount(cylinderCount);
        assertEquals(Integer.valueOf(cylinderCount), instance.getCylinderCount());
    }

    
    @Test
    public void testGetDeadPoints() {
        Engine instance = new Engine();
        List<Integer> expResult = null;
        List<Integer> result = instance.getDeadPoints();
        assertEquals(expResult, result);
    }

    
    @Test
    public void testSetDeadPoints() {
        List<Integer> deadPoints = new ArrayList<>();
        deadPoints.add(5);
        deadPoints.add(35);
        int size = deadPoints.size();
        Engine instance = new Engine();
        instance.setDeadPoints(deadPoints);
        List<Integer> result = instance.getDeadPoints();
        assertEquals(size, result.size());
        for (int i = 0; i < size; ++i) {
            assertEquals(deadPoints.get(i), result.get(i));
        }
    }


    @Test
    public void testListener() {
        final List<Engine.Property> notifiedProperties = new ArrayList<>();
        Engine.Listener listener = new Engine.Listener() {
            @Override
            public void propertyChanged(Engine engine, Engine.Property property) {
                notifiedProperties.add(property);
            }
        };
        Engine instance = new Engine();
        instance.addListener(listener);
        instance.setCogwheel(36, 1, 5);
        assertTrue(notifiedProperties.contains(Engine.Property.COGWHEEL));
        notifiedProperties.clear();
        instance.setCylinderCount(6);
        assertTrue(notifiedProperties.contains(Engine.Property.CYLINDER_COUNT));
        notifiedProperties.clear();
        instance.setDeadPoints(new ArrayList<Integer>());
        assertTrue(notifiedProperties.contains(Engine.Property.DEAD_POINTS));
        notifiedProperties.clear();
        instance.setRunning(true);
        assertTrue(notifiedProperties.contains(Engine.Property.IS_RUNNING));
        instance.removeListener(listener);
    }
    
}
