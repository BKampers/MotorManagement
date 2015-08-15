/*
** Copyright © Bart Kampers
**
** For distribution make sure that: 
** - win32com.dll is in dist directory
** - javax.comm.properties is in dist\lib directory
*/

package randd.motormanagement.swing;

import bka.communication.Channel;
import bka.communication.ChannelException;
import bka.communication.SerialPortChannel;
import bka.communication.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import randd.motormanagement.communication.RemoteSystem;
import randd.motormanagement.communication.Transporter;
import randd.motormanagement.system.Engine;
import randd.motormanagement.system.Measurement;
import randd.motormanagement.system.Notification;
import randd.motormanagement.system.Table;


public class Monitor extends bka.swing.FrameApplication {

    public static final int RANDD_MM_PORT = 44252;
    public static final int RANDD_CONTROL_PORT = RANDD_MM_PORT - 1;
    
    
    public static void main(final String arguments[]) {
        setLookAndFeel("Nimbus");
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Monitor monitor = new Monitor();
                monitor.parseArguments(arguments);
                if (monitor.loadedFromJar()) {
                    /* Make comm libray find system's com ports */
                    String classPath = System.getProperty("java.class.path");
                    classPath += ";.\\lib\\comm.jar";
                    System.setProperty("java.class.path", classPath);
                }
                monitor.setVisible(true);
            }
        });
    }

    
    @Override
    public String manufacturerName() {
        return "Randd";
    }
    
    
    @Override
    public String applicationName() {
        return "Motor Management";
    }
    
    
    @Override
    protected void opened() {
        setupLogging();
        populateChannelComboBox();
        selectStoredChannel();
        initializePanels();
    }

    
    RemoteSystem getRemoteSystem() {
        return remoteSystem;
    }
    
    
    private Collection<String> getLogPaths(Level level) {
        String property = getProperty(level.getName());
        return (property != null) ? Arrays.asList(property.split(";")) : null;
    }
    
    
    private Map<Level, Collection<String>> getLogLevelMap() {
        Map<Level, Collection<String>> map = new HashMap<>();
        map.put(Level.OFF, getLogPaths(Level.OFF));
        map.put(Level.SEVERE, getLogPaths(Level.SEVERE));
        map.put(Level.WARNING, getLogPaths(Level.WARNING));
        map.put(Level.INFO, getLogPaths(Level.INFO));
        map.put(Level.CONFIG, getLogPaths(Level.CONFIG));
        map.put(Level.FINE, getLogPaths(Level.FINE));
        map.put(Level.FINER, getLogPaths(Level.FINER));
        map.put(Level.FINEST, getLogPaths(Level.FINEST));
        map.put(Level.ALL, getLogPaths(Level.ALL));
        return map;
    }
    

    private void setupLogging() {
        try {
            randd.motormanagement.logging.Manager.setup(getLogLevelMap());
        }
        catch (java.io.IOException ex) {
            handle(ex);
        }
    }

    
    private void initializePanels() {
        boolean developerMode = getBooleanProperty(DEVELOPER_MODE, false);
        channelComboBox.setEditable(developerMode);
        addMeasurementPanel("RPM", developerMode);
        addMeasurementPanel("Load", developerMode);
        addMeasurementPanel("Water", developerMode);
        addMeasurementPanel("Air", developerMode);
        addMeasurementPanel("Battery", developerMode);
        addMeasurementPanel("Map", developerMode);
//        addMeasurementPanel("Lambda")));
//        addMeasurementPanel("Aux1")));
//        addMeasurementPanel("Aux2")));
        addEngineTabPanel();
        if (developerMode) {
            addTabPanel(memoryPanel, "Flash");
            addTabPanel(statusPanel, "Status");
            addTabPanel(new ControlPanel(), "Control");
        }
    }
    

    private void handle(Throwable throwable) {
        JOptionPane.showMessageDialog(this, throwable.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    
    private String title() {
        return manufacturerName() + " " + applicationName();
    }
    
    
    private Monitor() {
        initComponents();
        tabsPanel.addChangeListener(new TabChangeListener());
    }
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        toolsPanel = new javax.swing.JPanel();
        channelComboBox = new javax.swing.JComboBox();
        tabsPanel = new javax.swing.JTabbedPane();
        valuesPanel = new javax.swing.JPanel();
        settingsPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(title());
        setMinimumSize(new java.awt.Dimension(1000, 500));

        toolsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        channelComboBox.setMinimumSize(new java.awt.Dimension(75, 20));
        channelComboBox.setPreferredSize(new java.awt.Dimension(100, 20));
        channelComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                channelComboBox_actionPerformed(evt);
            }
        });
        toolsPanel.add(channelComboBox);

        getContentPane().add(toolsPanel, java.awt.BorderLayout.NORTH);
        getContentPane().add(tabsPanel, java.awt.BorderLayout.CENTER);

        valuesPanel.setLayout(new javax.swing.BoxLayout(valuesPanel, javax.swing.BoxLayout.Y_AXIS));
        getContentPane().add(valuesPanel, java.awt.BorderLayout.EAST);
        getContentPane().add(settingsPanel, java.awt.BorderLayout.WEST);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    private void channelComboBox_actionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_channelComboBox_actionPerformed
        Object selectedItem = channelComboBox.getSelectedItem();
        if (selectedItem == NO_SELECTION) {
            disconnect();
        }
        else {
            if (! (selectedItem instanceof Channel)) {
                // Value typed
                String host = selectedItem.toString();
                Collection<String> hosts = (Collection<String>) getSetting(SOCKET_HOSTS);
                if (hosts == null) {
                    hosts = new ArrayList<>();
                    setSetting(SOCKET_HOSTS, hosts);
                }
                if (! hosts.contains(host)) {
                    hosts.add(host);
                }            
                selectedItem = SocketChannel.create(selectedItem.toString(), RANDD_MM_PORT);
            }
            if (selectedItem != selectedChannel) {
                disconnect();
                connect((Channel) selectedItem);
            }
        }
    }//GEN-LAST:event_channelComboBox_actionPerformed


    private void populateChannelComboBox() {
        channelComboBox.removeAllItems();
        channelComboBox.addItem(NO_SELECTION);
        for (SerialPortChannel channel : SerialPortChannel.findAll()) {
            channelComboBox.addItem(channel);
        }
        Collection<String> socketHosts = (Collection<String>) getSetting(SOCKET_HOSTS);
        if (socketHosts != null) {
            for (String host : socketHosts) {
                channelComboBox.addItem(SocketChannel.create(host, RANDD_MM_PORT));
            }
        }
    }
    
    
    private void selectStoredChannel() {
        String channelName = getProperty(SELECTED_CHANNEL);
        if (channelName != null) {
            int i = 0;
            while (selectedChannel == null && i < channelComboBox.getItemCount()) {
                Object comboBoxItem = channelComboBox.getItemAt(i);
                if (comboBoxItem.toString().equals(channelName)) {
                    channelComboBox.setSelectedItem(comboBoxItem);
                }
                i++;
            }
        }
    }
    
    private void disconnect() {
        try {
            if (remoteSystem != null) {
                remoteSystem.disconnect();
                remoteSystem = null;
                selectedChannel = null;
                setProperty(SELECTED_CHANNEL, null);
                tabsPanel.removeAll();
            }
        }
        catch (ChannelException ex) {
            handle(ex);
        }
    }

    
    private void connect(Channel selectedItem) {
        selectedChannel = selectedItem;
        try {
            Transporter transporter = new Transporter(selectedItem, title());
            remoteSystem = new RemoteSystem(transporter);
            remoteSystem.connect();
            setProperty(SELECTED_CHANNEL, transporter.getName());
            memoryPanel.setMemory(remoteSystem.getFlash());
            activateSelectedTab();
            statusPanel.setRemoteSystem(remoteSystem);
            remoteSystem.addListener(new RemoteSystemListener());
            remoteSystem.requestTableNames();
            if (getBooleanProperty(LIVE_MODE, true)) {
                remoteSystem.startPolling();
            }
        }
        catch (ChannelException | InterruptedException | JSONException ex) {
            handle(ex);
        }
    }
    
    
    private void addMeasurementPanel(String measurementName, boolean developerMode) {
        Measurement measurement = Measurement.getInstance(measurementName);
        Table correctionTable = getCorrectionTable(measurement);
        MeasurementPanel panel = new MeasurementPanel(measurement, correctionTable, measurementPanelListener, developerMode);
        valuesPanel.add(panel);
        Table table = getCorrectionTable(measurement);
        if (table != null) {
            try {
                remoteSystem.requestTableEnabled(table);
            }
            catch (InterruptedException | org.json.JSONException ex) {
                handle(ex);
            }
        }
    }
    
    
    private Table getCorrectionTable(Measurement measurement) {
        assert (measurement != null);
        String measurementName = measurement.getName();
        if (! "Load".equals(measurementName) && ! "RPM".equals(measurementName)) {
            return Table.getInstance(measurementName + RemoteSystem.CORRECTION_SUFFIX);
        }
        else {
            return null;
        }
    }
    

    private void addTableTabPanel(String name) {
        Table table = Table.getInstance(name);
        addTabPanel(new TablePanel(new TablePanelListener(), table), name);
    }
    
    
    private void addEngineTabPanel() {
        EnginePanel panel = new EnginePanel(remoteSystem.getEngine());
        panel.setListener(new EnginePanelListener());
        addTabPanel(panel, "Engine");
    }
    
    
    private void  addTabPanel(JPanel panel, String titleKey) {
        tabsPanel.add(panel);
        tabsPanel.setTitleAt(tabsPanel.getTabCount() - 1, Bundle.getInstance().get(titleKey));
    }
    
    private void activateSelectedTab() {
        if (remoteSystem != null) {
            try {
                if (selectedTab instanceof TablePanel) {
                    Table table = ((TablePanel) selectedTab).getTable();
                    remoteSystem.stopIndexPoll(table);
                }
                selectedTab = tabsPanel.getSelectedComponent();
                if (selectedTab instanceof TablePanel) {
                    Table table = ((TablePanel) selectedTab).getTable();
                    if (! table.hasFields()) {
                        remoteSystem.requestTable(table);
                    }
                    remoteSystem.startIndexPoll(table);
                }
                else if (selectedTab instanceof EnginePanel) {
                    remoteSystem.requestEngine();
                    ((EnginePanel) selectedTab).activate();
                }
                else if (selectedTab instanceof MemoryPanel) {
                    remoteSystem.requestFlash();
                }
            }
            catch (InterruptedException | org.json.JSONException ex) {
                handle(ex);
            }
        }
    }
    
    
    private String loadTextFile() {
        String source = null;
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Resource resource = new Resource(fileChooser.getSelectedFile());
            try {
                source = resource.loadText();
            }
            catch (java.io.IOException ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
        return source;
    }
    

    private class TabChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent evt) {
            activateSelectedTab();
        }

    }
    
    
    private class MeasurementPanelListener implements MeasurementPanel.Listener {
        
        @Override
        public void tableEnabled(MeasurementPanel panel, boolean enabled) {
            Measurement measurement = panel.getMeasurement();
            Table table = getCorrectionTable(measurement);
            try {
                remoteSystem.enableTable(table, enabled);
            }
            catch (org.json.JSONException | InterruptedException ex) {
                handle(ex);
            }
        }

        @Override
        public void simulationEnabled(MeasurementPanel panel, boolean enabled) {
            Measurement measurement = panel.getMeasurement();
            try {
                if (measurement.isSimulationEnabled() != enabled) {
                    remoteSystem.enableMeasurementSimulation(measurement, enabled);
                }
            }
            catch (org.json.JSONException | InterruptedException ex) {
                handle(ex);
            }
        }

        @Override
        public void simulationValueModified(MeasurementPanel panel, double value) {
            try {
                remoteSystem.setMeasurementSimulationValue(panel.getMeasurement(), value);
            }
            catch (org.json.JSONException | InterruptedException ex) {
                handle(ex);
            }
        }
        
    }
    
    
    private class TablePanelListener implements TablePanel.Listener {
        
        @Override
        public void startIndexPoll(Table table) {
            assert remoteSystem != null;
            remoteSystem.startIndexPoll(table);
        }
        
        @Override
        public void setValue(Table table, int column, int row, float value) {
            try {
                remoteSystem.modifyTable(table, column, row, value);
            }
            catch (org.json.JSONException | InterruptedException ex) {
                handle(ex);
            }
        }
        
    }
    
    
    private class EnginePanelListener implements EnginePanel.Listener {

        @Override
        public void cylinderCountModified(int count) {
            try {
                remoteSystem.modifyCylinderCount(count);
            }
            catch (org.json.JSONException | InterruptedException ex) {
                handle(ex);
            }
        }

        @Override
        public void cogwheelTypeModified(int cogTotal, int gapSize) {
            Engine.Cogwheel cogwheel = remoteSystem.getEngine().getCogwheel();
            if (cogwheel.getCogTotal() != cogTotal || cogwheel.getGapSize() != gapSize) {
                modifyCogwheel(cogTotal, gapSize, cogwheel.getOffset());
            }
        }

        @Override
        public void offsetModified(int offset) {
            Engine.Cogwheel cogwheel = remoteSystem.getEngine().getCogwheel();
            if (cogwheel.getOffset() != offset) {
                modifyCogwheel(cogwheel.getCogTotal(), cogwheel.getGapSize(), offset);
            }
        }
        
        private void modifyCogwheel(int cogTotal, int gapSize, int offset) {
            try {
                remoteSystem.modifyCogwheel(cogTotal, gapSize, offset);
            }
            catch (org.json.JSONException | InterruptedException ex) {
                handle(ex);
            }
        }
        
    }
    

    private final MemoryPanel memoryPanel = new MemoryPanel(new MemoryPanel.Listener() {

        @Override
        public void clearButtonPressed() {
            try {
                remoteSystem.modifyFlash(0, remoteSystem.getFlash().getSize(), 0xAA);
            }
            catch (org.json.JSONException | InterruptedException ex) {
                handle(ex);
            }
            
        }
        
        @Override
        public void loadButtonPressed() {
            try {
                String source = loadTextFile();
                JSONObject jsonObject = new JSONObject(source);
                int reference = jsonObject.getInt(RemoteSystem.REFERENCE);
                int[] values = getIntArray(jsonObject.getJSONArray(RemoteSystem.VALUE));
                remoteSystem.modifyFlash(reference, values);
            }
            catch (JSONException | InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }        

        private int[] getIntArray(JSONArray jsonArray) throws JSONException {
            int length = jsonArray.length();
            int[] intArray = new int[length];
            for (int i = 0; i < length; ++i) {
                intArray[i] = jsonArray.getInt(i);
            }
            return intArray;
        }
        
    });
    
    
    private class RemoteSystemListener implements RemoteSystem.Listener {

        @Override
        public void notificationReceived(Notification notification) {
        }
        
        @Override
        public void tableNames(Collection<String> names) {
            for (String name : names) {
                addTableTabPanel(name);
            }
            
        }
        
    }


    private final StatusPanel statusPanel = new StatusPanel();
    
    private RemoteSystem remoteSystem = null;
    private Channel selectedChannel = null;
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox channelComboBox;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JTabbedPane tabsPanel;
    private javax.swing.JPanel toolsPanel;
    private javax.swing.JPanel valuesPanel;
    // End of variables declaration//GEN-END:variables

    
    private java.awt.Component selectedTab = null;
    
    private final MeasurementPanelListener measurementPanelListener = new MeasurementPanelListener();
    
    private static final String NO_SELECTION = "-";

    private static final String SELECTED_TAB = "SelectedTab";
    private static final String SELECTED_CHANNEL = "SelectedChannel";
    private static final String SOCKET_HOSTS = "SocketChannels";
    private static final String DEVELOPER_MODE = "DeveloperMode";
    private static final String LIVE_MODE = "LiveMode";

    private static final Logger logger = Logger.getLogger(Monitor.class.getName());
    
}
