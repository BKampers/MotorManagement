package randd.motormanagement.swing;


import bka.communication.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ControlPanel extends javax.swing.JPanel {

    /**
     * Creates new form ControlPanel
     */
    public ControlPanel(String host) {
        this.host = host;
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pulseNanosLabel = new javax.swing.JLabel();
        pulseNanosSpinner = new javax.swing.JSpinner();

        pulseNanosLabel.setText("Pulse Nanos");

        pulseNanosSpinner.setModel(new javax.swing.SpinnerNumberModel(1000000, 500, 30000000, 50000));
        pulseNanosSpinner.setEditor(new javax.swing.JSpinner.NumberEditor(pulseNanosSpinner, "#"));
        pulseNanosSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                pulseNanosSpinner_stateChanged(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(pulseNanosLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pulseNanosSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(190, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pulseNanosLabel)
                    .addComponent(pulseNanosSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(266, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    
    private void pulseNanosSpinner_stateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_pulseNanosSpinner_stateChanged
        openConnection();
        String message = "{\"PulseNanos\":" + pulseNanosSpinner.getValue() + "}";
        channel.send(message.getBytes());
    }//GEN-LAST:event_pulseNanosSpinner_stateChanged

    
    private void openConnection() {
        if (channel == null) {
            try {
                channel = SocketChannel.create(host, Monitor.RANDD_CONTROL_PORT);
                channel.open(ControlPanel.class.getName());
            }
            catch (ChannelException ex) {
                Logger.getLogger(ControlPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }


    private final String host;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel pulseNanosLabel;
    private javax.swing.JSpinner pulseNanosSpinner;
    // End of variables declaration//GEN-END:variables
    
    private Channel channel;
    
}
