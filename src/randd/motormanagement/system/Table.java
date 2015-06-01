/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.system;

import java.util.*;


public class Table {
    
    
    public enum Property {
        FIELDS,
        DECIMALS,
        MINIMUM,
        MAXIMUM,
        COLUMN_MEASUREMENT,
        ROW_MEASUREMENT,
        INDEX,
        VALUE,
        ENABLED
    }


    public interface Listener {
        public void propertyChanged(Table table, Property property);
    }
    
    
    public static Table getInstance(String name) {
        Table instance = instances.get(name);
        if (instance == null) {
            instance = new Table(name);
            instances.put(name, instance);
        }
        return instance;
    }
    
    
    public void setFields(float[][] fields) {
        columnCount = 0;
        rowCount = 0;
        if (fields != null) {
            rowCount = fields.length;
            this.fields = new float[rowCount][];
            columnCount = Integer.MAX_VALUE;
            for (int row = 0; row < rowCount; ++row) {
                columnCount = Math.min(columnCount, fields[row].length);
            }
            for (int row = 0; row < rowCount; ++row) {
                this.fields[row] = new float[columnCount];
                for (int column = 0; column < columnCount; ++ column) {
                    this.fields[row][column] = fields[row][column];
                }
                
            }
            if (rowCount > 0) {
                columnCount = fields[0].length;
                for (int row = 1; row < rowCount; ++row) {
                    columnCount = Math.min(columnCount, fields[row].length);
                }
            }
            else {
                this.fields = null;
            }
        }
        notifyPropertyChanged(Property.FIELDS);
    }
    
    
    public boolean hasFields() {
        return fields != null;
    }
    
    
    public void setField(int column, int row, float value) {
        if (0 <= column && column < columnCount && 0 <= row && row < rowCount) {
            fields[row][column] = value;
            notifyPropertyChanged(Property.VALUE);
        }
        else {
            throw new java.lang.IndexOutOfBoundsException();
        }
    }
    
    
    public float getField(int column, int row) {
        if (0 <= column && column < columnCount && 0 <= row && row < rowCount) {
            return fields[row][column];
        }
        else {
            throw new java.lang.IndexOutOfBoundsException();
        }
    }

    
    public String getName() {
        return name;
    }

    
    public void setName(String name) {
        this.name = name;
    }

    
    public float getMinimum() {
        return minimum;
    }
    
    
    public void setMinimum(float minimum) {
        this.minimum = minimum;
        notifyPropertyChanged(Property.MINIMUM);
    }
    
    
    public float getMaximum() {
        return maximum;
    }

        
    public void setMaximum(float maximum) {
        this.maximum = maximum;
        notifyPropertyChanged(Property.MAXIMUM);
    }
    
    
    public int getDecimals() {
        return decimals;
    }

    
    public void setDecimals(int decimals) {
        this.decimals = decimals;
        notifyPropertyChanged(Property.DECIMALS);
    }
    
    
    public Measurement getColumnMeasurement() {
        return columnMeasurement;
    }

    
    public void setColumnMeasurement(Measurement columnMeasurement) {
        this.columnMeasurement = columnMeasurement;
        notifyPropertyChanged(Property.COLUMN_MEASUREMENT);
    }

    
    public Measurement getRowMeasurement() {
        return rowMeasurement;
    }


    public void setRowMeasurement(Measurement rowMeasurement) {
        this.rowMeasurement = rowMeasurement;
        notifyPropertyChanged(Property.ROW_MEASUREMENT);
    }

    
    public int getColumnCount() {
        return columnCount;
    }

    
    public int getRowCount() {
        return rowCount;
    }

    
    public int getColumnIndex() {
        return columnIndex;
    }

    
    public void setColumnIndex(int columnIndex) {
        if (this.columnIndex != columnIndex) {
            this.columnIndex = columnIndex;
            notifyPropertyChanged(Property.INDEX);
        }
    }

    
    public int getRowIndex() {
        return rowIndex;
    }

    
    public void setRowIndex(int rowIndex) {
        if (this.rowIndex != rowIndex) {
            this.rowIndex = rowIndex;
            notifyPropertyChanged(Property.INDEX);
        }
    }

    
    public boolean isEnabled() {
        return enabled;
    }

    
    public void setEnabled(boolean enabled) {
        Boolean newValue = enabled;
        if (! newValue.equals(this.enabled)) {
            this.enabled = newValue;
            notifyPropertyChanged(Property.ENABLED);
        }
    }
    
    
    public void addListener(Listener listener) {
        synchronized (listeners) {
            if (! listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    
    public void removeListener(Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    
    private void notifyPropertyChanged(Property property) {
        synchronized (listeners) {
            for (Listener listener : listeners) {
                listener.propertyChanged(this, property);
            }        
        }
    }
    
    
    private Table(String name) {
        this.name = name;
    }
    
    
    private static final java.util.Map<String, Table> instances = new java.util.HashMap<>();
    
    
    private String name;
    
    private int columnCount = 0;
    private int rowCount = 0;
    
    private int columnIndex = -1;
    private int rowIndex = -1;
    
    private Measurement columnMeasurement;
    private Measurement rowMeasurement;

    private float minimum = 0.0f;
    private float maximum = 100.0f;
    private int decimals = 0;
    
    private Boolean enabled;
    
    private float[][] fields = null;
    
    private final Collection<Listener> listeners = new ArrayList<>();
}
