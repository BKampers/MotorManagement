/*
 * Copyright © Bart Kampers
 */

package randd.motormanagement.communication;

import bka.communication.*;
import java.io.*;
import java.util.Iterator;
import java.util.logging.*;

import org.json.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;


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
    
    
    @Test(timeout=1000)
    public void invalidMessages() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject();
        JSONObject response = receiveResponse(message);
        assertEquals("Fire", response.getString("Direction"));
        assertEquals("InvalidMessageReceived", response.getString("Status"));
        message = new JSONObject("{\"Direction\" : \"x\"}");
        response = receiveResponse(message);
        assertEquals("Fire", response.getString("Direction"));
        assertEquals("InvalidDirection", response.getString("Status"));
        message = new JSONObject("{\"Direction\"=\"Call\"}");
        response = receiveResponse(message);
        assertEquals("Return", response.getString("Direction"));
        assertEquals("NoFunction", response.get("Status"));
    }
    
    
    @Test(timeout=1000)
    public void invalidFunction() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject(
            "{" +
                "\"Direction\" : \"Call\"," +
                "\"Function\"  : \"Xxx\"," +
                "\"Parameters\": {}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals("UnknownFunction", response.getString("Status"));
    }
    
    
    @Test(timeout=1000)
    public void invalidTablename() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetTableEnabled\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"Xxx\"," +
                    "\"Enabled\"   : false" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals("InvalidId", response.getString("Status"));
    }
    
    
    @Test(timeout=1000)
    public void invalidParameter() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetTableEnabled\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"AirCorrection\"," +
                    "\"Enabled\"   : 123" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals("InvalidParameter", response.getString("Status"));
        assertTrue(response.isNull("ReturnValue"));
        message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetTableEnabled\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"AirCorrection\"," +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals("InvalidParameter", response.getString("Status"));
        assertTrue(response.isNull("ReturnValue"));
        message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetTableEnabled\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"Enabled\" : false" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals("InvalidParameter", response.getString("Status"));
        assertTrue(response.isNull("ReturnValue"));
    }
    
    
    @Test(timeout=1000)
    public void getMeasurements() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject(
            "{" +
                "\"Direction\" : \"Call\"," +
                "\"Function\" : \"GetMeasurements\"" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        JSONObject value = response.getJSONObject("ReturnValue");
        Iterator names = value.keys();
        while (names.hasNext()) {
            String measurmentName = (String) names.next();
            JSONObject measurement = value.getJSONObject(measurmentName);
            Object measurementValue = measurement.opt("Value");
            assertTrue(measurementValue == JSONObject.NULL || measurementValue instanceof Number);
            assertNotNull(measurement.optBoolean("Simulation"));
        }
    }
    
    
    @Test(timeout=1000)
    public void getMeasurementProperties() throws JSONException, InterruptedException {
        // Valid name
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetMeasurementProperties\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"MeasurementName\" : \"Load\"" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.get(STATUS));
        JSONObject value = response.getJSONObject("ReturnValue");
        assertNotNull(value.getDouble("Minimum"));
        assertNotNull(value.getDouble("Maximum"));
        assertNotNull(value.getString("Format"));
        Object simulationValue = value.get("SimulationValue");
        assertTrue(simulationValue == JSONObject.NULL || simulationValue instanceof Number);
        // Invalid name
        message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetMeasurementProperties\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"MeasurementName\" : \"Xxx\"" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals("NoSuchMeasurement", response.get("Status"));
    }
    
    
     @Test(timeout=1000)
    public void setMeasurementSimulation() throws JSONException, InterruptedException {
        // Activate simulation
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetMeasurementSimulation\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"MeasurementName\" : \"RPM\"," +
                    "\"SimulationValue\" : 2000" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.optString(STATUS));
        assertTrue(response.getJSONObject("ReturnValue").getBoolean("Simulation"));
        assertEquals(2000.0, response.getJSONObject("ReturnValue").getDouble("SimulationValue"), 0.1);
        // Check value
        message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetMeasurements\"" +
            "}");                   
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.optString(STATUS));
        JSONObject value = response.getJSONObject("ReturnValue");
        Iterator names = value.keys();
        while (names.hasNext()) {
            String measurementName = (String) names.next();
            if ("RPM".equals(measurementName)) {
                JSONObject measurement = value.getJSONObject(measurementName);
                Number measurementValue = (Number) measurement.opt("Value");
                assertTrue(measurement.getBoolean("Simulation"));
                assertEquals(2000, Math.round(measurementValue.floatValue()));
            }
        }
        // Deactivate simulation
        message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"ResetMeasurementSimulation\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"MeasurementName\" : \"RPM\"" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.optString(STATUS));
        assertFalse(response.getJSONObject("ReturnValue").getBoolean("Simulation"));
        // Check value
        message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetMeasurements\"" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.optString(STATUS));
        value = response.getJSONObject("ReturnValue");
        names = value.keys();
        while (names.hasNext()) {
            String measurementName = (String) names.next();
            if ("RPM".equals(measurementName)) {
                JSONObject measurement = value.getJSONObject(measurementName);
                assertFalse(measurement.getBoolean("Simulation"));
            }
        }
    }
    
    
    @Test(timeout=1000)
    public void getTableNames() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject(
            "{" +
                "\"Direction\" : \"Call\"," +
                "\"Function\" : \"GetTableNames\"" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        JSONArray names = response.getJSONArray("ReturnValue");
        for (int i = 0; i < names.length(); ++i) {
            String tableName = names.getString(i);
            assertFalse(tableName.isEmpty());
        }
    }
    
    
    @Test(timeout=1000)
    public void getTableActualValues() throws JSONException, InterruptedException {
        // Valid name
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetTableActualValues\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"Injection\"" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.get(STATUS));
        JSONObject value = response.getJSONObject("ReturnValue");
        assertNotNull(value.getInt("CurrentColumn"));        
        assertNotNull(value.getInt("CurrentRow"));
        // Invalid name
        message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetTableProperties\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"Xxx\"" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals("NoSuchMeasurementTable", response.get("Status"));
        // Invalid parameter
        message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetTableProperties\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"Xxx\" : \"Injection\"" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals("InvalidParameter", response.get("Status"));
    }


    @Test(timeout=1000)
    public void getTableProperties() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetTableProperties\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"Ignition\"" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.get(STATUS));
        JSONObject value = response.getJSONObject("ReturnValue");
        assertNotNull(value.getDouble("Minimum"));
        assertNotNull(value.getDouble("Maximum"));
        assertNotNull(value.getDouble("Precision"));
        assertNotNull(value.getInt("Decimals"));
        assertNotNull(value.getString("ColumnMeasurementName"));
        assertNotNull(value.getString("RowMeasurementName"));
    }
    
    
    @Test(timeout=1000)
    public void getTableFields() throws JSONException, InterruptedException {
        // Valid name
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetTableFields\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"Injection\"" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.get(STATUS));
        JSONArray rows = response.getJSONObject("ReturnValue").getJSONArray("Fields");
        for (int row = 0; row < rows.length(); ++row) {
            JSONArray columns = rows.getJSONArray(row);
            for (int column = 0; column < columns.length(); ++column) {
                assertNotNull(columns.optDouble(column));
            }
        }
    }
    
    
    @Test(timeout=1000)
    public void setTableField() throws JSONException, InterruptedException {
        // Get values
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetTableFields\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"Ignition\"" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        JSONObject returnValue = response.getJSONObject("ReturnValue");
        assertEquals("Ignition", returnValue.getString("TableName"));
        double value = returnValue.getJSONArray("Fields").getJSONArray(0).getDouble(0);
        // Modify value
        double newValue = value + 1.0;
        message = new JSONObject(
            "{" +
                "\"Direction\" : \"Call\"," +
                "\"Function\"  : \"SetTableField\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"Ignition\"," +
                    "\"Column\"    : 0," +
                    "\"Row\"       : 0," +
                    "\"Value\"     : " + Double.toString(newValue) +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.optString(STATUS));
        message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetTableFields\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"Ignition\"" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        returnValue = response.getJSONObject("ReturnValue");
        value = returnValue.getJSONArray("Fields").getJSONArray(0).getDouble(0);
        assertEquals(Math.round(newValue), Math.round(value));
    }
    
    
    @Test(timeout=1000)
    public void setTableEnabled() throws JSONException, InterruptedException {
        // Enable WaterCorrection
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetTableEnabled\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"WaterCorrection\"," +
                    "\"Enabled\"   : true" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.opt(STATUS));
        // Check enabled
        message = new JSONObject(
            "{" +
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetTableProperties\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"WaterCorrection\"" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertTrue(response.getJSONObject("ReturnValue").getBoolean("Enabled"));
        // Disable WaterCorrection
        message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetTableEnabled\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"WaterCorrection\"," +
                    "\"Enabled\"   : false" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.opt(STATUS));
        // Check disabled
        message = new JSONObject(
            "{" +
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetTableProperties\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"WaterCorrection\"" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertFalse(response.getJSONObject("ReturnValue").getBoolean("Enabled"));
    }
    
    
    @Test(timeout=1000)
    public void getEngineProperties() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject(
            "{" +
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetEngineProperties\"" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        int cylinderCount = response.getJSONObject("ReturnValue").getInt("CylinderCount");
        assertTrue(4 == cylinderCount || 6 == cylinderCount || 8 == cylinderCount);
        assertNotNull(response.getJSONObject("ReturnValue").getJSONObject("Cogwheel").getInt("CogTotal"));
        assertNotNull(response.getJSONObject("ReturnValue").getJSONObject("Cogwheel").getInt("GapSize"));
        assertNotNull(response.getJSONObject("ReturnValue").getJSONObject("Cogwheel").getInt("Offset"));
        JSONArray deadPoints = response.getJSONObject("ReturnValue").getJSONArray("DeadPoints");
        assertEquals(cylinderCount / 2, deadPoints.length());
    }
    
    
    @Test(timeout=1000)
    public void setCogwheel() throws JSONException, InterruptedException {
        // Valid cogwheel
        JSONObject message = new JSONObject(
            "{" +
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetCogwheelProperties\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"CogTotal\" : 60," +
                    "\"GapSize\"  :  2," +
                    "\"Offset\"   : 20" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        String status = response.getString(STATUS);
        assertTrue(OK_STATUS.equals(status) || ENGINE_RUNNING_STATUS.equals(status));
        // Invalid cogwheel
        message = new JSONObject(
            "{" +
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetCogwheelProperties\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"CogTotal\" :  300," +
                    "\"GapSize\"  : 2500," +
                    "\"Offset\"   : 2000" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertFalse(OK_STATUS.equals(response.get(STATUS)));
    }
    
    
    @Test(timeout=1000)
    public void setCylinderCount() throws JSONException, InterruptedException {
        // Valid cylinder count
        JSONObject message = new JSONObject(
            "{" +
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetCylinderCount\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"CylinderCount\" : 6" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        String status = response.getString(STATUS);
        assertTrue(OK_STATUS.equals(status) || ENGINE_RUNNING_STATUS.equals(status));
        // Invalid cylinder count
        message = new JSONObject(
            "{" +
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetCylinderCount\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"CylinderCount\" : 0" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertFalse(OK_STATUS.equals(response.get(STATUS)));
    }
    
    
    @Test(timeout=1000)
    public void getPersistentElements() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject(
           "{" +
                "\"Direction\" : \"Call\"," +
                "\"Function\"  : \"GetPersistentElements\"" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        JSONArray elements = response.getJSONArray("ReturnValue");
        for (int e = 0; e < elements.length(); ++e) {
            JSONObject element = elements.getJSONObject(e);
            assertTrue(element.has("TypeId"));
            assertTrue(element.has("Reference"));
            assertTrue(element.has("Size"));
        }
    }


    @Ignore @Test
    public void persistentMemoryByte() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject(
           "{" +
                "\"Direction\" : \"Call\"," +
                "\"Function\"  : \"GetPersistentMemoryBytes\"" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        JSONArray bytes = response.getJSONArray("ReturnValue");
        int lastReference = bytes.length() - 1;
        assertTrue(lastReference >= 0);
        int newValue = (bytes.getInt(lastReference) + 1) % 0xFF;
        message = new JSONObject(
           "{" +
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetPersistentMemoryByte\"," +
                "\"Parameters\" :" +
                "{" +
                    "\"Reference\" :" + lastReference  + "," +
                    "\"Value\"     : " + newValue +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.get("Status"));
        assertTrue(response.isNull("ReturnValue"));
        message = new JSONObject(
           "{" +
                "\"Direction\" : \"Call\"," +
                "\"Function\"  : \"GetPersistentMemoryBytes\"" +
            "}");
        response = receiveResponse(message);
        assertEquals(newValue, response.getJSONArray("ReturnValue").get(lastReference));
    }


    @Ignore @Test
    public void persistentMemoryBytes() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject(
           "{" +
                "\"Direction\" : \"Call\"," +
                "\"Function\"  : \"GetPersistentMemoryBytes\"" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        JSONArray bytes = response.getJSONArray("ReturnValue");
        int length = bytes.length();
        assertTrue(length >= 8);
        int reference = length - 8;
        JSONArray newValues = new JSONArray();
        for (int i = 0; i < 8; ++i) {
            newValues.put((bytes.getInt(length - 8 + i) + 1) % 0x100);
        }
        message = new JSONObject(
           "{" +
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetPersistentMemoryBytes\"," +
                "\"Parameters\" :" +
                "{" +
                    "\"Reference\" :" + reference  + "," +
                    "\"Value\"     : " + newValues +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.get("Status"));
        assertTrue(response.isNull("ReturnValue"));
        message = new JSONObject(
           "{" +
                "\"Direction\" : \"Call\"," +
                "\"Function\"  : \"GetPersistentMemoryBytes\"" +
            "}");
        response = receiveResponse(message);
        bytes = response.getJSONArray("ReturnValue");
        boolean changeSucceeded = true;
        for (int i = 0; i < 8; ++i) {
            changeSucceeded &= (bytes.getInt(length - 8 + i) == newValues.getInt(i));
        }
        assertTrue(changeSucceeded);
    }


    @Ignore @Test
    public void memorySizeTest() throws Exception {
        String status = OK_STATUS;
        int size = 8;
        System.out.println(size);
        JSONArray newValues = new JSONArray();
        for (int i = 0; i < size; ++i) {
            newValues.put(size % 0xFF);
        }
        JSONObject message = new JSONObject(
           "{" +
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetPersistentMemoryBytes\"," +
                "\"Parameters\" :" +
                "{" +
                    "\"Reference\" : 0," +
                    "\"Value\"     : " + newValues +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        status = response.getString("Status");
        assertEquals(OK_STATUS, status);
    }


    private boolean isResponse(JSONObject response, JSONObject message) {
        return 
            "Return".equals(response.optString("Direction")) && memberMatch(message, response, "Function");
    }
   

    private static boolean memberMatch(JSONObject message, JSONObject response, String memberName) {
        return 
            message.has(memberName) && response.has(memberName) &&
            message.opt(memberName).equals(response.opt(memberName));
    }
  
    
    private JSONObject receiveResponse(JSONObject messageObject) throws InterruptedException {
        transporter.send(messageObject);
        return transporter.nextReceivedObject();
    }
    
    
    private static Channel createChannel() throws gnu.io.NoSuchPortException {
        final String CHANNEL = "Channel";
        final java.io.File propertiesFile = new java.io.File("MessagingTest.properties");
        final java.util.Properties properties = new java.util.Properties();
        String channelName;
        try {
            properties.load(new java.io.FileInputStream(propertiesFile));
            channelName = properties.getProperty(CHANNEL);
        }
        catch (java.io.IOException ex) {
            channelName = "localhost";
            properties.setProperty(CHANNEL, channelName);
            try {
                properties.store(new java.io.FileOutputStream(propertiesFile), "Messaging Test properties");
            }
            catch (IOException ioEx) {
                Logger.getLogger(MessagingTest.class.getName()).log(Level.WARNING, "Store properties failed", ioEx);
            }
        }
        if (channelName.startsWith("COM")) {
            try {
                return SerialPortChannel.create(channelName);
            }
            catch (ChannelException ex) {
                fail(ex.toString());
                return null;
            }
        }
        else {
            return SocketChannel.create(channelName, 44252);
        }
    }


    private static Transporter transporter = null;

    private static final String STATUS = "Status";
    private static final String OK_STATUS = "OK";
    private static final String ENGINE_RUNNING_STATUS = "EngineIsRunning";
     
}
