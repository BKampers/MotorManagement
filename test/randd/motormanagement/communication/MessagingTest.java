/*
 * Copyright © Bart Kampers
 */

package randd.motormanagement.communication;

import bka.communication.*;

import org.json.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


public class MessagingTest {
    
    
    @BeforeClass
    public static void openPort() throws ChannelException, gnu.io.NoSuchPortException {
        /** 
         * Windows allows to open a serial port only once per application,
         * so it needs to be a static member to be used for all tests.
         */
        if (transporter == null) {
            Channel channel = createChannel(); 
            transporter = new Transporter(channel, "MessagingTest");
            transporter.open();
        }
    }
    
    
    @AfterClass
    public static void closePort() throws ChannelException {
        if (transporter != null) {
            transporter.close();
            transporter = null;
        }
    }
    
    
    @Test(timeout=50)
    public void notAMessage() throws InterruptedException {
        JSONObject message = new JSONObject();
        JSONObject response = receiveResponse(message);
        assertEquals("Notification", response.optString("Message"));
        assertEquals("Not a message", response.optString("Error"));
    }
    
    
    @Test(timeout=50)
    public void emptyMessage() throws JSONException, InterruptedException {
        JSONObject message = createMessage("", "Flash");
        JSONObject response = receiveResponse(message);
        assertEquals("Notification", response.optString("Message"));
        assertEquals("Unknown message", response.optString("Error"));
    }
    

    @Test(timeout=50)
    public void unknownMessage() throws JSONException, InterruptedException {
        JSONObject message = createMessage("Invalid", "Flash");
        JSONObject response = receiveResponse(message);
        assertEquals("Notification", response.optString("Message"));
        assertEquals("Unknown message", response.optString("Error"));
    }
    

    @Test(timeout=50)
    public void noSubject() throws JSONException, InterruptedException {
        JSONObject message = createMessage("Request", null);
        JSONObject response = receiveResponse(message);
        assertEquals("Request", response.optString("Response"));
        assertEquals("Invalid subject", response.optString("Error"));
    }
    
    
    @Test(timeout=50)
    public void emptySubject() throws JSONException, InterruptedException {
        JSONObject message = createMessage("Request", "");
        JSONObject response = receiveResponse(message);
        assertEquals("Request", response.optString("Response"));
        assertEquals("", response.optString("Subject"));
        assertEquals("Unknown subject", response.optString("Error"));
    }
    
    
    @Test(timeout=50)
    public void unknownSubject() throws JSONException, InterruptedException {
        JSONObject message = createMessage("Request", "Invalid");
        JSONObject response = receiveResponse(message);
        assertEquals("Request", response.optString("Response"));
        assertEquals("Invalid", response.optString("Subject"));
        assertEquals("Unknown subject", response.optString("Error"));
    }
    

    @Test(timeout=2000)
    public void validMessage() throws JSONException, InterruptedException {
        JSONObject message = createMessage("Request", "Flash");
        JSONObject response = receiveResponse(message);
        assertEquals("OK", response.optString("Status"));
        assertEquals("Request", response.optString("Response"));
        assertEquals("Flash", response.optString("Subject"));
    }
    
    
    @Test(timeout=200)
    public void modifyRpmSimulation() throws JSONException, InterruptedException {
        // Activate simulation
        JSONObject message = createMessage("Modify", "RPM");
        message.put("Simulation", true);
        message.put("Value", 5000);
        JSONObject response = receiveResponse(message);
        assertEquals("OK", response.optString("Status"));
        // Check value
        message = createMessage("Request", "RPM");
        response = receiveResponse(message);
        assertEquals(5000, response.optInt("Value"));
        message = createMessage("Modify", "RPM");
        // Deactivate simulation
        message.put("Simulation", false);
        response = receiveResponse(message);
        assertEquals("OK", response.optString("Status"));
    }
    
    
    @Test(timeout=1000)
    public void modifyTableField() throws JSONException, InterruptedException {
        // Get table
        JSONObject message = createMessage("Request", "Ignition");
        JSONObject response = receiveResponse(message);
        Integer row = (Integer) response.opt("Row");
        if (row == null) {
            row = 0;
        }
        Integer column = (Integer) response.opt("Column");
        if (column == null) {
            column = 0; 
        }
        JSONArray rows = response.optJSONArray("Table");
        assertNotNull(rows);
        assertTrue(row < rows.length());
        JSONArray columns = rows.optJSONArray(row);
        assertNotNull(columns);
        assertTrue(column < columns.length());
        int original = columns.optInt(column);
        // Modify  field
        message = createMessage("Modify", "Ignition");     
        message.put("Row", row);
        message.put("Column", column);
        message.put("Value", (original + 1) % 60);
        response = receiveResponse(message);
        assertEquals("OK", response.optString("Status"));
        message.put("Value", original);
        response = receiveResponse(message);
        assertEquals("OK", response.optString("Status"));
    }
    
    
    @Test(timeout=200)
    public void modifyTableEnabling() throws JSONException, InterruptedException {
        JSONObject message = createMessage("Modify", "WaterCorrection");
        message.put("Enabled", true);
        JSONObject response = receiveResponse(message);
        assertNotNull(response.get("Status"));
        assertTrue("OK".equals(response.get("Status")));
        message = createMessage("Request", "WaterCorrection");
        JSONArray properties = new JSONArray();
        properties.put("Enabled");
        message.put("Properties", properties);
        response = receiveResponse(message);
        assertTrue("OK".equals(response.get("Status")));
        assertTrue(response.getBoolean("Enabled"));
    }
    
    
    @Test(timeout=50)
    public void requestMeasurementTables() throws JSONException, InterruptedException {
        JSONObject message = createMessage("Request", "MeasurementTables");
        JSONObject response = receiveResponse(message);
        assertEquals("Request", response.opt("Response"));
        assertEquals("MeasurementTables", response.opt("Subject"));
        JSONArray names =  response.optJSONArray("Names");
        assertNotNull(names);
        assertTrue(names.length() > 0);
        for (int i = 0; i < names.length(); ++i) {
            assertNotNull(names.optString(i));
        }
    }
    
    
    @Test(timeout=750)
    public void requestTable() throws JSONException, InterruptedException {
        JSONObject message = createMessage("Request", "Ignition");
        JSONArray properties = new JSONArray();
        properties.put("Table");
        message.put("Properties", properties);
        JSONObject response = receiveResponse(message);
        assertNotNull(response.opt("Table"));
        assertNull(response.opt("Row"));
        assertNull(response.opt("Column"));
        assertNull(response.opt("Enabled"));
        properties.remove(0);
        properties.put("Index");
        response = receiveResponse(message);
        assertNull(response.opt("Table"));
        assertNotNull(response.opt("Row"));
        assertNotNull(response.opt("Column"));
        assertNull(response.opt("Enabled"));
        properties.remove(0);
        properties.put("Enabled");
        response = receiveResponse(message);
        assertNull(response.opt("Table"));
        assertNull(response.opt("Row"));
        assertNull(response.opt("Column"));
        assertTrue(response.getBoolean("Enabled"));
    }
    
    
    @Test(timeout=50)
    public void getEngineProperties() throws JSONException, InterruptedException {
        JSONObject message = createMessage("Request", "Engine");
        JSONObject response = receiveResponse(message);
        int cylinderCount = response.getInt("CylinderCount");
        assertTrue(1 <= cylinderCount && cylinderCount <= 8);
        JSONObject cogwheel = response.getJSONObject("Cogwheel");
        assertNotNull(cogwheel);
        assertNotNull(cogwheel.getInt("CogTotal"));
        assertNotNull(cogwheel.getInt("GapSize"));
        assertNotNull(cogwheel.getInt("Offset"));
        JSONArray deadPoints = response.getJSONArray("DeadPoints");
        assertNotNull(deadPoints);
        assertTrue(deadPoints.length() > 0);
    }
    
    
    @Test(timeout=200)
    public void setCogwheel() throws JSONException, InterruptedException {
        JSONObject message = createMessage("Modify", "Cogwheel");
        message.put("CogTotal", 60);
        message.put("GapSize", 2);
        message.put("Offset", 20);
        JSONObject response = receiveResponse(message);
        assertTrue("OK".equals(response.get("Status")) || "EngineIsRunning".equals(response.get("Status")));
        
        message = createMessage("Modify", "Cogwheel");
        message.put("CogTotal", 300);
        message.put("GapSize", 2500);
        message.put("Offset", 2000);
        response = receiveResponse(message);
        assertFalse("OK".equals(response.get("Status")));
    }
    
    
    @Test(timeout=200)
    public void setCylinderCount() throws JSONException, InterruptedException {
        JSONObject message = createMessage("Modify", "CylinderCount");
        message.put("Value", 6);
        JSONObject response = receiveResponse(message);
        assertTrue("OK".equals(response.get("Status")) || "EngineIsRunning".equals(response.get("Status")));
        
        message = createMessage("Modify", "CylinderCount");
        message.put("Value", 0);
        response = receiveResponse(message);
        assertFalse("OK".equals(response.get("Status")));
    }
    
    
    @Test(timeout=400)
    public void requestFlashElements() throws JSONException, InterruptedException {
        JSONObject message = createMessage("Request", "FlashElements");
        JSONObject response = receiveResponse(message);
        assertEquals("OK", response.get("Status"));
        JSONArray elements = response.getJSONArray("Elements");
        assertNotNull(elements);
        for (int i = 0; i < elements.length(); ++i) {
            JSONObject element = elements.getJSONObject(i);
            assertNotNull(element.getInt("TypeId"));
            assertNotNull(element.getInt("Reference"));
            assertNotNull(element.getInt("Size"));
        }
    }
    
    
    private JSONObject createMessage(String message, String subject) throws JSONException {
        JSONObject messageObject = new JSONObject();
        messageObject.put("Message", message);
        messageObject.put("Subject", subject);
        return messageObject;
    }
  
    
    private JSONObject receiveResponse(JSONObject messageObject) throws InterruptedException {
        transporter.send(messageObject);
        return transporter.nextReceivedObject();
    }
    
    
    private static Channel createChannel() throws gnu.io.NoSuchPortException {
        String channelName;
        try {
            java.util.Properties properties = new java.util.Properties();
            properties.load(new java.io.FileInputStream(new java.io.File("MessagingTest.properties")));
            channelName = properties.getProperty("Channel");
        }
        catch (java.io.IOException ex) {
            channelName = "localhost";
        }
        if (channelName.startsWith("COM")) {
            return SerialPortChannel.create(channelName);
        }
        else {
            return SocketChannel.create(channelName, 44252);
        }
    }


    private static Transporter transporter = null;
     
}
