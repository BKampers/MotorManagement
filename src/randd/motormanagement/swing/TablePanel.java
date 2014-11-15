/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.swing;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import randd.motormanagement.system.*;


public class TablePanel extends JPanel {

    
    public interface Listener {
        void startIndexPoll(Table table);
        void setValue(Table table, int column, int row, float value);
    }
    

    public TablePanel(Listener listener, Table table) {
        assert listener != null;
        this.tablePanelListener = listener;
        setTable(table);
        initComponents();
        grid.setDefaultRenderer(Object.class, new CellRenderer());
        JTableHeader columnHeader = grid.getTableHeader();
        columnHeader.setDefaultRenderer(new ColumnHeaderRenderer(grid));
        columnHeader.setResizingAllowed(false);
        columnHeader.setEnabled(false);
        RowHeaders rowHeaders = new RowHeaders();
        rowHeaders.initialize(model, scrollPane, grid.getRowHeight());
        rowHeaders.setAlignment(JLabel.RIGHT, JLabel.TOP);

        grid.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                followActiveCell = false;
            }
        });
    }
    
    
    Table getTable() {
        return table;
    }
    
    
    private void setTable(Table table) {
        if (this.table != null) {
            this.table.removeListener(tableListener);
        }
        this.table = table;
        if (table != null) {
            table.addListener(tableListener);
        }
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
        grid = new javax.swing.JTable();

        setLayout(new java.awt.BorderLayout());

        grid.setModel(model);
        grid.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        grid.setGridColor(new java.awt.Color(0, 0, 0));
        grid.setRowHeight(30);
        grid.setRowSelectionAllowed(false);
        grid.getTableHeader().setReorderingAllowed(false);
        scrollPane.setViewportView(grid);

        add(scrollPane, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents


    private class Model extends DefaultTableModel {
        
        @Override
        public int getColumnCount() {
            return (table != null) ? table.getColumnCount() : 0;
        }

        @Override
        public int getRowCount() {
            return (table != null) ? table.getRowCount() : 0;
        }
        
        @Override
        public String getColumnName(int column) {
            Measurement columnMeasurement = table.getColumnMeasurement();
            if (columnMeasurement != null) {
                float stepSize = (columnMeasurement.getMaximum() - columnMeasurement.getMinimum()) / table.getColumnCount();
                return Integer.toString((int) (columnMeasurement.getMinimum() + column * stepSize));
            }
            else {
                return Integer.toString(column + 1);
            }
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            return table.getField(column, row);
        }
        
        @Override
        public void setValueAt(Object value, int row, int column) {
            try {
                tablePanelListener.setValue(table, column, row, ((Number) value).floatValue());
            }
            catch (ClassCastException ex) {
                JOptionPane.showMessageDialog(
                    TablePanel.this, 
                    value.toString(), 
                    "Invalid input",
                    JOptionPane.ERROR_MESSAGE);
            }
        }

    }
    
    
    private class ColumnHeaderRenderer implements TableCellRenderer {

        ColumnHeaderRenderer(JTable table) {
            renderer = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
            renderer.setHorizontalAlignment(JLabel.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            return renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
        }

        private final DefaultTableCellRenderer renderer;

    }
    
    
    private class RowHeaders extends bka.swing.TableRowHeaders {
        
        @Override
        public String cornerName() {
            return "RPM / Load";
        }
        
        @Override
        public Object rowName(int row) {
            Measurement rowMeasurement = table.getRowMeasurement();
            if (rowMeasurement != null) {
                float stepSize = (rowMeasurement.getMaximum() - rowMeasurement.getMinimum()) / table.getRowCount();
                return Integer.toString((int) (rowMeasurement.getMinimum() + row * stepSize));
            }
            else {
                return Integer.toString(row + 1);
            }
        }
        
    };
    
    private class CellRenderer extends DefaultTableCellRenderer {
        
        CellRenderer() {
            setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
            activeRenderer.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
            activeRenderer.setOpaque(true);
            activeRenderer.setBackground(Color.YELLOW);
            activeRenderer.setBorder(activeCellBorder);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable grid, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String valueString = numberFormat.format(value);
            if (column == table.getColumnIndex() && row == table.getRowIndex()) {
                activeColumn = column;
                activeRow = row;
                activeRenderer.setText(valueString);
                return activeRenderer;
            }
            else {
                return super.getTableCellRendererComponent(grid, valueString, isSelected, hasFocus, row, column);
            }
        }
        
        private final JLabel activeRenderer = new JLabel();
 
        private final Border activeCellBorder = new LineBorder(Color.ORANGE, 1) {
            @Override
            public Insets getBorderInsets(Component c) {
                return CellRenderer.this.getInsets();
            }
            @Override
            public Insets getBorderInsets(Component c, Insets insets) {
                return CellRenderer.this.getInsets(insets);
            }
        };
         
    }
    
    
    private class CellEditor extends DefaultCellEditor {

        CellEditor() {
            super(new JTextField());
            int decimals = table.getDecimals();
            double stepSize = Math.pow(10.0, -decimals);
            spinner = new JSpinner(new SpinnerNumberModel(0.0, table.getMinimum(), table.getMaximum(), stepSize));
            spinner.setBorder(new LineBorder(Color.GREEN, 2));            
            final JSpinner.NumberEditor editor = (JSpinner.NumberEditor) spinner.getEditor();
            java.text.DecimalFormat format = editor.getFormat();  
            format.setMinimumFractionDigits(decimals);
            format.setGroupingUsed(false);
            format.setDecimalFormatSymbols(new java.text.DecimalFormatSymbols(Locale.getDefault()));
            JFormattedTextField editorTextField = editor.getTextField();
            editorTextField.addKeyListener(new java.awt.event.KeyListener() {
                
                @Override
                public void keyTyped(KeyEvent evt) {
                }

                @Override
                public void keyPressed(KeyEvent evt) {
                }

                @Override
                public void keyReleased(KeyEvent evt) {
                    System.out.println(evt.getKeyChar());
                    String currentText = ((JFormattedTextField) evt.getSource()).getText();
                    try {
                        float newValue = numberFormat.parse(currentText).floatValue();
                        if (newValue < table.getMinimum() || table.getMaximum() < newValue) {
                            spinner.setBorder(new LineBorder(Color.RED, 2));
                            if (evt.getKeyCode() == KeyEvent.VK_BACK_SPACE || evt.getKeyCode() == KeyEvent.VK_DELETE) {
                                Toolkit.getDefaultToolkit().beep();
                            }
                        }
                        else {
                            spinner.setBorder(new LineBorder(Color.GREEN, 2));
                        }
                    }
                    catch (java.text.ParseException ex) {
                        ((JFormattedTextField) evt.getSource()).setText(currentText);
                        evt.consume();
                    }
                }
            });
            spinner.setEditor(editor);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            spinner.setValue(value);
            return spinner;
        }
        
        @Override
        public Object getCellEditorValue() {
            return spinner.getValue(); 
        }
        
        private final JSpinner spinner;
        
    }
    
    
    private class TableListener implements Table.Listener {

        @Override
        public void propertyChanged(Table table, Table.Property property) {
            if (table == TablePanel.this.table) {
                switch (property) {
                    case INDEX:
                        indexChanged();
                        break;
                    case VALUE:
                        valueChanged(table.getRowIndex(), table.getColumnIndex());
                        break;
                    default:
                        initializationPropertyChanged(property);
                        break;
                }
            }
        }
        
        private void initializationPropertyChanged(Table.Property property) {
            uninitializedProperties.remove(property);
            if (uninitializedProperties.isEmpty()) {
                numberFormat.setMinimumFractionDigits(table.getDecimals());
                grid.setDefaultEditor(Object.class, new CellEditor());
                model.fireTableStructureChanged();
            }
        }
        
        private void indexChanged() {
            int rowIndex = table.getRowIndex();
            int columnIndex = table.getColumnIndex();
            if (rowIndex != activeRow || columnIndex != activeColumn) {
                valueChanged(activeRow, activeColumn);
                valueChanged(rowIndex, columnIndex);
            }
            if (followActiveCell) {
                Rectangle rectangle = grid.getCellRect(table.getRowIndex(), table.getColumnIndex(), true);
                grid.scrollRectToVisible(rectangle);
            }
        }
        
        private void valueChanged(int row, int column) {
            if (0 <= row && row < model.getRowCount() && 0 <= column && column < model.getColumnCount()) {
                model.fireTableCellUpdated(row, column);
            }
        }
        
        private final Set<Table.Property> uninitializedProperties = EnumSet.of(
            Table.Property.FIELDS,
            Table.Property.MINIMUM,
            Table.Property.MAXIMUM,
            Table.Property.DECIMALS,
            Table.Property.COLUMN_MEASUREMENT,
            Table.Property.ROW_MEASUREMENT);
        
    }

    
    private Table table;   
    private final java.text.NumberFormat numberFormat = java.text.NumberFormat.getNumberInstance();
    
    private int activeColumn = -1;
    private int activeRow = -1;
    private boolean followActiveCell = true;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable grid;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables

    private final Listener tablePanelListener;
    private final Model model = new Model();
    private final TableListener tableListener = new TableListener();
    
}
