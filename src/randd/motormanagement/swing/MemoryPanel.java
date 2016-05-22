/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.swing;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import randd.motormanagement.system.Flash;


public class MemoryPanel extends javax.swing.JPanel {
    
    
    public interface Listener {
        void clearButtonPressed();
        void loadButtonPressed();
    }


    public MemoryPanel(Listener listener) {
        this.listener = listener;
        initComponents();
        RowHeaders headers = new RowHeaders();
        headers.initialize(tableModel, tableScrollPane, memoryTable.getRowHeight());
        memoryTable.setDefaultRenderer(Object.class, new CellRenderer());
        elementList.addListSelectionListener(new ElementListSelectionListener());
    }


    public void setMemory(Flash flash) {
        if (this.flash != null) {
            this.flash.removeListener(flashListener);
        }
        this.flash = flash;
        if (flash != null) {
            flash.addListener(flashListener);
            tableModel.fireTableStructureChanged();
        }
    }
    
    
    Flash getMemory() {
        return flash;
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tableScrollPane = new javax.swing.JScrollPane();
        memoryTable = new javax.swing.JTable();
        elementScrollPane = new javax.swing.JScrollPane();
        elementList = new javax.swing.JList();
        clearButton = new javax.swing.JButton();
        loadButton = new javax.swing.JButton();

        memoryTable.setModel(tableModel);
        tableScrollPane.setViewportView(memoryTable);

        elementList.setModel(new javax.swing.DefaultListModel());
        elementScrollPane.setViewportView(elementList);

        clearButton.setText("Clear");
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButton_actionPerformed(evt);
            }
        });

        loadButton.setText("Load");
        loadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadButton_actionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(tableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(58, 58, 58)
                        .addComponent(clearButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(loadButton)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(elementScrollPane)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(elementScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(17, 17, 17)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(clearButton)
                    .addComponent(loadButton)))
        );
    }// </editor-fold>//GEN-END:initComponents

    
    private void clearButton_actionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButton_actionPerformed
        Object[] options = {
            UIManager.getString("OptionPane.yesButtonText", java.util.Locale.getDefault()),
            UIManager.getString("OptionPane.noButtonText", java.util.Locale.getDefault())
        };
        if (JOptionPane.showOptionDialog(
                this, "Are you sure?", "Clear flash memory", 
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, 
                null, options, options[1]) == JOptionPane.YES_OPTION) {
            listener.clearButtonPressed();
        }
    }//GEN-LAST:event_clearButton_actionPerformed

    
    private void loadButton_actionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadButton_actionPerformed
        listener.loadButtonPressed();
    }//GEN-LAST:event_loadButton_actionPerformed


    private class TableModel extends javax.swing.table.DefaultTableModel {
        
        @Override
        public int getColumnCount() {
            return (flash != null) ? Math.min(flash.getSize(), COLUMN_COUNT) : 0;
        }
        
        @Override
        public int getRowCount() {
            return (flash != null) ? flash.getSize() / COLUMN_COUNT : 0; 
        }
        
        @Override
        public String getColumnName(int column) {
            return String.format("%X", column);
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            byte b = flash.getByteAt(row * COLUMN_COUNT + column);
            return String.format("%02X", b);
        }

    }
    
    
    class ElementListSelectionListener implements javax.swing.event.ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent evt) {
            tableModel.fireTableDataChanged();
        }
        
    }
    
    
    private class RowHeaders extends bka.swing.TableRowHeaders {
        
        @Override
        public Object rowName(int row) {
            return String.format("%04X", row * COLUMN_COUNT);
        }    
        
    };
    
    
    private class CellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(JTable grid, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component renderer = super.getTableCellRendererComponent(grid, value, isSelected, hasFocus, row, column);
            if (flash != null && ! isSelected) {
                int reference = row * COLUMN_COUNT + column;
                renderer.setBackground(Color.WHITE);
                int paletteIndex = 0;
                for (int i : elementList.getSelectedIndices()) {
                    Flash.Element element = flash.getElement(i);
                    if (element.getReference() <= reference && reference < element.getReference() + element.getSize() + 3) {
                        renderer.setBackground(PALETTE[paletteIndex]);
                    }
                    paletteIndex = (paletteIndex + 1) % PALETTE.length;
                }
            }        
            return renderer;
            
        }
        
    }
    
    
    private Listener listener = null;
    
    private final TableModel tableModel = new TableModel();
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton clearButton;
    private javax.swing.JList elementList;
    private javax.swing.JScrollPane elementScrollPane;
    private javax.swing.JButton loadButton;
    private javax.swing.JTable memoryTable;
    private javax.swing.JScrollPane tableScrollPane;
    // End of variables declaration//GEN-END:variables

    
    private Flash flash = null;

    
    private final Flash.Listener flashListener = new Flash.Listener() {
        @Override
        public void refreshed() {
            tableModel.fireTableStructureChanged();
            ((javax.swing.DefaultListModel) elementList.getModel()).removeAllElements();
            for (int i = 0; i < flash.getElementCount(); ++i) {
                ((javax.swing.DefaultListModel) elementList.getModel()).addElement(flash.getElement(i));
            }
        }
    };

    
    private static final Color[] PALETTE = {
        new Color(120, 220, 125), 
        new Color(120, 190, 125), 
        new Color(230, 220, 125), 
        new Color(230, 190, 120)
    };
    
    private static final int COLUMN_COUNT = 0x10;
    
}
