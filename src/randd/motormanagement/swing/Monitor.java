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
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
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
//        ignitionTimerPanel = new TimerPanel(this, "Ignition timer");
//        settingsPanel.add(ignitionTimerPanel);
        boolean developerMode = getBooleanProperty(DEVELOPER_MODE, false);
        channelComboBox.setEditable(developerMode);
        valuesPanel.add(new MeasurementPanel(Measurement.getInstance("RPM"), measurementPanelLsitener, developerMode));
        valuesPanel.add(new MeasurementPanel(Measurement.getInstance("Load"), measurementPanelLsitener, developerMode));
        valuesPanel.add(new MeasurementPanel(Measurement.getInstance("Water"), measurementPanelLsitener, developerMode));
        valuesPanel.add(new MeasurementPanel(Measurement.getInstance("Air"), measurementPanelLsitener, developerMode));
        valuesPanel.add(new MeasurementPanel(Measurement.getInstance("Battery"), measurementPanelLsitener, developerMode));
        valuesPanel.add(new MeasurementPanel(Measurement.getInstance("Map"), measurementPanelLsitener, developerMode));
//        valuesPanel.add(new MeasurementPanel(Measurement.get("Lambda")));
//        valuesPanel.add(new MeasurementPanel(Measurement.get("Aux1")));
//        valuesPanel.add(new MeasurementPanel(Measurement.get("Aux2")));
        addCogWheelPanel();
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
            JsonChannel channel = new JsonChannel(selectedItem, title());
            remoteSystem = new RemoteSystem(channel);
            remoteSystem.connect();
            setProperty(SELECTED_CHANNEL, channel.getName());
            memoryPanel.setMemory(flash);
            activateSelectedTab();
            statusPanel.setRemoteSystem(remoteSystem);
            populateTableTabs();
            remoteSystem.startPolling();
        }
        catch (ChannelException ex) {
            handle(ex);
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
        tabsPanel.setTitleAt(tabsPanel.getTabCount() - 1, name);
    }
    
    
    private void addCogWheelPanel() {
        tabsPanel.add(new CogWheelPanel());
        tabsPanel.setTitleAt(tabsPanel.getTabCount() - 1, "CogWheel");
    }
    
    
    private void addMemoryPanel() {
        tabsPanel.add(memoryPanel);
        tabsPanel.setTitleAt(tabsPanel.getTabCount() - 1, "Flash");
    }
    
    
    private void addStatusPanel() {
        tabsPanel.add(statusPanel);
        tabsPanel.setTitleAt(tabsPanel.getTabCount() - 1, "Status");
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
                if (selectedTab instanceof MemoryPanel) {
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
        
    }) ;
    
    
    private final StatusPanel statusPanel = new StatusPanel();

    
    private RemoteSystem remoteSystem = null;
    private Channel selectedChannel = null;
    
    private final Flash flash = new Flash();

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox channelComboBox;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JTabbedPane tabsPanel;
    private javax.swing.JPanel toolsPanel;
    private javax.swing.JPanel valuesPanel;
    // End of variables declaration//GEN-END:variables

    
    java.awt.Component selectedTab = null;
    
    private final MeasurementPanelListener measurementPanelLsitener = new MeasurementPanelListener();
    
    private static final String NO_SELECTION = "-";

    private static final String SELECTED_CHANNEL = "SelectedChannel";
    private static final String SOCKET_HOSTS = "SocketChannels";
    private static final String DEVELOPER_MODE = "DeveloperMode";

    private static final int RANDD_MM_PORT = 44252;
    
}
