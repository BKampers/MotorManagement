/*
 * Copyright © Bart Kampers
 */

package randd.motormanagement.communication;

import bka.communication.SerialPortChannel;

import org.json.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


public class MessagingTest {
    
    
    @BeforeClass
    public static void openPort() throws javax.comm.NoSuchPortException, bka.communication.ChannelException {
        /** 
         * Windows allows to open a serial port only once per application,
         * so it needs to be a static member to be used for all tests.
         */
        if (serialPort == null) {
            SerialPortChannel channel = SerialPortChannel.create(PORT_NAME); 
            serialPort = new SerialPort(channel, "MessagingTest");
            serialPort.open();
        }
    }
    
    
    @AfterClass
    public static void closePort() {
        if (serialPort != null) {
            serialPort.close();
            serialPort = null;
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
        assertEquals("No subject", response.optString("Error"));
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
    
    
    @Test(timeout=100)
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
        assertNotNull(row);
        Integer column = (Integer) response.opt("Column");
        assertNotNull(column);
        JSONArray rows = response.optJSONArray("Table");
        assertNotNull(rows);
        assertTrue(row < rows.length());
        JSONArray columns = rows.optJSONArray(row);
        assertNotNull(columns);
        assertTrue(column < columns.length());
        int original = columns.optInt(column);
        // Modify active field
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
        properties.remove(0);
        properties.put("Index");
        response = receiveResponse(message);
        assertNull(response.opt("Table"));
        assertNotNull(response.opt("Row"));
        assertNotNull(response.opt("Column"));
    }
    
    
    private JSONObject createMessage(String message, String subject) throws JSONException {
        JSONObject messageObject = new JSONObject();
        messageObject.put("Message", message);
        messageObject.put("Subject", subject);
        return messageObject;
    }
  
    
    private JSONObject receiveResponse(JSONObject messageObject) throws InterruptedException {
        serialPort.send(messageObject);
        return serialPort.nextReceivedObject();
    }


    private static SerialPort serialPort = null;
    
    private static final String PORT_NAME = "COM5";
     
}
