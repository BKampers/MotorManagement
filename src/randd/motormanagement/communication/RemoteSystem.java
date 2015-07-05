/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.communication;

import java.util.*;
import org.json.*;
import randd.motormanagement.system.*;


public class RemoteSystem {

    
    public static final String OK = "OK";

    
    public interface Listener {
        
        void notificationReceived(Notification notification);
        
    }
    
    
    public RemoteSystem(Transporter transporter) {
        assert(transporter != null);
        messenger = new Messenger(transporter);
        messenger.setListener(new MessengerListener());
    }
    
    
    public void connect() throws bka.communication.ChannelException  {
        messenger.open();
    }
    
    
    public void disconnect() throws bka.communication.ChannelException {
        stopPolling();
        messenger.close();
    }

    
    public void startPolling() {
        if (pollTask == null) {
            pollTask = new PollTask();
            Thread pollThread = new Thread(pollTask);
            pollThread.start();
        }
    }
    
    
    public void stopPolling() {
        if (pollTask != null) {
            pollTask.stop();
            pollTask = null;
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
    
    
    public Collection<String> requestTableNames() throws InterruptedException, JSONException {
        ArrayList<String> names = new ArrayList<>();
        JSONObject namesObject = request(MEASUREMENT_TABLES);
        JSONArray namesArray = namesObject.optJSONArray(NAMES);
        if (namesArray != null) {
            for (int i = 0; i < namesArray.length(); ++i) {
                String name = namesArray.optString(i);
                if (name != null) {
                    names.add(name);
                }
            }
        }
        return names;
    }
    
    
    public void requestTable(Table table) throws InterruptedException, JSONException {
        final JSONArray TABLE_PROPERTY = new JSONArray(new String[] {TABLE});
        JSONObject tableObject = request(table.getName(), PROPERTIES, TABLE_PROPERTY);
        updateTable(tableObject);
    }
    
    
    public String modifyTable(Table table, int column, int row, float value) throws JSONException, InterruptedException {
        return modify(table.getName(),
            COLUMN, column,
            ROW, row,
            VALUE, value);
    }
    
    
    public void requestTableEnabled(Table table) throws InterruptedException, JSONException {
        final JSONArray ENABLED_PROPERTY = new JSONArray(new String[] {ENABLED});
        JSONObject tableObject = request(table.getName(), PROPERTIES, ENABLED_PROPERTY);
        updateTableEnabled(tableObject);
    }
    
    
    public void requestEngine(Engine engine) throws InterruptedException, JSONException {
        JSONObject engineObject = request(ENGINE);
        updateEngine(engine, engineObject);
    }
    
    
    public String modifyCylinderCount(int count) throws InterruptedException, JSONException {
        return modify(CYLINDER_COUNT, VALUE, count);
    }
    
    
    public String modifyCogwheel(int cogTotal, int gapSize, int offset) throws InterruptedException, JSONException {
        return modify(COGWHEEL, COG_TOTAL, cogTotal, GAP_SIZE, gapSize, OFFSET, offset);
    }
    

    public String enableTable(Table table, boolean enabled) throws JSONException, InterruptedException {
        return modify(table.getName(), ENABLED, enabled);
    }

    
    public String enableMeasurementSimulation(Measurement measurement, boolean enable) throws JSONException, InterruptedException {
        if (enable) {
            return modify(measurement.getName(), SIMULATION, true, VALUE, measurement.getValue());
        }
        else {
            return modify(measurement.getName(), SIMULATION, false);            
        }
    }
    
    
    public String setMeasurementSimulationValue(Measurement measurement, double value) throws JSONException, InterruptedException {
        return modify(measurement.getName(), SIMULATION, true, VALUE, value);
    }
    
    
    public String modifyIgnitionTimerSettings(TimerSettings settings) throws JSONException, InterruptedException {
        return modify("IgnitionTimer",
            "Prescaler", settings.getPrescaler(),
            "Period", settings.getPeriod(),
            "Counter", settings.getCounter());
    }
    
    
    public void requestFlash(Flash flash) throws InterruptedException, JSONException {
        JSONObject flashObject = request(FLASH);
        JSONObject elementsObject = request(FLASH_ELEMENTS);
        updateFlash(flash, flashObject, elementsObject);
    }
    
    
    public String modifyFlash(int reference, int count, int value) throws JSONException, InterruptedException {
        return modify(FLASH, REFERENCE, reference, COUNT, count, VALUE, value);
    }
    
    
    public String modifyFlash(int reference, int[] value) throws JSONException, InterruptedException {
        return modify(FLASH, REFERENCE, reference, VALUE, value);
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
    
    
    private JSONObject request(String subject, Object ... options) throws JSONException, InterruptedException {
        return send(REQUEST, subject, options);
    }
    
    
    private String modify(String subject, Object ... options) throws JSONException, InterruptedException {
        JSONObject responseObject = send(MODIFY, subject, options);
        return responseObject.getString(STATUS);
    }
    
    
    private JSONObject send(String message, String subject, Object ... options) throws JSONException, InterruptedException {
        JSONObject messageObject = messageObject(message, subject, options);
        return messenger.send(messageObject);
    }
    
    
    private JSONObject messageObject(String message, String subject, Object ... options) throws JSONException {
        JSONObject object = new JSONObject();
        object.put(Messenger.MESSAGE, message);
        object.put(Messenger.SUBJECT, subject);
        for (int i = 0; i < options.length - 1; i += 2)
        {
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
    

    private void updateTable(JSONObject object) throws JSONException {
        Table table = Table.getInstance(object.getString(Messenger.SUBJECT));
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
    
    
    private void updateEngine(Engine engine, JSONObject engineObject) throws JSONException {
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
    

    private void updateFlash(Flash flash, JSONObject flashObject, JSONObject elementsObject) throws JSONException {
        JSONArray memoryArray = flashObject.getJSONArray(VALUE);
        int length = memoryArray.length();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; ++i) {
            bytes[i] = (byte) memoryArray.getInt(i);
        }
        JSONArray elementsArray = elementsObject.getJSONArray(ELEMENTS);
        length = elementsArray.length();
        Flash.Element[] elements = new Flash.Element[length];
        for (int i = 0; i < length; ++i) {
            JSONObject elementObject = elementsArray.getJSONObject(i);
            elements[i] = new Flash.Element(
                elementObject.getInt(TYPE_ID),
                elementObject.getInt(REFERENCE),
                elementObject.getInt(SIZE));
        }
        flash.setBytes(bytes);
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

    
    private class PollTask implements Runnable {
        
        @Override
        public void run() {
            while (running) {
                try {
                    if (index < MEASUREMENTS.length) {
                        JSONObject message = messageObject(REQUEST, MEASUREMENTS[index].getName());
                        JSONObject response = messenger.send(message);
                        updateMeasurement(response);
                        index++;
                    }
                    else {
                        Collection<Table> tables;
                        synchronized (tablesToPoll) {
                            tables = new ArrayList<>(tablesToPoll);
                        }
                        for (Table table : tables) {
                            final JSONArray INDEX_PROPERTY = new JSONArray(new String[] {INDEX});
                            JSONObject message = messageObject(REQUEST, table.getName(), PROPERTIES, INDEX_PROPERTY);
                            JSONObject response = messenger.send(message);
                            updateTableIndex(response);
                        }
                        index = 0;
                    }
                }
                catch (InterruptedException | JSONException ex) {
                    ex.printStackTrace(System.err);
                }
            }
        }
        
        void stop() {
            running = false;
        }
        
        private volatile boolean running = true;
        private int index = 0;
    }
    
    
    private class MessengerListener implements Messenger.Listener {

        @Override
        public void notifyMessage(JSONObject message) {
            if (NOTIFICATION.equals(message.optString(Messenger.MESSAGE))) {
                Iterator keys = message.keys();
                while (keys.hasNext()) {
                    String key = keys.next().toString();
                    if (! Messenger.MESSAGE.equals(key)) {
                        Notification notification = new Notification(key, message.optString(key));
                        synchronized (listeners) {
                            for (Listener listener : listeners) {
                                listener.notificationReceived(notification);
                            }
                        }
                    }                
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
        }
        
    }
    
    
    private final Collection<Table> tablesToPoll = new ArrayList<>();
    private final Collection<Listener> listeners = new ArrayList<>();

    
    private PollTask pollTask = null;
    
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

    
    private static final String STATUS = "Status";
    private static final String NOTIFICATION = "Notification";
    
    private static final String REQUEST = "Request";
    private static final String MODIFY = "Modify";
    
    private static final String PROPERTIES = "Properties";
    private static final String VALUE = "Value";
    private static final String TABLE = "Table";
    private static final String DECIMALS = "Decimals";
    private static final String INDEX = "Index";
    private static final String SIMULATION = "Simulation";
    private static final String ENABLED = "Enabled";
    
    private static final String MEASUREMENT_TABLES = "MeasurementTables";
    private static final String FLASH = "Flash";
    private static final String FLASH_ELEMENTS = "FlashElements";
    
    private static final String NAMES = "Names";
    private static final String ELEMENTS = "Elements";
    private static final String COUNT = "Count";
    private static final String REFERENCE = "Reference";
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
    
}
