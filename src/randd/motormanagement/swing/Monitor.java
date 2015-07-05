/*
** Copyright © Bart Kampers
**
** For distribution make sure that: 
** - win32com.dll is in dist directory
** - javax.comm.properties is in dist\lib directory
*/

package randd.motormanagement.swing;

import bka.communication.*;
import java.util.*;
import java.util.logging.*;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import org.json.*;
import randd.motormanagement.communication.*;
import randd.motormanagement.system.*;


public class Monitor extends bka.swing.FrameApplication {

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
    

    private void setupLogging() {
        try {
            String logFilters = getProperty("logFilters");
            String[] logPaths = (logFilters != null) ? logFilters.split(";") : null;
            randd.motormanagement.logging.Manager.setup(logPaths);
        }
        catch (java.io.IOException ex) {
            handle(ex);
        }
    }

    
    private void initializePanels() {
        boolean developerMode = getBooleanProperty(DEVELOPER_MODE, false);
        if (getBooleanProperty(LIVE_MODE, true)) {
    //        ignitionTimerPanel = new TimerPanel(this, "Ignition timer");
    //        settingsPanel.add(ignitionTimerPanel);
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
            addEnginePanel();
        }
        if (developerMode) {
            addMemoryPanel();
            addStatusPanel();
        }
    }
    

    private void handle(Throwable throwable) {
        throwable.printStackTrace(System.err);
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
            memoryPanel.setMemory(flash);
            activateSelectedTab();
            statusPanel.setRemoteSystem(remoteSystem);
            if (getBooleanProperty(LIVE_MODE, true)) {
                populateTableTabs();
                remoteSystem.startPolling();
            }
        }
        catch (ChannelException ex) {
            handle(ex);
        }
    }
    
    
    private void addMeasurementPanel(String measurementName, boolean developerMode) {
        Measurement measurement = Measurement.getInstance(measurementName);
        Table correctionTable = getCorrectionTable(measurement);
        MeasurementPanel panel = new MeasurementPanel(measurement, correctionTable, measurementPanelLsitener, developerMode);
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
            return Table.getInstance(measurementName + "Correction");
        }
        else {
            return null;
        }
    }
    

    private void populateTableTabs() {
        try {
            Collection<String> tableNames = remoteSystem.requestTableNames();
            for (String name : tableNames) {
                addTable(name);
            }
        }
        catch (InterruptedException | org.json.JSONException ex) {
            handle(ex);
        }
    }
    
    
    private void addTable(String name) {
        Table table = Table.getInstance(name);
        tabsPanel.add(new TablePanel(new TablePanelListener(), table));
        tabsPanel.setTitleAt(tabsPanel.getTabCount() - 1, Bundle.getInstance().get(name));
    }
    
    
    private void addEnginePanel() {
        EnginePanel panel = new EnginePanel(engine);
        panel.setListener(new EnginePanelListener());
        tabsPanel.add(panel);
        tabsPanel.setTitleAt(tabsPanel.getTabCount() - 1, Bundle.getInstance().get("Engine"));
    }
    
    
    private void addMemoryPanel() {
        tabsPanel.add(memoryPanel);
        tabsPanel.setTitleAt(tabsPanel.getTabCount() - 1, "Flash");
    }
    
    
    private void addStatusPanel() {
        tabsPanel.add(statusPanel);
        tabsPanel.setTitleAt(tabsPanel.getTabCount() - 1, Bundle.getInstance().get("Status"));
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
                    remoteSystem.requestEngine(engine);
                }
                else if (selectedTab instanceof MemoryPanel) {
                    remoteSystem.requestFlash(flash);
                }
            }
            catch (InterruptedException | org.json.JSONException ex) {
                handle(ex);
            }
        }
    }
    

    private class TabChangeListener implements javax.swing.event.ChangeListener {

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
                String result = remoteSystem.enableTable(table, enabled);
                panel.notifyResult(Measurement.Property.TABLE_ENABLED, result);
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
                    String result = remoteSystem.enableMeasurementSimulation(measurement, enabled);
                    panel.notifyResult(Measurement.Property.SIMULATION_ENABLED, result);
                }
            }
            catch (org.json.JSONException | InterruptedException ex) {
                handle(ex);
            }
        }

        @Override
        public void simulationValueModified(MeasurementPanel panel, double value) {
            try {
                String result = remoteSystem.setMeasurementSimulationValue(panel.getMeasurement(), value);
                panel.notifyResult(Measurement.Property.SIMULATION_VALUE, result);
            }
            catch (org.json.JSONException | InterruptedException ex) {
                handle(ex);
            }
        }
        
    }
    
    
    private class TablePanelListener implements TablePanel.Listener {
        
        @Override
        public void startIndexPoll(Table table) {
            if (remoteSystem != null) {
                remoteSystem.startIndexPoll(table);
            }
        }
        
        @Override
        public void setValue(Table table, int column, int row, float value) {
            try {
                String result = remoteSystem.modifyTable(table, column, row, value);
                if (RemoteSystem.OK.equals(result)) {
                    table.setField(column, row, value);
                }
            }
            catch (org.json.JSONException | InterruptedException ex) {
                handle(ex);
            }
        }
        
    }
    
    
    private class EnginePanelListener implements EnginePanel.Listener {

        @Override
        public void cylinderCountModified(int count) {
            if (engine.getCylinderCount() != count) {
                try {
                    String status = remoteSystem.modifyCylinderCount(count);
                    if (RemoteSystem.OK.equals(status)) {
                        remoteSystem.requestEngine(engine);
                    }
                }
                catch (org.json.JSONException | InterruptedException ex) {
                    handle(ex);
                }
            }
        }

        @Override
        public void totalCogsModified(int cogTotal) {
            if (engine.getCogwheel().getCogTotal() != cogTotal) {
                modifyCogwheel(cogTotal, engine.getCogwheel().getGapSize(), engine.getCogwheel().getOffset());
            }
        }

        @Override
        public void gapSizeModified(int gapSize) {
            if (engine.getCogwheel().getGapSize() != gapSize) {
                modifyCogwheel(engine.getCogwheel().getCogTotal(), gapSize, engine.getCogwheel().getOffset());
            }
        }

        @Override
        public void offsetModified(int offset) {
            if (engine.getCogwheel().getOffset() != offset) {
                modifyCogwheel(engine.getCogwheel().getCogTotal(), engine.getCogwheel().getGapSize(), offset);
            }
        }
        
        private void modifyCogwheel(int cogTotal, int gapSize, int offset) {
            try {
                String status = remoteSystem.modifyCogwheel(cogTotal, gapSize, offset);
                if (RemoteSystem.OK.equals(status)) {
                    remoteSystem.requestEngine(engine);
                }
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
                String status = remoteSystem.modifyFlash(0, flash.getSize(), 0xAA);
                if (RemoteSystem.OK.equals(status)) {
                    remoteSystem.requestFlash(flash);
                }
            }
            catch (org.json.JSONException | InterruptedException ex) {
                handle(ex);
            }
            
        }
        
        @Override
        public void loadButtonPressed() {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(Monitor.this) == JFileChooser.APPROVE_OPTION) {
                java.io.FileReader reader = null;
                try {
                    final int MAX = 0x10;
                    java.io.File file = fileChooser.getSelectedFile();
                    reader = new java.io.FileReader(file);
                    char[] chars = new char[(int) file.length()];
                    reader.read(chars);
                    String source = new String(chars);
                    JSONObject jsonObject = new JSONObject(source);
                    int reference = jsonObject.getInt("Reference");
                    JSONArray jsonValues = jsonObject.getJSONArray("Value");
                    int sourceIndex = 0;
                    int count = jsonValues.length();
                    while (sourceIndex < count) {
                        int[] values = new int[Math.min(count - sourceIndex, MAX)];
                        for (int i = 0; i < values.length; ++i) {
                            values[i] = jsonValues.getInt(sourceIndex);
                            sourceIndex++;
                        }
                        remoteSystem.modifyFlash(reference, values);
                        reference += values.length;
                    }
                }
                catch (java.io.IOException | JSONException | InterruptedException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                finally {
                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    }
                    catch (java.io.IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        
    }) ;
    
    
    private final StatusPanel statusPanel = new StatusPanel();

    
    private RemoteSystem remoteSystem = null;
    private Channel selectedChannel = null;
    
    private final Engine engine = new Engine();
    private final Flash flash = new Flash();
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox channelComboBox;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JTabbedPane tabsPanel;
    private javax.swing.JPanel toolsPanel;
    private javax.swing.JPanel valuesPanel;
    // End of variables declaration//GEN-END:variables

    
    private java.awt.Component selectedTab = null;
    
    private final MeasurementPanelListener measurementPanelLsitener = new MeasurementPanelListener();
    
    private static final String NO_SELECTION = "-";

    private static final String SELECTED_CHANNEL = "SelectedChannel";
    private static final String SOCKET_HOSTS = "SocketChannels";
    private static final String DEVELOPER_MODE = "DeveloperMode";
    private static final String LIVE_MODE = "LiveMode";

    private static final int RANDD_MM_PORT = 44252;
    
    private static final Logger logger = Logger.getLogger(Monitor.class.getName());
    
}
