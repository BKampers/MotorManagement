/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.communication;

import randd.motormanagement.system.*;

import java.util.*;
import java.util.logging.*;
import org.json.*;


public class RemoteSystem {
    
    
    public interface Listener {
        void notificationReceived(Notification notification);
        void tableNames(Collection<String> names);
    }
    
    
    RemoteSystem(Messenger messenger) {
        if (messenger == null) {
            throw new IllegalArgumentException();
        }
        this.messenger = messenger;
        this.messenger.setListener(new MessengerListener());
    }


    public RemoteSystem(Transporter transporter) {
        this(new Messenger(transporter));
    }


    public void connect() throws bka.communication.ChannelException  {
        messenger.start();
    }
    
    
    public void disconnect() throws bka.communication.ChannelException {
        stopPolling();
        messenger.stop();
    }

    
    public void startPolling(int pollInterval) {
        if (pollTimer == null) {
            PollTask pollTask = new PollTask();
            pollEngine = true;
            pollTimer = new Timer();
            pollTimer.schedule(pollTask, 0, pollInterval);
        }
    }
    
    
    public void stopPolling() {
        if (pollTimer != null) {
            pollTimer.cancel();
            pollEngine = false;
            pollTimer = null;
        }
    }
    
    
    public void startIndexPoll(Table table) {
        synchronized (tablesToPoll) {
            if (! tablesToPoll.contains(table)) {
                tablesToPoll.add(table);
            }
        }
    }
    
    
    public void stopIndexPoll(Table table) {
        synchronized (tablesToPoll) {
            tablesToPoll.remove(table);
        }
    }
    
    
    public void requestTableNames() throws InterruptedException {
        call(GET_TABLE_NAMES);
    }
    
    
    public void requestTableProperties(Table table) throws InterruptedException {
        call(GET_TABLE_PROPERTIES, TABLE_NAME, table.getName());
    }
    
    
    public void requestTableFields(Table table) throws InterruptedException {
        call(GET_TABLE_FIELDS, TABLE_NAME, table.getName());
    }
    
    
    public void enableTable(Table table, boolean enabled) throws InterruptedException {
        call(SET_TABLE_ENABLED, TABLE_NAME, table.getName(), ENABLED, enabled);
    }

    
    public void modifyTable(Table table, int column, int row, float value) throws InterruptedException {
        call(SET_TABLE_FIELD, TABLE_NAME, table.getName(), COLUMN, column, ROW, row, VALUE, value);
    }
    
    
    public void requestEngine() throws InterruptedException {
        call(GET_ENGINE_PROPERTIES);
    }
    
    
    public void modifyCylinderCount(int count) throws InterruptedException {
        call(SET_CYLINDER_COUNT, CYLINDER_COUNT, count);
    }
    
    
    public void modifyCogwheel(int cogTotal, int gapSize, int offset) throws InterruptedException {
        call(SET_COGWHEEL_PROPERTIES, COG_TOTAL, cogTotal, GAP_SIZE, gapSize, OFFSET, offset);
    }


    public void requestMeasurementProperties(Measurement measurement) throws InterruptedException {
        call(GET_MEASUREMENT_PROPERTIES, MEASUREMENT_NAME, measurement.getName());
    }
    

    public void enableMeasurementSimulation(Measurement measurement, float simulationValue) throws InterruptedException {
        call(SET_MEASUREMENT_SIMULATION, MEASUREMENT_NAME, measurement.getName(), SIMULATION_VALUE, simulationValue);
    }
    
    
    public void disableMeasurementSimulation(Measurement measurement) throws InterruptedException {
        call("ResetMeasurementSimulation", MEASUREMENT_NAME, measurement.getName());
    }
    
    
    public void requestFlash() throws InterruptedException {
        call(GET_PERSISTENT_MEMORY_BYTES);
        call(GET_PERSISTENT_ELEMENTS);
    }
    
       
    public void modifyFlash(int reference, int[] values) throws InterruptedException {
        int index = 0;
        int total = values.length;
        int referenceToSend = reference;
        while (index < total) {
            int count = Math.min(total - index, MAX_FLASH_SIZE_TO_SEND);
            int[] valuesToSend = Arrays.copyOfRange(values, index, index + count);
            call(SET_PERSISTENT_MEMORY_BYTES, REFERENCE, referenceToSend, VALUE, valuesToSend);
            referenceToSend += count;
            index += count;
        }
    }
    
    
    public Table getCorrectionTable(Measurement measurement) {
        assert (measurement != null);
        String measurementName = measurement.getName();
        if (! "Load".equals(measurementName) && ! "RPM".equals(measurementName)) {
            return Table.getInstance(measurementName + "Correction");
        }
        else {
            return null;
        }
    }


    public void fire(String name, String value) {
        try {
            JSONObject message = new JSONObject();
            message.put(Messenger.DIRECTION, Messenger.FIRE);
            message.put(name, value);
            messenger.send(message);
        }
        catch (JSONException ex) {
            LOGGER.log(Level.SEVERE, Messenger.FIRE, ex);
        }
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
    
    
    public Engine getEngine() {
        return engine;
    }
    
    
    public Flash getFlash() {
        return flash;
    }


    private void call(String function, Object ... arguments) {
        JSONObject message = callObject(function, arguments);
        messenger.send(message);
    }


    private JSONObject callObject(String function, Object ... arguments) {
        JSONObject object = new JSONObject();
        try {
            JSONObject parameters = new JSONObject();
            for (int i = 0; i < arguments.length - 1; i += 2) {
                parameters.put(arguments[i].toString(), arguments[i + 1]);
            }
            object.put(Messenger.DIRECTION, Messenger.CALL);
            object.put(Messenger.FUNCTION, function);
            object.put(Messenger.PARAMETERS, parameters);
        }
        catch (JSONException ex) {
            LOGGER.log(Level.SEVERE, Messenger.CALL, ex);
        }
        return object;
    }
    
    
    private void updateMeasurements(JSONObject measurementsObject)throws JSONException {
        Iterator names = measurementsObject.keys();
        while (names.hasNext()) {
            String measurementName = names.next().toString();
            Measurement measurement = Measurement.getInstance(measurementName);
            if (measurement != null) {
                JSONObject measurementObject = measurementsObject.getJSONObject(measurementName);
                double value = measurementObject.optDouble(VALUE);
                if (value != Double.NaN) {
                    measurement.setValue((float) value);
                }
                measurement.setSimulationEnabled(measurementObject.optBoolean(SIMULATION));
            }
        }
    }
    

    private void updateMeasurementProperties(JSONObject measurementObject) throws JSONException {
        Measurement measurement = Measurement.getInstance(measurementObject.optString(MEASUREMENT_NAME));
        if (measurement != null) {
            measurement.setFormat(measurementObject.getString(FORMAT));
            double minimum = measurementObject.optDouble(MINIMUM);
            if (minimum != Double.NaN) {
                measurement.setMinimum((float) minimum);
            }
            double maximum = measurementObject.optDouble(MAXIMUM);
            if (maximum != Double.NaN) {
                measurement.setMaximum((float) maximum);
            }
            double simulationValue = measurementObject.optDouble(SIMULATION_VALUE);
            if (simulationValue != Double.NaN) {
                measurement.setSimulationValue((float) simulationValue);
            }
        }
    }


    private static void updateMeasurementSimulation(JSONObject object) throws JSONException {
        //Measurement measurement = Measurement.getInstance(object.getString(MEASUREMENT_NAME));

    }
    

    private void updateTableField(JSONObject object) throws JSONException {
        Table table = Table.getInstance(object.getString(TABLE_NAME));
        table.setField(object.getInt(COLUMN), object.getInt(ROW), (float) object.getDouble(VALUE));
    }
    
    
    private void updateTableProperties(JSONObject object) throws JSONException {
        Table table = Table.getInstance(object.getString(TABLE_NAME));
        table.setEnabled(object.getBoolean(ENABLED));
        table.setDecimals(object.getInt(DECIMALS));
        table.setMinimum((float) object.getDouble(MINIMUM));
        table.setMaximum((float) object.getDouble(MAXIMUM));
        if (object.has(COLUMN_MEASUREMENT_NAME)) {
            String measurementName = object.getString(COLUMN_MEASUREMENT_NAME);
            Measurement measurement = Measurement.getInstance(measurementName);
            table.setColumnMeasurement(measurement);
        }
        if (object.has(ROW_MEASUREMENT_NAME)) {
            String measurementName = object.getString(ROW_MEASUREMENT_NAME);
            Measurement measurement = Measurement.getInstance(measurementName);
            table.setRowMeasurement(measurement);
        }
    }
    
    
    private void updateEngine(JSONObject engineObject) throws JSONException {
        engine.setCylinderCount(engineObject.getInt(CYLINDER_COUNT));
        JSONObject cogwheelObject = engineObject.getJSONObject(COGWHEEL);
        engine.setCogwheel(cogwheelObject.getInt(COG_TOTAL), cogwheelObject.getInt(GAP_SIZE), cogwheelObject.getInt(OFFSET));
        JSONArray deadPointArray = engineObject.getJSONArray(DEAD_POINTS);
        int deadPointCount = deadPointArray.length();
        List<Integer> deadPoints = new ArrayList<>();
        for (int i = 0; i < deadPointCount; ++i) {
            deadPoints.add(deadPointArray.getInt(i));
        }
        engine.setDeadPoints(deadPoints);
    }
    

    private void updatePersistentMemory(JSONArray memoryArray) throws JSONException {
        int length = memoryArray.length();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; ++i) {
            bytes[i] = (byte) memoryArray.getInt(i);
        }
        flash.setBytes(0, bytes);
    }
    
    
    private void updatePersistentElements(JSONArray elementsArray) throws JSONException {
        int length = elementsArray.length();
        Flash.Element[] elements = new Flash.Element[length];
        for (int i = 0; i < length; ++i) {
            JSONObject elementObject = elementsArray.getJSONObject(i);
            elements[i] = new Flash.Element(
                elementObject.getInt(TYPE_ID),
                elementObject.getInt(REFERENCE),
                elementObject.getInt(SIZE));
        }
        flash.setElements(elements);
    }

    
    private void updateTableActualValues(JSONObject object) throws JSONException {
        Table table = Table.getInstance(object.getString(TABLE_NAME));
        table.setColumnIndex(object.getInt(CURRENT_COLUMN));
        table.setRowIndex(object.getInt(CURRENT_ROW));
    }
    
    
    private void updateTableFields(JSONObject object) throws JSONException {
        Table table = Table.getInstance(object.getString(TABLE_NAME));
        JSONArray rows = object.getJSONArray(FIELDS);
        float[][] fields = new float[rows.length()][];
        for (int r = 0; r < rows.length(); ++r) {
            JSONArray columns = rows.getJSONArray(r);
            fields[r] = new float[columns.length()];
            for (int c = 0; c < columns.length(); ++c) {
                fields[r][c] = (float) columns.getDouble(c);
            }
        }
        table.setFields(fields);
    }
    
    
    private void updateTableEnabled(JSONObject object) throws JSONException {
        Table table = Table.getInstance(object.getString(TABLE_NAME));
        table.setEnabled(object.getBoolean(ENABLED));
    }
    
    
    private class PollTask extends TimerTask {
        
        @Override
        public void run() {
            message = nextMessage();
            if (message != null) {
                LOGGER.log(Level.FINEST, ">> {0}", message);
                messenger.send(message);
            }
        }
        
        private JSONObject nextMessage()  {
            if (message != null && IS_ENGINE_RUNNING.equals(message.optString(Messenger.FUNCTION))) {
                return callObject(GET_MEASUREMENTS);
            }
            else {
                JSONObject tableMessage = nextTableMessage();
                if (tableMessage != null) {
                    return tableMessage;
                }
                else {
                    return callObject(IS_ENGINE_RUNNING);
                }
            }
//            JSONObject pollMessage = nextMeasurementMessage();
//            if (pollMessage == null) {
//                pollMessage = nextTableMessage();
//                if (pollMessage == null) {
//                    pollMessage = engineMessage();
////                    measurmentIndex = 0;
//                    tableIndex = 0;
//                }
//            }
//            return pollMessage;
        }

//        private JSONObject nextMeasurementMessage() throws JSONException {
//            return messageObject(GET_MEASUREMENTS);
//        }

        private JSONObject nextTableMessage() {
            JSONObject pollMessage = null;
            List<Table> tables;
            synchronized (tablesToPoll) {
                tables = new ArrayList<>(tablesToPoll);
            }
            if (tableIndex < tables.size()) {
                Table table = tables.get(tableIndex);
                pollMessage = callObject(GET_TABLE_ACTUAL_VALUES, TABLE_NAME, table.getName());
                tableIndex++;
            }
            else {
                tableIndex = 0;
            }
            return pollMessage;
        }
        
//        private JSONObject engineMessage() throws JSONException {
//            if (pollEngine) {
//                return messageObject(IS_ENGINE_RUNNING);
//            }
//            else {
//                return null;
//            }
//        }
        
//        private int measurmentIndex = 0;
        private int tableIndex = 0;
        private JSONObject message = null;
    }


    private class MessengerListener implements Messenger.Listener {

        @Override
        public void notifyMessage(JSONObject message) {
            LOGGER.log(Level.FINEST, "<< {0}", message);
            if (Messenger.FIRE.equals(message.optString(Messenger.DIRECTION))) {
                handleNotifications(message);
            }
            //TODO: Handle system reboot
//                if ("Randd MM32".equals(message.optString("System"))) {
//                    synchronized (listeners) {
//                        for (Listener listener : listeners) {
//                            listener.systemStart("MM32");
//                        }
//                    }
//                }

        }

        @Override
        public void notifyResponse(JSONObject message, JSONObject response) {
            LOGGER.log(Level.FINEST, "<< {0}", response);
            try {
                String function = response.getString(Messenger.FUNCTION);
                JSONObject returnValue = response.optJSONObject(Messenger.RETURN_VALUE);
                if (function.equals(GET_MEASUREMENTS)) {
                    updateMeasurements(returnValue);
                }
                else if (function.equals(GET_MEASUREMENT_PROPERTIES)) {
                    updateMeasurementProperties(returnValue);
                }
                else if (function.equals(GET_TABLE_PROPERTIES)) {
                    updateTableProperties(returnValue);
                }
                else if (function.equals(GET_TABLE_ACTUAL_VALUES)) {
                    updateTableActualValues(returnValue);
                }
                else if (function.equals(GET_TABLE_FIELDS)) {
                    updateTableFields(returnValue);
                }
                else if (function.equals(SET_TABLE_FIELD)) {
                    updateTableField(returnValue);
                }
                else if (function.equals(SET_TABLE_ENABLED)) {
                    updateTableEnabled(returnValue);
                }
                else if (function.equals(IS_ENGINE_RUNNING)) {
                    engine.setRunning(response.getBoolean(Messenger.RETURN_VALUE));
                }
                else if (function.equals(GET_TABLE_NAMES)) {
                    notifyTableNames(response.getJSONArray(Messenger.RETURN_VALUE));
                }
                else if (function.equals(GET_ENGINE_PROPERTIES)) {
                    updateEngine(returnValue);
                }
                else if (function.equals(SET_CYLINDER_COUNT)) {
                    updateEngine(returnValue);
                }
                else if (function.equals(SET_COGWHEEL_PROPERTIES)) {
                    updateEngine(returnValue);
                }
                else if (function.equals(SET_MEASUREMENT_SIMULATION)) {
                    //updateMeasurementSimulation(returnValue);
                }
                else if (function.equals(GET_PERSISTENT_MEMORY_BYTES)) {
                    updatePersistentMemory(response.getJSONArray(Messenger.RETURN_VALUE));
                }
                else if (function.equals(GET_PERSISTENT_ELEMENTS)) {
                    updatePersistentElements(response.getJSONArray(Messenger.RETURN_VALUE));
                }
//                else if (response.has(ENABLED)) {
//                    updateTableEnabled(response);
//                }
                else {
                    LOGGER.log(Level.WARNING, "Unhandled: {0}", response);
                }
            }
            catch (JSONException ex) {
                LOGGER.log(Level.WARNING, response.toString(), ex);
            }       
        }
        
        private void handleNotifications(JSONObject message) {
            for (Notification notification : createNotifications(message)) {
                synchronized (listeners) {
                    for (Listener listener : listeners) {
                        listener.notificationReceived(notification);
                    }
                }
            }
        }

        Collection<Notification> createNotifications(JSONObject message) {
            Collection<Notification> notifications = new ArrayList<>(message.length());
            Iterator keys = message.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();
                if (! Messenger.DIRECTION.equals(key)) {
                    notifications.add(new Notification(key, message.optString(key)));
                }
            }
            return notifications;
        }


        private Measurement getMeasurement(String name) {
            for (Measurement measurement : MEASUREMENTS) {
                if (name.equals(measurement.getName())) {
                    return measurement;
                }
            }
            return null;
        }

        private void updateActiveTable(JSONObject response) throws JSONException {
            if (response.has(VALUE)) {
                updateTableField(response);
            }
            else {
                updateTableActualValues(response);
            }
        }

        private void notifyTableNames(JSONArray array) throws JSONException {
            Collection<String> names = new ArrayList<>();
            for (int i = 0; i < array.length(); ++i) {
                names.add(array.getString(i));
            }
            synchronized (listeners) {
                for (Listener listener : listeners) {
                    listener.tableNames(names);
                }
            }
        }

        private void requestEngineUpdate() {
            try {
                requestEngine();
            }
            catch (InterruptedException ex) {
                LOGGER.log(Level.WARNING, "requestEngineUpdate", ex);
            }
        }
        
    }

    
    private final Engine engine = new Engine();
    private final Flash flash = new Flash();
    
    private boolean pollEngine = false;
    private final Collection<Table> tablesToPoll = new ArrayList<>();
    private final Collection<Listener> listeners = new ArrayList<>();

    private Timer pollTimer;

    private final Messenger messenger;
    
    
    private static final Measurement[] MEASUREMENTS = new Measurement[] {
        Measurement.getInstance("RPM"),
        Measurement.getInstance("Load"),
        Measurement.getInstance("Water"),
        Measurement.getInstance("Air"),
        Measurement.getInstance("Battery"),
        Measurement.getInstance("Map"),
        Measurement.getInstance("Lambda"),
        Measurement.getInstance("Aux1"),
        Measurement.getInstance("Aux2")
    };
        

    private static final String GET_TABLE_FIELDS = "GetTableFields";
    private static final String GET_MEASUREMENT_PROPERTIES = "GetMeasurementProperties";
    private static final String GET_MEASUREMENTS = "GetMeasurements";
    private static final String SET_MEASUREMENT_SIMULATION = "SetMeasurementSimulation";
    private static final String SET_TABLE_FIELD = "SetTableField";
    private static final String SET_TABLE_ENABLED = "SetTableEnabled";
    private static final String GET_TABLE_ACTUAL_VALUES = "GetTableActualValues";
    private static final String GET_TABLE_PROPERTIES = "GetTableProperties";
    private static final String GET_TABLE_NAMES = "GetTableNames";
    private static final String GET_ENGINE_PROPERTIES = "GetEngineProperties";
    private static final String IS_ENGINE_RUNNING = "IsEngineRunning";
    private static final String SET_CYLINDER_COUNT = "SetCylinderCount";
    private static final String SET_COGWHEEL_PROPERTIES = "SetCogwheelProperties";
    private static final String GET_PERSISTENT_ELEMENTS = "GetPersistentElements";
    private static final String GET_PERSISTENT_MEMORY_BYTES = "GetPersistentMemoryBytes";
    private static final String SET_PERSISTENT_MEMORY_BYTES = "SetPersistentMemoryBytes";

    private static final String TABLE_NAME = "TableName";
    private static final String MEASUREMENT_NAME = "MeasurementName";

//    
//    private static final String NOTIFICATION = "Notification";
//    private static final String SUBJECT = "Subject";
//    
//    private static final String REQUEST = "Request";
//    private static final String MODIFY = "Modify";
//    
//    private static final String PROPERTIES = "Properties";
//    private static final String TABLE = "Table";
//    private static final String DECIMALS = "Decimals";
//    private static final String INDEX = "Index";
    private static final String ENABLED = "Enabled";
    private static final String SIMULATION = "Simulation";
    private static final String SIMULATION_VALUE = "SimulationValue";
//    
//    private static final String MEASUREMENT_TABLES = "MeasurementTables";
//    private static final String FLASH = "Flash";
//    private static final String FLASH_ELEMENTS = "FlashElements";
//    private static final String ENGINE_IS_RUNNING = "EngineIsRunning";
//    
//    private static final String NAMES = "Names";
//    private static final String ELEMENTS = "Elements";
//    private static final String COUNT = "Count";
    private static final String TYPE_ID = "TypeId";
    private static final String REFERENCE = "Reference";
    private static final String SIZE = "Size";
//    
    private static final String CURRENT_COLUMN = "CurrentColumn";
    private static final String CURRENT_ROW = "CurrentRow";
    private static final String FIELDS = "Fields";
    private static final String COLUMN = "Column";
    private static final String ROW = "Row";
    private static final String VALUE = "Value";
    
    private static final String COLUMN_MEASUREMENT_NAME = "ColumnMeasurementName";
    private static final String ROW_MEASUREMENT_NAME = "RowMeasurementName";
    private static final String FORMAT = "Format";
    private static final String MINIMUM = "Minimum";
    private static final String MAXIMUM = "Maximum";
    private static final String PRECISION = "Precision";
    private static final String DECIMALS = "Decimals";
//
//    private static final String ENGINE = "Engine";
    private static final String COGWHEEL = "Cogwheel";
    private static final String COG_TOTAL = "CogTotal";
    private static final String GAP_SIZE = "GapSize";
    private static final String OFFSET = "Offset";
    private static final String DEAD_POINTS = "DeadPoints";
    private static final String CYLINDER_COUNT = "CylinderCount";
    
    private static final int MAX_FLASH_SIZE_TO_SEND = 0x10;
    
    private static final Logger LOGGER = Logger.getLogger(RemoteSystem.class.getName());

}
