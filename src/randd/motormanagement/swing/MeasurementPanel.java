/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.swing;


import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import randd.motormanagement.system.*;


public class MeasurementPanel extends javax.swing.JPanel {

    
    public interface Listener {
        public void tableEnabled(MeasurementPanel panel, boolean enabled);
        public void simulationEnabled(MeasurementPanel panel, boolean enabled);
        public void simulationValueModified(MeasurementPanel panel, double value);
    }
    
    
    public MeasurementPanel(Measurement measurement, Listener listener, boolean simulationEnabled) {
        logger = Logger.getLogger(MeasurementPanel.class.getName() + "." + measurement.getName().replace('.', '-'));
        this.listener = listener;
        initComponents();
        boolean isCorrection = ! "Load".equals(measurement.getName()) && ! "RPM".equals(measurement.getName());
        enableToggleButton.setVisible(isCorrection);
        enableToggleButton.setEnabled(isCorrection);
        simulationToggleButton.setVisible(simulationEnabled);
        setMeasurement(measurement);
    }
    
    
    Measurement getMeasurement() {
        return measurement;
    }
    
    
    void notifyResult(Measurement.Property property, String result) {
        logger.log(Level.INFO, "notifyResult {0} {1}", new Object[] { property, result });
        switch (property) {
            case TABLE_ENABLED:
                enableToggleButton.setEnabled(true);
                showEnabled(enableToggleButton.isSelected());
                break;
            case SIMULATION_ENABLED:
                simulationToggleButton.setEnabled(true);
                enableSimulation(simulationToggleButton.isSelected());
                break;
        }
    }
    
    
    private void setMeasurement(Measurement measurement) {
        this.measurement = measurement;
        measurement.addListener(measurementListener);
        if (measurement.getName() != null) {
            nameLabel.setText(Bundle.getInstance().get(measurement.getName()));
        }
        updateValueText();
    }
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        nameLabel = new javax.swing.JLabel();
        valueTextField = new javax.swing.JTextField();
        unitLabel = new javax.swing.JLabel();
        enableToggleButton = new javax.swing.JToggleButton();
        simulationToggleButton = new javax.swing.JToggleButton();

        setPreferredSize(new java.awt.Dimension(250, 30));

        nameLabel.setPreferredSize(new java.awt.Dimension(60, 25));
        add(nameLabel);

        valueTextField.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        valueTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        valueTextField.setEnabled(false);
        valueTextField.setMinimumSize(new java.awt.Dimension(60, 25));
        valueTextField.setPreferredSize(new java.awt.Dimension(60, 25));
        valueTextField.setDisabledTextColor(LIME_GREEN);
        valueTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                valueTextField_actionPerformed(evt);
            }
        });
        add(valueTextField);

        unitLabel.setPreferredSize(new java.awt.Dimension(50, 25));
        add(unitLabel);

        enableToggleButton.setText("⬤");
        enableToggleButton.setEnabled(false);
        enableToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableToggleButton_actionPerformed(evt);
            }
        });
        add(enableToggleButton);

        simulationToggleButton.setText("Sim");
        simulationToggleButton.setPreferredSize(new java.awt.Dimension(50, 25));
        simulationToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                simulationToggleButton_actionPerformed(evt);
            }
        });
        add(simulationToggleButton);
    }// </editor-fold>//GEN-END:initComponents

    
    private void simulationToggleButton_actionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_simulationToggleButton_actionPerformed
        simulationToggleButton.setEnabled(false);
        listener.simulationEnabled(this, simulationToggleButton.isSelected());
    }//GEN-LAST:event_simulationToggleButton_actionPerformed

    
    private void valueTextField_actionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_valueTextField_actionPerformed
        try {
            java.text.NumberFormat numberFormat = java.text.NumberFormat.getNumberInstance();
            double value = numberFormat.parse(valueTextField.getText()).doubleValue();
            listener.simulationValueModified(this, value);
            valueTextField.setBackground(Color.WHITE);
        }
        catch (java.text.ParseException ex) {
            valueTextField.setBackground(Color.RED);
        }
    }//GEN-LAST:event_valueTextField_actionPerformed

    
    private void enableToggleButton_actionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableToggleButton_actionPerformed
        enableToggleButton.setEnabled(false);
        listener.tableEnabled(this, enableToggleButton.isSelected());
    }//GEN-LAST:event_enableToggleButton_actionPerformed

    
    private void showEnabled(boolean selected) {
        enableToggleButton.setForeground(selected ? Color.GREEN : Color.DARK_GRAY);
    }
    
    
    private void enableSimulation(boolean enabled) {
        logger.log(Level.INFO, "enableSimulation {0}", enabled);
        simulationToggleButton.setSelected(enabled);
        valueTextField.setEnabled(enabled);      
    }


    private void updateValueText() {
        Float value = measurement.getValue();
        if (value != null) {
            try {
                java.util.Formatter formatter = new java.util.Formatter();
                formatter.format(measurement.getFormat(), value);
                valueTextField.setText(formatter.toString());
            }
            catch (Exception ex) {
                valueTextField.setText(Long.toString(value.longValue()));
            }
        }
        else {
            valueTextField.setText("");
        }
    }


    private class MeasurementListener implements Measurement.Listener {

        public void valueUpdated() {
            logger.log(Level.INFO, "valueUpdated {0}", measurement.getValue());
            if (! simulationToggleButton.isSelected()) {
                updateValueText();
            }
        }

        public void simulationUpdated() {
            logger.log(Level.INFO, "simulationUpdated {0}", measurement.isSimulationEnabled());
            if (simulationToggleButton.isEnabled()) {
                enableSimulation(measurement.isSimulationEnabled());
            }
        }
    
    }    

    
    private final Listener listener;
    private Measurement measurement;

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton enableToggleButton;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JToggleButton simulationToggleButton;
    private javax.swing.JLabel unitLabel;
    private javax.swing.JTextField valueTextField;
    // End of variables declaration//GEN-END:variables


    private final MeasurementListener measurementListener = new MeasurementListener();
    
    private final Logger logger;
    
    private static final Color LIME_GREEN = new java.awt.Color(50, 205, 50);
    
}
