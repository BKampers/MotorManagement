/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.swing;

import javax.swing.*;


public class CogWheelPanel extends javax.swing.JPanel {


    public CogWheelPanel() {
        initComponents();
        java.util.List<Integer> deadPoints = new java.util.ArrayList<>();
        deadPoints.add(20);
        deadPoints.add(50);
        cogWheelRenderer.setDeadPoints(deadPoints);
        totalCogSpinner.setModel(totalCogSpinnerModel);
        gapLengthSpinner.setModel(gapLengthSpinnerModel);
        offsetSpinner.setModel(offsetSpinnerModel);
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        controlPanel = new javax.swing.JPanel();
        typeLabel = new javax.swing.JLabel();
        totalCogSpinner = new javax.swing.JSpinner();
        minusLabel = new javax.swing.JLabel();
        gapLengthSpinner = new javax.swing.JSpinner();
        offsetLabel = new javax.swing.JLabel();
        offsetSpinner = new javax.swing.JSpinner();
        cylinderComboBox = new javax.swing.JComboBox();

        scrollPane.setViewportView(cogWheelRenderer);

        controlPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        typeLabel.setText("CogWheelType");

        totalCogSpinner.setMinimumSize(new java.awt.Dimension(40, 28));
        totalCogSpinner.setPreferredSize(new java.awt.Dimension(40, 28));
        totalCogSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                totalCogSpinner_stateChanged(evt);
            }
        });

        minusLabel.setText("-");

        gapLengthSpinner.setMinimumSize(new java.awt.Dimension(40, 28));
        gapLengthSpinner.setPreferredSize(new java.awt.Dimension(40, 28));
        gapLengthSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                gapLengthSpinner_stateChanged(evt);
            }
        });

        javax.swing.GroupLayout controlPanelLayout = new javax.swing.GroupLayout(controlPanel);
        controlPanel.setLayout(controlPanelLayout);
        controlPanelLayout.setHorizontalGroup(
            controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(controlPanelLayout.createSequentialGroup()
                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(controlPanelLayout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addComponent(typeLabel))
                    .addGroup(controlPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(totalCogSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(minusLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(gapLengthSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(18, Short.MAX_VALUE))
        );
        controlPanelLayout.setVerticalGroup(
            controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(controlPanelLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(typeLabel)
                .addGap(12, 12, 12)
                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(totalCogSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(minusLabel)
                    .addComponent(gapLengthSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(12, Short.MAX_VALUE))
        );

        offsetLabel.setText("Offset");

        offsetSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                offsetSpinner_stateChanged(evt);
            }
        });

        cylinderComboBox.setModel(new CylinderComboBoxModel());
        cylinderComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cylinderComboBox_actionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(controlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addComponent(offsetLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(offsetSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(cylinderComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 64, Short.MAX_VALUE)
                .addComponent(scrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 277, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(controlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(offsetSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(offsetLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cylinderComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    
    private void totalCogSpinner_stateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_totalCogSpinner_stateChanged
        cogWheelRenderer.setCogCount((Integer) totalCogSpinner.getValue() - (Integer) gapLengthSpinner.getValue());
        cogWheelRenderer.repaint();
    }//GEN-LAST:event_totalCogSpinner_stateChanged

    
    private void gapLengthSpinner_stateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_gapLengthSpinner_stateChanged
        cogWheelRenderer.setGapLength((Integer) gapLengthSpinner.getValue());
        cogWheelRenderer.repaint();
    }//GEN-LAST:event_gapLengthSpinner_stateChanged

    
    private void offsetSpinner_stateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_offsetSpinner_stateChanged
        setDeadPoints();
    }//GEN-LAST:event_offsetSpinner_stateChanged

    
    private void cylinderComboBox_actionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cylinderComboBox_actionPerformed
        setDeadPoints();
    }//GEN-LAST:event_cylinderComboBox_actionPerformed

    
    private void setDeadPoints() {
        CylinderCount cylinderCount = (CylinderCount) cylinderComboBox.getSelectedItem();
        if (cylinderCount != null) {
            int deadPointCount;
            switch (cylinderCount) {
                case EIGHT:
                    deadPointCount = 4;
                    break;
                case SIX:
                    deadPointCount = 3;
                    break;
                default:
                    deadPointCount = 2;
                    break;
            }
            int totalCogs = (Integer) totalCogSpinner.getValue();
            int deadPoint = (Integer) offsetSpinner.getValue();
            java.util.ArrayList<Integer> deadPoints = new java.util.ArrayList<>();
            for (int i = 0; i < deadPointCount; ++i) {
                deadPoints.add(deadPoint);
                deadPoint =  (deadPoint + totalCogs / deadPointCount) % totalCogs;
                if (deadPoint == 0) {
                    deadPoint = totalCogs;
                }
            }
            cogWheelRenderer.setDeadPoints(deadPoints);
            cogWheelRenderer.repaint();
        }
    }
    
    
    private class CylinderComboBoxModel extends javax.swing.DefaultComboBoxModel<CylinderCount> {

        @Override
        public int getSize() {
            return CylinderCount.values().length;
        }

        @Override
        public CylinderCount getElementAt(int index) {
            return CylinderCount.values()[index];
        }
        
    }
    
    
    private enum CylinderCount {
        FOUR(4), SIX(6), EIGHT(8);
        
        CylinderCount(int count) {
            this.count = count;
        }
        
        @Override
        public String toString() {
            return Integer.toString(count) + "cylinder";
        }
        
        int count;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel controlPanel;
    private javax.swing.JComboBox cylinderComboBox;
    private javax.swing.JSpinner gapLengthSpinner;
    private javax.swing.JLabel minusLabel;
    private javax.swing.JLabel offsetLabel;
    private javax.swing.JSpinner offsetSpinner;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JSpinner totalCogSpinner;
    private javax.swing.JLabel typeLabel;
    // End of variables declaration//GEN-END:variables

    private final CogWheelRenderer cogWheelRenderer = new CogWheelRenderer();
    
    private final SpinnerNumberModel totalCogSpinnerModel = new SpinnerNumberModel(60, 2, 200, 1);
    private final SpinnerNumberModel gapLengthSpinnerModel = new javax.swing.SpinnerNumberModel(2, 1, 9, 1);
    private final SpinnerNumberModel offsetSpinnerModel = new SpinnerNumberModel(20, 1, 100, 1);
    
}
