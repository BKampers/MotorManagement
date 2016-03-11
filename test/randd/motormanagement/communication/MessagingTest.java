/*
 * Copyright © Bart Kampers
 */

package randd.motormanagement.communication;

import bka.communication.*;
import java.util.Iterator;

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
    
    
    @Test
    public void inclompleteMessage() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject();
        JSONObject response = receiveResponse(message);
//        assertEquals(NOTIFICATION, response.getString(DIRECTION));
        assertEquals("NoDirection", response.getString("Status"));
        message = new JSONObject("{\"Direction\"=\"Call\"}");
        response = receiveResponse(message);
        assertEquals("NoFunction", response.get("Status"));
    }
    
    
    @Test
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
    
    
    @Test
    public void invalidTablename() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetMeasurementTableEnabled\"," +
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
    
    
    @Test
    public void invalidParameter() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetMeasurementTableEnabled\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"AirCorrection\"," +
                    "\"Enabled\"   : 123" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals("InvalidParameter", response.getString("Status"));
        message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetMeasurementTableEnabled\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"AirCorrection\"," +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals("InvalidParameter", response.getString("Status"));
        message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetMeasurementTableEnabled\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"Enabled\" : false" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals("InvalidParameter", response.getString("Status"));
    }
    
    
    @Test(timeout=50)
    public void invalidProcedure() throws JSONException, InterruptedException {
        JSONObject message = createMessage("XXX", "Engine");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(message, response));
        assertEquals("InvalidProcedure", response.getString(STATUS));
    }
    

    @Test(timeout=50)
    public void invalidDataType() throws JSONException, InterruptedException {
        JSONObject message = createMessage(REQUEST, "XXX");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(message, response));
        assertEquals(REQUEST, response.optString("Response"));
        assertEquals("InvalidDatatType", response.optString("Error"));
    }
    
    
    @Test
    public void requestMeasurements() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject(
            "{" +
                "\"Direction\" : \"Call\"," +
                "\"Procedure\" : \"Request\"," +
                "\"DataType\"  : \"Measurement\","+
                "\"Instances\" : [\"RPM\",\"Load\"]," +
                "\"Attributes\": [\"Value\",\"Simulation\"]" +
            "}");
        final String simulation = "Simulation";
        final String value = "Value";
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        JSONObject values = response.getJSONObject(VALUES);
        JSONArray instances = message.getJSONArray(INSTANCES);
        int instanceLength = instances.length();
        assertEquals(instanceLength, values.length());
        for (int i = 0; i < instanceLength; ++i) {
            String instanceName = instances.getString(i);
            JSONObject instanceValue = values.getJSONObject(instanceName);
            assertTrue(instanceValue.has(value));
            assertTrue(instanceValue.has(simulation));
        }
    }
    
    
    @Test
    public void requestAllMeasurements() throws JSONException, InterruptedException {
        final String[] allMeasurementNames = { "RPM", "Load", "Water", "Air", "Battery", "Map", "Lambda", "Aux1", "Aux2" };
        final String dataType = "Measurement";
        JSONObject message = createMessage(REQUEST, dataType);
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        JSONObject values = response.getJSONObject(VALUES);
        assertEquals(allMeasurementNames.length, values.length());
        for (String name : allMeasurementNames) {
            JSONObject value = values.getJSONObject(name);
            assertTrue(value.has("Value"));
            assertTrue(value.has("Simulation"));
        }
    }
    
    
    @Test
    public void requestMeasurementTable() throws JSONException, InterruptedException {
        final String dataType = "MeasurementTable";
        final String currentColumn = "CurrentColumn";
        final String currentRow = "CurrentRow";
        final String table = "Table";
        JSONArray instances = new JSONArray();
        instances.put("Injection");
        JSONArray attributes = new JSONArray();
        attributes.put(currentColumn);
        attributes.put(currentRow);
        attributes.put(table);
        JSONObject message = createMessage(REQUEST, dataType);
        message.put(INSTANCES, instances);
        message.put(ATTRIBUTES, attributes);
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        JSONObject values = response.getJSONObject(VALUES);
        assertEquals(instances.length(), values.length());
        for (int i = 0; i < instances.length(); ++i) {
            String instanceName = instances.getString(i);
            JSONObject instanceValue = values.getJSONObject(instanceName);
            assertTrue(instanceValue.has(currentColumn));
            assertTrue(instanceValue.has(currentRow));
            assertTrue(instanceValue.has(table));
        }
    }
    
    
    @Test
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
    
    
    @Test
    public void getMeasurementProperties() throws JSONException, InterruptedException {
        // Valid name
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetMeasurementProperties\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"MeasurmentName\" : \"Load\"" +
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
                    "\"MeasurmentName\" : \"Xxx\"" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals("NoSuchMeasurement", response.get("Status"));
    }
    
    
    @Test
    public void getMeasurementTableNames() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject(
            "{" +
                "\"Direction\" : \"Call\"," +
                "\"Function\" : \"GetMeasurementTableNames\"" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        JSONArray names = response.getJSONArray("ReturnValue");
        for (int i = 0; i < names.length(); ++i) {
            String tableName = names.getString(i);
            assertFalse(tableName.isEmpty());
        }
    }
    
    
     @Test
    public void modifyRpmSimulation() throws JSONException, InterruptedException {
        // Activate simulation
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetMeasurementSimulation\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"MeasurmentName\" : \"RPM\"," +
                    "\"SimulationValue\" : 2000" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.optString(STATUS));
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
                    "\"MeasurmentName\" : \"RPM\"" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.optString(STATUS));
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
    
    
    @Test
    public void setMeasurementTableField() throws JSONException, InterruptedException {
        // Get values
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetMeasurementTableFields\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"Ignition\"" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        double value = response.getJSONArray("ReturnValue").getJSONArray(0).getDouble(0);
        // Modify value
        double newValue = value + 1.0;
        message = new JSONObject(
            "{" +
                "\"Direction\" : \"Call\"," +
                "\"Function\"  : \"SetMeasurementTableField\"," +
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
                "\"Function\"   : \"GetMeasurementTableFields\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"Ignition\"" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        value = response.getJSONArray("ReturnValue").getJSONArray(0).getDouble(0);
        assertEquals(Math.round(newValue), Math.round(value));
    }
    
    
    @Test
    public void modifyMultipleTableField() throws JSONException, InterruptedException {
        JSONObject message = new JSONObject(
            "{" +
                "\"Direction\" : \"Call\"," +
                "\"Procedure\" : \"Modify\"," +
                "\"DataType\"  : \"MeasurementTable\","+
                "\"Instances\" : [\"Ignition\",\"Injection\"]," +
                "\"Values\": {\"Fields\":[" +
                    "{\"Column\":0,\"Row\":1,\"Value\":0.1}" + 
                "]}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        String status = response.getString("Status");
        assertFalse(OK_STATUS.equals(status));
    }
    
    
    @Test
    public void getMeasurementTableProperties() throws JSONException, InterruptedException {
        // Valid name
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetMeasurementTableProperties\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"Injection\"" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.get(STATUS));
        JSONObject value = response.getJSONObject("ReturnValue");
        assertNotNull(value.getBoolean("Enabled"));
        assertNotNull(value.getInt("CurrentColumn"));        
        assertNotNull(value.getInt("CurrentRow"));
        // Invalid name
        message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetMeasurementTableProperties\"," +
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
                "\"Function\"   : \"GetMeasurementTableProperties\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"Xxx\" : \"Injection\"" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals("InvalidParameter", response.get("Status"));
    }
    
    
    @Test
    public void getMeasurementTableFields() throws JSONException, InterruptedException {
        // Valid name
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"GetMeasurementTableFields\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"Injection\"" +
                "}" +
            "}");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.get(STATUS));
        JSONArray rows = response.getJSONArray("ReturnValue");
        for (int row = 0; row < rows.length(); ++row) {
            JSONArray columns = rows.getJSONArray(row);
            for (int column = 0; column < columns.length(); ++column) {
                assertNotNull(columns.optDouble(column));
            }
        }
    }
    
    
    @Test
    public void modifyTableEnabling() throws JSONException, InterruptedException {
        // Enable WaterCorrection
        JSONObject message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetMeasurementTableEnabled\"," +
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
                "\"Direction\" : \"Call\"," +
                "\"Procedure\" : \"Request\"," +
                "\"DataType\"  : \"MeasurementTable\","+
                "\"Instances\" : [\"WaterCorrection\"]," +
                "\"Attributes\": [\"Enabled\"]" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertTrue(response.getJSONObject("Values").getJSONObject("WaterCorrection").getBoolean("Enabled"));
        // Disable WaterCorrection
        message = new JSONObject(
            "{"+
                "\"Direction\"  : \"Call\"," +
                "\"Function\"   : \"SetMeasurementTableEnabled\"," +
                "\"Parameters\" : " +
                "{" +
                    "\"TableName\" : \"WaterCorrection\"," +
                    "\"Enabled\"   : false" +
                "}" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertEquals(OK_STATUS, response.opt(STATUS));
        // Check enabled
        message = new JSONObject(
            "{" +
                "\"Direction\" : \"Call\"," +
                "\"Procedure\" : \"Request\"," +
                "\"DataType\"  : \"MeasurementTable\","+
                "\"Instances\" : [\"WaterCorrection\"]," +
                "\"Attributes\": [\"Enabled\"]" +
            "}");
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertFalse(response.getJSONObject("Values").getJSONObject("WaterCorrection").getBoolean("Enabled"));
    }
    
    
    @Test(timeout=50)
    public void requestMeasurementTables() throws JSONException, InterruptedException {
        final String dataType = "MeasurementTable";
        final String nameAttribute = "Name";
        JSONObject message = createMessage(REQUEST, dataType);
        JSONArray fields = new JSONArray();
        fields.put(nameAttribute);
        message.put(ATTRIBUTES, fields);
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        JSONObject values = response.optJSONObject(VALUES);
        assertNotNull(values);
        assertTrue(values.length() > 0);
        java.util.Iterator keys = values.keys();
        while (keys.hasNext()) {
            JSONObject tableObject = values.getJSONObject(keys.next().toString());
            assertNotNull(tableObject);
            assertEquals(0, tableObject.length());
        }
    }
    
    
    @Test(timeout=750)
    public void requestTable() throws JSONException, InterruptedException {
        JSONObject message = createMessage(REQUEST, "MeasurementTable");
        JSONArray instances = new JSONArray();
        instances.put("Ignition");
        message.put(INSTANCES, instances);
        JSONArray fields = new JSONArray();
        fields.put(ATTRIBUTES);
        fields.put("Enabled");
        fields.put("CurrentColum");
        fields.put("CurrentRow");
        message.put(ATTRIBUTES, fields);
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        JSONObject values = response.getJSONObject("Values");
        assertEquals(1, values.length());
        JSONObject tableObject = values.getJSONObject("Ignition");
        assertTrue(tableObject.getBoolean("Enabled"));
        assertNotNull(tableObject.get("CurrentColumn"));
        assertNotNull(tableObject.get("CurrentRow"));
    }
    
    
    @Test
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
    
    
    @Test(timeout=200)
    public void setCogwheel() throws JSONException, InterruptedException {
        // Valid cog wheel
        JSONObject message = createMessage(MODIFY, "Engine");
        JSONObject cogWheel = new JSONObject();
        cogWheel.put("CogTotal", 60);
        cogWheel.put("GapSize", 2);
        cogWheel.put("Offset", 20);
        JSONObject values = new JSONObject();
        values.put("CogWheel", cogWheel);
        message.put(VALUES, values);
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        String status = response.getString(STATUS);
        assertTrue(OK_STATUS.equals(status) || ENGINE_RUNNING_STATUS.equals(status));
        // Invalid cog wheel
        cogWheel.put("CogTotal", 300);
        cogWheel.put("GapSize", 2500);
        cogWheel.put("Offset", 2000);
        response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        assertFalse(OK_STATUS.equals(response.get(STATUS)));
    }
    
    
    @Test
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
    
    
    @Test
    public void requestPersistentElements() throws JSONException, InterruptedException {
        JSONObject message = createMessage(REQUEST, "Elements");
        JSONObject response = receiveResponse(message);
        assertTrue(isResponse(response, message));
        JSONArray elements =  response.getJSONObject(VALUES).getJSONArray("Persistent");
        for (int e = 0; e < elements.length(); ++e) {
            JSONObject element = elements.getJSONObject(e);
            assertTrue(element.has("TypeId"));
            assertTrue(element.has("Reference"));
            assertTrue(element.has("Size"));
        }
    }
    
    
    private JSONObject createMessage(String procedure, String dataType) throws JSONException {
        JSONObject messageObject = new JSONObject();
        messageObject.put(DIRECTION, "Call");
        messageObject.put(PROCEDURE, procedure);
        messageObject.put(DATA_TYPE, dataType);
        return messageObject;
    }
    
  
    private boolean isResponse(JSONObject response, JSONObject message) {
        return 
            RETURN.equals(response.optString(DIRECTION)) &&
            (memberMatch(message, response, PROCEDURE) &&
             memberMatch(message, response, DATA_TYPE) ||
             memberMatch(message, response, "Function"));
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

    private static final String DATA_TYPE = "DataType";
    private static final String PROCEDURE = "Procedure";
    private static final String DIRECTION = "Direction";
    private static final String NOTIFICATION = "Notification";
    private static final String REQUEST = "Request";
    private static final String MODIFY = "Modify";
    private static final String INSTANCES = "Instances";
    private static final String RETURN = "Return";
    private static final String ATTRIBUTES = "Attributes";
    private static final String VALUES = "Values";
    
    private static final String STATUS = "Status";
    private static final String OK_STATUS = "OK";
    private static final String ENGINE_RUNNING_STATUS = "EngineIsRunning";
     
}
