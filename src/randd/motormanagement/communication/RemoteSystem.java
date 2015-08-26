/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.communication;

import randd.motormanagement.system.*;

import java.util.*;
import java.util.logging.*;
import org.json.*;


public class RemoteSystem {
    
    public static final String OK = "OK";
    public static final String REFERENCE = "Reference";
    public static final String VALUE = "Value";
    public static final String CORRECTION_SUFFIX = "Correction";

    
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

    
    public void startPolling() {
        if (pollTimer == null) {
            PollTask pollTask = new PollTask();
            pollEngine = true;
            pollTimer = new Timer();
            pollTimer.schedule(pollTask, 500, 500);
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
        request(MEASUREMENT_TABLES);
    }
    
    
    public void requestTable(Table table) throws InterruptedException, JSONException {
        final JSONArray TABLE_PROPERTY = new JSONArray(new String[] {TABLE});
        request(table.getName(), PROPERTIES, TABLE_PROPERTY);
    }
    
    
    public void modifyTable(Table table, int column, int row, float value) throws JSONException, InterruptedException {
        modify(table.getName(), COLUMN, column, ROW, row, VALUE, value);
    }
    
    
    public void requestTableEnabled(Table table) throws InterruptedException, JSONException {
        final JSONArray ENABLED_PROPERTY = new JSONArray(new String[] {ENABLED});
        request(table.getName(), PROPERTIES, ENABLED_PROPERTY);
    }
    
    
    public void requestEngine() throws InterruptedException, JSONException {
        request(ENGINE);
    }
    
    
    public void modifyCylinderCount(int count) throws InterruptedException, JSONException {
        modify(CYLINDER_COUNT, VALUE, count);
    }
    
    
    public void modifyCogwheel(int cogTotal, int gapSize, int offset) throws InterruptedException, JSONException {
        modify(COGWHEEL, COG_TOTAL, cogTotal, GAP_SIZE, gapSize, OFFSET, offset);
    }
    

    public void enableTable(Table table, boolean enabled) throws JSONException, InterruptedException {
        modify(table.getName(), ENABLED, enabled);
    }

    
    public void enableMeasurementSimulation(Measurement measurement, boolean enable) throws JSONException, InterruptedException {
        if (enable) {
            Float value = measurement.getValue();
            if (value == null) {
                value = 0.0f;
            }
            modify(measurement.getName(), SIMULATION, true, VALUE, value);
        }
        else {
            modify(measurement.getName(), SIMULATION, false);            
        }
    }
    
    
    public void setMeasurementSimulationValue(Measurement measurement, double value) throws JSONException, InterruptedException {
        modify(measurement.getName(), SIMULATION, true, VALUE, value);
    }
    
    
    public void requestFlash() throws InterruptedException, JSONException {
        request(FLASH);
        request(FLASH_ELEMENTS);
    }
    
    
    public void modifyFlash(int reference, int count, int value) throws JSONException, InterruptedException {
        modify(FLASH, REFERENCE, reference, COUNT, count, VALUE, value);
    }
    
    
    public void modifyFlash(int reference, int[] values) throws JSONException, InterruptedException {
        int index = 0;
        int total = values.length;
        while (index < total) {
            int count = Math.min(total - index, MAX_FLASH_SIZE_TO_SEND);
            int[] valuesToSend = Arrays.copyOfRange(values, index, index + count);
            modify(FLASH, REFERENCE, reference, VALUE, valuesToSend);
            reference += count;
            index += count;
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
    
    
    private void request(String subject, Object ... options) throws JSONException, InterruptedException {
        send(REQUEST, subject, options);
    }
    
    
    private void modify(String subject, Object ... options) throws JSONException, InterruptedException {
        send(MODIFY, subject, options);
    }
    
    
    private void send(String message, String subject, Object ... options) throws JSONException {
        JSONObject messageObject = messageObject(message, subject, options);
        logger.log(Level.FINEST, ">> {0}", messageObject);
        messenger.send(messageObject);
    }
    
    
    private JSONObject messageObject(String message, String subject, Object ... options) throws JSONException {
        JSONObject object = new JSONObject();
        object.put(Messenger.MESSAGE, message);
        object.put(Messenger.SUBJECT, subject);
        for (int i = 0; i < options.length - 1; i += 2) {
            object.put(options[i].toString(), options[i+1]);
        }
        return object;
    }
    
    
    private void updateMeasurement(JSONObject measurementObject) throws JSONException {
        Measurement measurement = Measurement.getInstance(measurementObject.optString(Messenger.SUBJECT));
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
            double value = measurementObject.optDouble(VALUE);
            if (value != Double.NaN) {
                measurement.setValue((float) value);
            }
            boolean simulation = measurementObject.optBoolean(SIMULATION, false);
            measurement.setSimulationEnabled(simulation);
        }
    }
    

    private void updateTableField(JSONObject object) throws JSONException {
        Table table = Table.getInstance(object.getString(SUBJECT));
        table.setField(object.getInt(COLUMN), object.getInt(ROW), (float) object.getDouble(VALUE));
    }
    
    
    private void updateTable(JSONObject object) throws JSONException {
        Table table = Table.getInstance(object.getString(SUBJECT));
        JSONArray tableArray = object.getJSONArray(TABLE);
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
        table.setDecimals(object.optInt(DECIMALS, 0));
        table.setMinimum((float) object.optDouble(MINIMUM, 0.0));
        table.setMaximum((float) object.optDouble(MAXIMUM, 100.0));
        String measurementName = object.optString(COLUMN_MEASUREMENT_NAME);
        if (measurementName != null) {
            Measurement measurement = Measurement.getInstance(measurementName);
            table.setColumnMeasurement(measurement);
        }
        measurementName = object.optString(ROW_MEASUREMENT_NAME);
        if (measurementName != null) {
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
    

    private void updateFlash(JSONObject flashObject) throws JSONException {
        int reference = flashObject.getInt(REFERENCE);
        JSONArray memoryArray = flashObject.getJSONArray(VALUE);
        int length = memoryArray.length();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; ++i) {
            bytes[i] = (byte) memoryArray.getInt(i);
        }
        flash.setBytes(reference, bytes);
    }
    
    
    private void updateFlashElements(JSONObject elementsObject) throws JSONException {
        JSONArray elementsArray = elementsObject.getJSONArray(ELEMENTS);
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
        Table table = Table.getInstance(object.getString(Messenger.SUBJECT));
        table.setColumnIndex(object.getInt(COLUMN));
        table.setRowIndex(object.getInt(ROW));
    }
    
    
    private void updateTableEnabled(JSONObject object) throws JSONException {
        Table table = Table.getInstance(object.getString(Messenger.SUBJECT));
        table.setEnabled(object.getBoolean(ENABLED));
    }
    
    
    private class PollTask extends TimerTask /*implements Runnable */{
        
        @Override
        public void run() {
            try {
                message = nextMessage();
                if (message != null) {
                    logger.log(Level.FINEST, ">> {0}", message);
                    messenger.send(message);
                }
            }
            catch (JSONException ex) {
                logger.log(Level.WARNING, "PollTask", ex);
            }
        }
        
        private JSONObject nextMessage() throws JSONException {
            JSONObject pollMessage = nextMeasurementMessage();
            if (pollMessage == null) {
                pollMessage = nextTableMessage();
                if (pollMessage == null) {
                    pollMessage = engineMessage(pollMessage);
                    measurmentIndex = 0;
                    tableIndex = 0;
                }
            }
            return pollMessage;
        }

        private JSONObject nextMeasurementMessage() throws JSONException {
            JSONObject pollMessage = null;
            if (measurmentIndex < MEASUREMENTS.length) {
                pollMessage = messageObject(REQUEST, MEASUREMENTS[measurmentIndex].getName());
                measurmentIndex++;
            }
            return pollMessage;
        }

        private JSONObject nextTableMessage() throws JSONException {
            JSONObject pollMessage = null;
            List<Table> tables;
            synchronized (tablesToPoll) {
                tables = new ArrayList<>(tablesToPoll);
            }
            if (tableIndex < tables.size()) {
                Table table = tables.get(tableIndex);
                final JSONArray INDEX_PROPERTY = new JSONArray(new String[] {INDEX});
                pollMessage = messageObject(REQUEST, table.getName(), PROPERTIES, INDEX_PROPERTY);
                tableIndex++;
            }
            return pollMessage;
        }
        
        private JSONObject engineMessage(JSONObject pollMessage) throws JSONException {
            if (pollEngine) {
                pollMessage = messageObject(REQUEST, ENGINE_IS_RUNNING);
            }
            return pollMessage;
        }
        
        private int measurmentIndex = 0;
        private int tableIndex = 0;
        private JSONObject message = null;
    }


    private class MessengerListener implements Messenger.Listener {

        @Override
        public void notifyMessage(JSONObject message) {
            logger.log(Level.FINEST, "<< {0}", message);
            if (NOTIFICATION.equals(message.optString(Messenger.MESSAGE))) {
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
            logger.log(Level.FINEST, "<< {0}", response);
            try {
                String subject = response.getString(SUBJECT);
                if (getMeasurement(subject) != null) {
                    updateMeasurement(response);
                }
                else if (response.has(COLUMN) && response.has(ROW)) {
                    updateActiveTable(response);
                }
                else if (response.has(TABLE)) {
                    updateTable(response);
                }
                else if (subject.equals(ENGINE_IS_RUNNING)) {
                    engine.setRunning(response.getBoolean(VALUE));
                }
                else if (subject.equals(MEASUREMENT_TABLES)) {
                    notifyTableNames(response);
                }
                else if (subject.equals(ENGINE)) {
                    updateEngine(response);
                }
                else if (subject.equals(CYLINDER_COUNT)) {
                    engine.setCylinderCount(response.getInt(VALUE));
                    requestEngineUpdate();
                }
                else if (subject.equals(COGWHEEL)) {
                    engine.setCogwheel(response.getInt(COG_TOTAL), response.getInt(GAP_SIZE), response.getInt(OFFSET));
                    requestEngineUpdate();
                }
                else if (subject.equals(FLASH)) {
                    updateFlash(response);
                }
                else if (subject.equals(FLASH_ELEMENTS)) {
                    updateFlashElements(response);
                }
                else if (response.has(ENABLED)) {
                    updateTableEnabled(response);
                }
                else {
                    logger.log(Level.WARNING, "Unhandled: {0}", response);
                }
            }
            catch (JSONException ex) {
                logger.log(Level.WARNING, response.toString(), ex);
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
                if (! Messenger.MESSAGE.equals(key)) {
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
            JSONArray namesArray = response.getJSONArray(NAMES);
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
                logger.log(Level.WARNING, "requestEngineUpdate", ex);
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
        Measurement.getInstance("Map")/*,
        Measurement.get("Lambda"),
        Measurement.get("Aux1"),
        Measurement.get("Aux2")*/
    };
        
    
    private static final String NOTIFICATION = "Notification";
    private static final String SUBJECT = "Subject";
    
    private static final String REQUEST = "Request";
    private static final String MODIFY = "Modify";
    
    private static final String PROPERTIES = "Properties";
    private static final String TABLE = "Table";
    private static final String DECIMALS = "Decimals";
    private static final String INDEX = "Index";
    private static final String SIMULATION = "Simulation";
    private static final String ENABLED = "Enabled";
    
    private static final String MEASUREMENT_TABLES = "MeasurementTables";
    private static final String FLASH = "Flash";
    private static final String FLASH_ELEMENTS = "FlashElements";
    private static final String ENGINE_IS_RUNNING = "EngineIsRunning";
    
    private static final String NAMES = "Names";
    private static final String ELEMENTS = "Elements";
    private static final String COUNT = "Count";
    private static final String SIZE = "Size";
    private static final String TYPE_ID = "TypeId";
    
    private static final String COLUMN = "Column";
    private static final String ROW = "Row";
    
    private static final String COLUMN_MEASUREMENT_NAME = "ColumnMeasurement";
    private static final String ROW_MEASUREMENT_NAME = "RowMeasurement";
    
    private static final String FORMAT = "Format";
    private static final String MINIMUM = "Minimum";
    private static final String MAXIMUM = "Maximum";

    private static final String ENGINE = "Engine";
    private static final String COGWHEEL = "Cogwheel";
    private static final String COG_TOTAL = "CogTotal";
    private static final String GAP_SIZE = "GapSize";
    private static final String OFFSET = "Offset";
    private static final String DEAD_POINTS = "DeadPoints";
    private static final String CYLINDER_COUNT = "CylinderCount";
    
    
    private static final int MAX_FLASH_SIZE_TO_SEND = 0x10;
    
    private static final Logger logger = Logger.getLogger(RemoteSystem.class.getName());

}
