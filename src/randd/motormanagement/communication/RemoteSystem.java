/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.communication;

import randd.motormanagement.system.*;

import java.util.*;
import java.util.logging.*;
import org.json.*;


public class RemoteSystem {
    
//    public static final String OK = "OK";
//    public static final String CORRECTION_SUFFIX = "Correction";

    
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
    
    
    public void requestTableNames() throws InterruptedException, JSONException {
        call(GET_TABLE_NAMES);
    }
    
    
    public void requestTableProperties(Table table) throws InterruptedException, JSONException {
        JSONObject parameters = new JSONObject();
        parameters.put(TABLE_NAME, table.getName());
        call(GET_TABLE_PROPERTIES, parameters);
    }
    
    
    public void requestTableFields(Table table) throws InterruptedException, JSONException {
        JSONObject parameters = new JSONObject();
        parameters.put(TABLE_NAME, table.getName());
        call(GET_TABLE_FIELDS, parameters);
    }
    
    
    public void enableTable(Table table, boolean enabled) throws JSONException, InterruptedException {
        JSONObject parameters = new JSONObject();
        parameters.put(ENABLED, enabled);
        call("SetTableEnabled", parameters);
    }

    
    public void modifyTable(Table table, int column, int row, float value) throws JSONException, InterruptedException {
        JSONObject parameters = new JSONObject();
        parameters.put(TABLE_NAME, table.getName());
        parameters.put(COLUMN, column);
        parameters.put(ROW, row);
        parameters.put(VALUE, value);
        call(SET_TABLE_FIELD, parameters);
    }
    
    
    public void requestEngine() throws InterruptedException, JSONException {
        call(GET_ENGINE_PROPERTIES);
    }
    
    
    public void modifyCylinderCount(int count) throws InterruptedException, JSONException {
        JSONObject parameters = new JSONObject();
        parameters.put(CYLINDER_COUNT, count);
        call("SetCylinderCount", parameters);
    }
    
    
    public void modifyCogwheel(int cogTotal, int gapSize, int offset) throws InterruptedException, JSONException {
        JSONObject parameters = new JSONObject();
        parameters.put(COG_TOTAL, cogTotal);
        parameters.put(GAP_SIZE, gapSize);
        parameters.put(OFFSET, offset);
        call("SetCogwheelProperties", parameters);
    }
    

    public void enableMeasurementSimulation(Measurement measurement, float simulationValue) throws JSONException, InterruptedException {
        JSONObject parameters = new JSONObject();
        parameters.put(MEASUREMENT_NAME, measurement.getName());
        parameters.put(SIMULATION_VALUE, simulationValue);
        call("SetMeasurementSimulation", parameters);
    }
    
    
    public void disableMeasurementSimulation(Measurement measurement) throws JSONException, InterruptedException {
        JSONObject parameters = new JSONObject();
        parameters.put(MEASUREMENT_NAME, measurement.getName());
        call("ResetMeasurementSimulation", parameters);
    }
    
    
    public void requestFlash() throws InterruptedException, JSONException {
//        request(FLASH);
        call("GetPersistentElements");
    }
    
    
    public void modifyFlash(int reference, int count, int value) throws JSONException, InterruptedException {
        //modify(FLASH, REFERENCE, reference, COUNT, count, VALUE, value);
    }
    
    
    public void modifyFlash(int reference, int[] values) throws JSONException, InterruptedException {
        int index = 0;
        int total = values.length;
        int referenceToSend = reference;
        while (index < total) {
            int count = Math.min(total - index, MAX_FLASH_SIZE_TO_SEND);
            int[] valuesToSend = Arrays.copyOfRange(values, index, index + count);
//            modify(FLASH, REFERENCE, referenceToSend, VALUE, valuesToSend);
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
    
    
    private void call(String function) throws JSONException {
        call(function, null);
    }
    
    
    private void call(String function, JSONObject parameters) throws JSONException {
        JSONObject message = new JSONObject();
        message.put(Messenger.DIRECTION, Messenger.CALL);
        message.put(Messenger.FUNCTION, function);
        if (parameters != null) {
            message.put(Messenger.PARAMETERS, parameters);
        }
        messenger.send(message);
    }
    
//    private void send(String message, String subject, Object ... options) throws JSONException {
//        JSONObject messageObject = messageObject(message, subject, options);
//        logger.log(Level.FINEST, ">> {0}", messageObject);
//        messenger.send(messageObject);
//    }
    
    
    private JSONObject messageObject(String function, Object ... options) throws JSONException {
        JSONObject object = new JSONObject();
        object.put(Messenger.DIRECTION, Messenger.CALL);
        object.put(Messenger.FUNCTION, function);
        for (int i = 0; i < options.length - 1; i += 2) {
            object.put(options[i].toString(), options[i+1]);
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
    

    private void updateTableField(JSONObject object) throws JSONException {
        Table table = Table.getInstance(object.getString(TABLE_NAME));
        table.setField(object.getInt(COLUMN), object.getInt(ROW), (float) object.getDouble(VALUE));
    }
    
    
    private void updateTable(JSONObject object) throws JSONException {
        Table table = Table.getInstance(object.getString(TABLE_NAME));
        JSONArray tableArray = object.getJSONArray(Messenger.RETURN_VALUE);
        final int rowCount = tableArray.length();
        float[][] fields = new float[rowCount][];
        for (int row = 0; row < rowCount; ++row) {
            JSONArray rowArray = tableArray.getJSONArray(row);
            final int columnCount = rowArray.length();
            fields[row] = new float[columnCount];
            for (int column = 0; column < columnCount; ++column) {
                fields[row][column] = (float) rowArray.getDouble(column);
            }
        }
        table.setFields(fields);
//        table.setDecimals(object.optInt(DECIMALS, 0));
//        table.setMinimum((float) object.optDouble(MINIMUM, 0.0));
//        table.setMaximum((float) object.optDouble(MAXIMUM, 100.0));
//        if (object.has(COLUMN_MEASUREMENT_NAME)) {
//            String measurementName = object.getString(COLUMN_MEASUREMENT_NAME);
//            Measurement measurement = Measurement.getInstance(measurementName);
//            table.setColumnMeasurement(measurement);
//        }
//        if (object.has(ROW_MEASUREMENT_NAME)) {
//            String measurementName = object.getString(ROW_MEASUREMENT_NAME);
//            Measurement measurement = Measurement.getInstance(measurementName);
//            table.setRowMeasurement(measurement);
//        }
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
    

    private void updateFlash(JSONObject flashObject) throws JSONException {
//        int reference = flashObject.getInt(REFERENCE);
//        JSONArray memoryArray = flashObject.getJSONArray(VALUE);
//        int length = memoryArray.length();
//        byte[] bytes = new byte[length];
//        for (int i = 0; i < length; ++i) {
//            bytes[i] = (byte) memoryArray.getInt(i);
//        }
//        flash.setBytes(reference, bytes);
    }
    
    
    private void updateFlashElements(JSONObject elementsObject) throws JSONException {
        JSONArray elementsArray = elementsObject.getJSONArray(Messenger.RETURN_VALUE);
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

    
    private void updateTableIndex(JSONObject object) throws JSONException {
        Table table = Table.getInstance(object.getString(TABLE_NAME));
        table.setColumnIndex(object.getInt(CURRENT_COLUMN));
        table.setRowIndex(object.getInt(CURRENT_ROW));
    }
    
    
    private void updateTableEnabled(JSONObject object) throws JSONException {
        Table table = Table.getInstance(object.getString(TABLE_NAME));
        table.setEnabled(object.getBoolean(ENABLED));
    }
    
    
    private class PollTask extends TimerTask /*implements Runnable */{
        
        @Override
        public void run() {
            try {
                message = nextMessage();
                if (message != null) {
                    LOGGER.log(Level.FINEST, ">> {0}", message);
                    messenger.send(message);
                }
            }
            catch (JSONException ex) {
                LOGGER.log(Level.WARNING, "PollTask", ex);
            }
        }
        
        private JSONObject nextMessage() throws JSONException {
            JSONObject pollMessage = nextMeasurementMessage();
            if (pollMessage == null) {
                pollMessage = nextTableMessage();
                if (pollMessage == null) {
                    pollMessage = engineMessage();
//                    measurmentIndex = 0;
                    tableIndex = 0;
                }
            }
            return pollMessage;
        }

        private JSONObject nextMeasurementMessage() throws JSONException {
            return messageObject(GET_MEASUREMENTS);
        }

        private JSONObject nextTableMessage() throws JSONException {
            JSONObject pollMessage = null;
            List<Table> tables;
            synchronized (tablesToPoll) {
                tables = new ArrayList<>(tablesToPoll);
            }
            if (tableIndex < tables.size()) {
                Table table = tables.get(tableIndex);
                pollMessage = messageObject(GET_TABLE_PROPERTIES, TABLE_NAME, table.getName());
                tableIndex++;
            }
            return pollMessage;
        }
        
        private JSONObject engineMessage() throws JSONException {
            if (pollEngine) {
                return messageObject(IS_ENGINE_RUNNING);
            }
            else {
                return null;
            }
        }
        
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
                    updateMeasurementProperties(response);
                }
                else if (function.equals(GET_TABLE_PROPERTIES)) {
                    updateTableIndex(returnValue);
//                    updateTable(response);
                }
                else if (function.equals(SET_TABLE_FIELD)) {
                    updateTableField(response);
                }
                else if (function.equals("IsEngineRunning")) {
                    engine.setRunning(response.getBoolean(VALUE));
                }
                else if (function.equals(GET_TABLE_NAMES)) {
                    notifyTableNames(response);
                }
                else if (function.equals(GET_ENGINE_PROPERTIES)) {
                    updateEngine(response);
                }
//                else if (function.equals(FLASH)) {
//                    updateFlash(response);
//                }
                else if (function.equals(GET_PERSISTENT_ELEMENTS)) {
                    updateFlashElements(response);
                }
                else if (response.has(ENABLED)) {
                    updateTableEnabled(response);
                }
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
                updateTableIndex(response);
            }
        }

        private void notifyTableNames(JSONObject response) throws JSONException {
            Collection<String> names = new ArrayList<>();
            JSONArray namesArray = response.getJSONArray(Messenger.RETURN_VALUE);
            for (int i = 0; i < namesArray.length(); ++i) {
                names.add(namesArray.getString(i));
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
            catch (InterruptedException | JSONException ex) {
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
    private static final String SET_TABLE_FIELD = "SetTableField";
    private static final String GET_TABLE_PROPERTIES = "GetTableProperties";
    private static final String GET_TABLE_NAMES = "GetTableNames";
    private static final String GET_ENGINE_PROPERTIES = "GetEngineProperties";
    private static final String IS_ENGINE_RUNNING = "IsEngineRunning";
    private static final String GET_PERSISTENT_ELEMENTS = "GetPersistentElements";

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

    private static final String COLUMN = "Column";
    private static final String ROW = "Row";
    private static final String VALUE = "Value";
//    
//    private static final String COLUMN_MEASUREMENT_NAME = "ColumnMeasurement";
//    private static final String ROW_MEASUREMENT_NAME = "RowMeasurement";
//    
    private static final String FORMAT = "Format";
    private static final String MINIMUM = "Minimum";
    private static final String MAXIMUM = "Maximum";
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
