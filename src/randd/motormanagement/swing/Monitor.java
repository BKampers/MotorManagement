/*
** Copyright © Bart Kampers
**
** For distribution make sure that: 
** - win32com.dll is in dist directory
** - javax.comm.properties is in dist\lib directory
*/

package randd.motormanagement.swing;

import bka.communication.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.event.*;
import org.json.*;
import randd.motormanagement.communication.*;
import randd.motormanagement.system.*;


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
        channelComboBox.setEditable(getBooleanProperty(DEVELOPER_MODE, false));
        populateChannelComboBox();
        selectStoredChannel();
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

    
    private void handle(Throwable throwable) {
        JOptionPane.showMessageDialog(this, throwable.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    
    private String title() {
        return manufacturerName() + " " + applicationName();
    }
    
    
    private Monitor() {
        initComponents();
    }
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

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

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    @SuppressWarnings("unchecked")
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


    @SuppressWarnings("unchecked")
    private void populateChannelComboBox() {
        channelComboBox.removeAllItems();
        channelComboBox.addItem(NO_SELECTION);
        try {
            for (SerialPortChannel channel : SerialPortChannel.findAll()) {
                channelComboBox.addItem(channel);
            }
        }
        catch (ChannelException ex) {
            LOGGER.log(Level.INFO, "Serial communication not supported.");
            LOGGER.log(Level.FINEST, "", ex);
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
                tabsPanel.removeChangeListener(tabChangeListener);
                tabsPanel.removeAll();
                valuesPanel.removeAll();
                remoteSystem.disconnect();
                remoteSystem = null;
                selectedChannel = null;
                setProperty(SELECTED_CHANNEL, null);
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
            initializePanels();
            memoryPanel.setMemory(remoteSystem.getFlash());
            //activateSelectedTab();
            statusPanel.setRemoteSystem(remoteSystem);
            remoteSystem.addListener(new RemoteSystemListener());
            remoteSystem.requestTableNames();
            if (getBooleanProperty(LIVE_MODE, true)) {
                int pollInterval = getIntProperty(POLL_INTERVAL, DEFAULT_POLL_INTERVAL);
                remoteSystem.startPolling(pollInterval);
            }
        }
        catch (ChannelException | InterruptedException | JSONException ex) {
            handle(ex);
        }
    }
    
    
    private void initializePanels() {
        boolean developerMode = getBooleanProperty(DEVELOPER_MODE, false);
        addMeasurementPanel("RPM", developerMode);
        addMeasurementPanel("Load", developerMode);
        addMeasurementPanel("Water", developerMode);
        addMeasurementPanel("Air", developerMode);
        addMeasurementPanel("Battery", developerMode);
        addMeasurementPanel("Map", developerMode);
        addMeasurementPanel("Lambda", developerMode);
        addMeasurementPanel("Aux1", developerMode);
        addMeasurementPanel("Aux2", developerMode);
        addEngineTabPanel();
        if (developerMode) {
            addTabPanel(memoryPanel, "Flash");
            addTabPanel(statusPanel, "Status");
            addTabPanel(new ControlPanel(), "Control");
        }
    }
    

    private void addMeasurementPanel(String measurementName, boolean developerMode) {
        Measurement measurement = Measurement.getInstance(measurementName);
        Table correctionTable = remoteSystem.getCorrectionTable(measurement);
        MeasurementPanel panel = new MeasurementPanel(measurement, correctionTable, measurementPanelListener, developerMode);
        valuesPanel.add(panel);
        Table table = remoteSystem.getCorrectionTable(measurement);
        if (table != null) {
            try {
                remoteSystem.requestTableProperties(table);
            }
            catch (InterruptedException | org.json.JSONException ex) {
                handle(ex);
            }
        }
    }
    
    
    private void addTableTabPanel(String name) {
        Table table = Table.getInstance(name);
        table.addListener(tableListener);
        addTabPanel(new TablePanel(new TablePanelListener(), table), name);
    }
    
    
    private void addEngineTabPanel() {
        EnginePanel panel = new EnginePanel(remoteSystem.getEngine());
        panel.setListener(new EnginePanelListener());
        addTabPanel(panel, "Engine");
    }
    
    
    private synchronized void addTabPanel(JPanel panel, String titleKey) {
        LOGGER.log(Level.FINE, "Add panel {0}", titleKey);
        int index = panelIndex(panel);
        tabsPanel.add(panel, index);
        String title = Bundle.getInstance().get(titleKey);
        tabsPanel.setTitleAt(index, title);
        panel.setName(titleKey);
    }


    private int panelIndex(JPanel panel) {
        if (panel instanceof EnginePanel) {
            return 0;
        }
        else if (panel instanceof TablePanel) {
            return tablePanelIndex((TablePanel) panel);
        }
        else {
            return tabsPanel.getTabCount();
        }
    }


    private int tablePanelIndex(TablePanel panel) {
        int order = order(panel);
        for (int i = 1; i < tabsPanel.getTabCount(); ++i) {
            java.awt.Component component = tabsPanel.getComponentAt(i);
            if (component instanceof TablePanel) {
                if (order < order((TablePanel) component))  {
                    return i;
                }
            }
            else {
                return i;
            }
        }
        return tabsPanel.getTabCount();
    }


    private int order(TablePanel panel) {
        String tableName = panel.getTable().getName();
        Resource resource = new Resource("randd/motormanagement/Tables.json");
        try {
            JSONObject tablesObject = new JSONObject(resource.loadText());
            JSONArray orderArray = tablesObject.getJSONArray("order");
            for (int i = 0; i < orderArray.length(); ++i) {
                if (orderArray.getString(i).equals(tableName)) {
                    return i;
                }
            }
        }
        catch (IOException | JSONException ex) {
            Logger.getLogger(Monitor.class.getName()).log(Level.WARNING, "table order", ex);
        }
        return Integer.MAX_VALUE;
    }
    
    
    private void selectTab(String titleKey) {
        java.awt.Component tabPanel = findTabPanel(titleKey);
        if (tabPanel != null) {
            tabsPanel.setSelectedComponent(tabPanel);
        }
    }
    
    
    private java.awt.Component findTabPanel(String name) {
        if (name != null) {
            for (int i = 0; i < tabsPanel.getTabCount(); ++i) {
                java.awt.Component tabPanel = tabsPanel.getComponentAt(i);
                if (tabPanel != null && name.equals(tabPanel.getName())) {
                    return tabPanel;
                }
            }
        }
        return null;
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
                        remoteSystem.requestTableFields(table);
                    }
                    Boolean enabled = table.isEnabled();
                    if (enabled == null) {
                        remoteSystem.requestTableProperties(table);
                    }
                    else if (enabled) {
                        remoteSystem.startIndexPoll(table);
                    }
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
                LOGGER.log(Level.WARNING, "loadTextFile", ex);
            }
        }
        return source;
    }
    

    private class TabChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent evt) {
            activateSelectedTab();
            setProperty(SELECTED_TAB, selectedTab.getName());
        }

    }
    
    
    private class MeasurementPanelListener implements MeasurementPanel.Listener {
        
        @Override
        public void tableEnabled(MeasurementPanel panel, boolean enabled) {
            Measurement measurement = panel.getMeasurement();
            Table table = remoteSystem.getCorrectionTable(measurement);
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
                    if (enabled) { 
                        remoteSystem.enableMeasurementSimulation(measurement, measurement.getValue());
                    }
                    else {
                        remoteSystem.disableMeasurementSimulation(measurement);
                    }
                }
            }
            catch (org.json.JSONException | InterruptedException ex) {
                handle(ex);
            }
        }

        @Override
        public void simulationValueModified(MeasurementPanel panel, double value) {
            Measurement measurement = panel.getMeasurement();
            try {
                remoteSystem.enableMeasurementSimulation(measurement, (float) value);
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
                int reference = jsonObject.getInt("Reference");
                int[] values = getIntArray(jsonObject.getJSONArray("Value"));
                remoteSystem.modifyFlash(reference, values);
            }
            catch (JSONException | InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
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
            tabsPanel.setSelectedIndex(-1); // Deselect tab panel to ensure tabChangeListener to fire
            tabsPanel.addChangeListener(tabChangeListener);
            selectTab(Monitor.this.getProperty(SELECTED_TAB));
        }
        
    }
    
    
    private class TableListener implements Table.Listener {

        @Override
        public void propertyChanged(Table table, Table.Property property, Object... attributes) {
            if (property == Table.Property.ENABLED) {
                java.awt.Component component;
                component = tabsPanel.getSelectedComponent();
                if (component instanceof TablePanel && ((TablePanel) component).getTable() == table) {
                    if (table.isEnabled()) {
                        remoteSystem.startIndexPoll(table);
                    }
                    else {
                        remoteSystem.stopIndexPoll(table);
                    }
                }
            }
        }
        
    }


    private final StatusPanel statusPanel = new StatusPanel();
    
    private RemoteSystem remoteSystem = null;
    private Channel selectedChannel = null;
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private final javax.swing.JComboBox channelComboBox = new javax.swing.JComboBox();
    private final javax.swing.JTabbedPane tabsPanel = new javax.swing.JTabbedPane();
    private final javax.swing.JPanel toolsPanel = new javax.swing.JPanel();
    private final javax.swing.JPanel valuesPanel = new javax.swing.JPanel();
    // End of variables declaration//GEN-END:variables

    
    private java.awt.Component selectedTab = null;
    
    private final ChangeListener tabChangeListener = new TabChangeListener();
    private final MeasurementPanelListener measurementPanelListener = new MeasurementPanelListener();
    private final TableListener tableListener = new TableListener();
    
    private static final String NO_SELECTION = "-";

    private static final String SELECTED_TAB = "SelectedTab";
    private static final String SELECTED_CHANNEL = "SelectedChannel";
    private static final String SOCKET_HOSTS = "SocketChannels";
    private static final String POLL_INTERVAL = "PollInterval";
    private static final String DEVELOPER_MODE = "DeveloperMode";
    private static final String LIVE_MODE = "LiveMode";

    private static final int DEFAULT_POLL_INTERVAL = 50;


    private static final Logger LOGGER = Logger.getLogger(Monitor.class.getName());
    
}
