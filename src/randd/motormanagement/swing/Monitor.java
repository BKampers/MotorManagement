/*
** Copyright © Bart Kampers
**
** For distribution make sure that: 
** - win32com.dll is in dist directory
** - javax.comm.properties is in dist\lib directory
*/

package randd.motormanagement.swing;

import bka.communication.*;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import randd.motormanagement.communication.*;
import randd.motormanagement.system.*;


public class Monitor extends bka.swing.FrameApplication {

    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
        catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Monitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Monitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Monitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Monitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Monitor monitor = new Monitor();
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
        detectComPorts();
        String channelName = getProperty(SELECTED_CHANNEL);
        if (channelName != null) {
            int i = 0;
            while (selectedChannel == null && i < comPortComboBox.getItemCount()) {
                Object comboBoxItem = comPortComboBox.getItemAt(i);
                if (comboBoxItem.toString().equals(channelName)) {
                    comPortComboBox.setSelectedItem(comboBoxItem);
                }
                i++;
            }
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
//        ignitionTimerPanel = new TimerPanel(this, "Ignition timer");
//        settingsPanel.add(ignitionTimerPanel);
        valuesPanel.add(new MeasurementPanel(Measurement.getInstance("RPM"), measurementPanelLsitener));
        valuesPanel.add(new MeasurementPanel(Measurement.getInstance("Load"), measurementPanelLsitener));
        valuesPanel.add(new MeasurementPanel(Measurement.getInstance("Water"), measurementPanelLsitener));
        valuesPanel.add(new MeasurementPanel(Measurement.getInstance("Air"), measurementPanelLsitener));
        valuesPanel.add(new MeasurementPanel(Measurement.getInstance("Battery"), measurementPanelLsitener));
        valuesPanel.add(new MeasurementPanel(Measurement.getInstance("Map"), measurementPanelLsitener));
//        valuesPanel.add(new MeasurementPanel(Measurement.get("Lambda")));
//        valuesPanel.add(new MeasurementPanel(Measurement.get("Aux1")));
//        valuesPanel.add(new MeasurementPanel(Measurement.get("Aux2")));
        addTable("Ignition");
        addTable("Injection");
        addMemoryPanel();
        addStatusPanel();
    }
    

    RemoteSystem getMessenger() {
        return remoteSystem;
    } 
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        toolsPanel = new javax.swing.JPanel();
        comPortComboBox = new javax.swing.JComboBox();
        tabsPanel = new javax.swing.JTabbedPane();
        valuesPanel = new javax.swing.JPanel();
        settingsPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(title());
        setMinimumSize(new java.awt.Dimension(1000, 500));

        toolsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        comPortComboBox.setMinimumSize(new java.awt.Dimension(75, 20));
        comPortComboBox.setPreferredSize(new java.awt.Dimension(75, 20));
        comPortComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comPortComboBox_actionPerformed(evt);
            }
        });
        toolsPanel.add(comPortComboBox);

        getContentPane().add(toolsPanel, java.awt.BorderLayout.NORTH);
        getContentPane().add(tabsPanel, java.awt.BorderLayout.CENTER);

        valuesPanel.setLayout(new javax.swing.BoxLayout(valuesPanel, javax.swing.BoxLayout.Y_AXIS));
        getContentPane().add(valuesPanel, java.awt.BorderLayout.EAST);
        getContentPane().add(settingsPanel, java.awt.BorderLayout.WEST);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    private void comPortComboBox_actionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comPortComboBox_actionPerformed
        Object selectedItem = comPortComboBox.getSelectedItem();
        if (selectedItem == NO_SELECTION) {
            disconnect();
        }
        else if (selectedItem != selectedChannel) {
            connect((SerialPortChannel) selectedItem);
        }
    }//GEN-LAST:event_comPortComboBox_actionPerformed


    private void detectComPorts() {
        comPortComboBox.removeAllItems();
        comPortComboBox.addItem(NO_SELECTION);
        for (SerialPortChannel channel : SerialPortChannel.findAll()) {
            comPortComboBox.addItem(channel);
        }
    }
    
    
    private void disconnect() {
        if (remoteSystem != null) {
            remoteSystem.disconnect();
            remoteSystem = null;
            selectedChannel = null;
            setProperty(SELECTED_CHANNEL, null);
        }
    }

    
    private void connect(SerialPortChannel selectedItem) {
        selectedChannel = selectedItem;
        try {
            SerialPort port = new SerialPort(selectedItem, title());
            remoteSystem = new RemoteSystem(port);
            remoteSystem.connect();
            setProperty(SELECTED_CHANNEL, port.getName());
            memoryPanel.setMemory(flash);
            activateSelectedTab();
            statusPanel.setRemoteSystem(remoteSystem);
            remoteSystem.startPolling();
            
        }
        catch (ChannelException ex) {
            handle(ex);
        }
    }
    
    
    private void addTable(String name) {
        Table table = Table.getInstance(name);
        tabsPanel.add(new TablePanel(new TablePanelListener(), table));
        tabsPanel.setTitleAt(tabsPanel.getTabCount() - 1, name);
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
        public void simulationEnabled(Measurement measurement, boolean enabled) {
            try {
                if (! enabled) {
                    remoteSystem.enableMeasurementSimulation(measurement, enabled);
                }
            }
            catch (org.json.JSONException | InterruptedException ex) {
                handle(ex);
            }
        }

        @Override
        public void simulationValueModified(Measurement measurement, double value) {
            try {
                remoteSystem.setMeasurementSimulationValue(measurement, value);
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
    private SerialPortChannel selectedChannel = null;
    
    private final Flash flash = new Flash();

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox comPortComboBox;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JTabbedPane tabsPanel;
    private javax.swing.JPanel toolsPanel;
    private javax.swing.JPanel valuesPanel;
    // End of variables declaration//GEN-END:variables

    
    java.awt.Component selectedTab = null;
    
    private final MeasurementPanelListener measurementPanelLsitener = new MeasurementPanelListener();

    private static final String NO_SELECTION = "-";

    private static final String SELECTED_CHANNEL = "SelectedChannel";

}
