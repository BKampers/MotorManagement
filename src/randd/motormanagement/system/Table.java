/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.system;

import java.util.*;


public class Table {
    
    
    public enum Property {
        COLUMN_INDEX,
        COLUMN_MEASUREMENT,
        DECIMALS,
        ENABLED,
        FIELDS,
        MAXIMUM,
        MINIMUM,
        PROGRAMMABLE,
        PROGRAMMER_ACTIVATED,
        ROW_INDEX,
        ROW_MEASUREMENT,
        VALUE
    }


    public interface Listener {
        public void propertyChanged(Table table, Property property, Object ... attributes);
    }
        
    private Table(String name) {
        this.name = name;
    }

    public static Table getInstance(String name) {
        return instances.computeIfAbsent(name, Table::new);
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
            notifyPropertyChanged(Property.VALUE, column, row);
        }
        else {
            throw new java.lang.IndexOutOfBoundsException();
        }
    }
        
    public float getField(int column, int row) {
        if (0 <= column && column < columnCount && 0 <= row && row < rowCount) {
            return fields[row][column];
        }
        throw new java.lang.IndexOutOfBoundsException();
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
    
    public Optional<Integer> getColumnIndex() {
        return getOptionalProperty(Property.COLUMN_INDEX);
    }
    
    public void setColumnIndex(int columnIndex) {
        setProperty(Property.COLUMN_INDEX, columnIndex);
    }

    public Optional<Integer> getRowIndex() {
        return getOptionalProperty(Property.ROW_INDEX);
    }
    
    public void setRowIndex(int rowIndex) {
        setProperty(Property.ROW_INDEX, rowIndex);
    }
    
    public Optional<Boolean> isEnabled() {
        return getOptionalProperty(Property.ENABLED);
    }

    public void setEnabled(boolean enabled) {
        setProperty(Property.ENABLED, enabled);
    }
    
    public boolean isProgrammable() {
        return getProperty(Property.PROGRAMMABLE);
    }
        
    public void setProgrammable(boolean programmable) {
        setProperty(Property.PROGRAMMABLE, programmable);
    }
    
    public boolean isProgrammerActivated() {
        return getProperty(Property.PROGRAMMER_ACTIVATED);
    }
        
    public void setProgrammerActivated(boolean programmerActivated) {
        setProperty(Property.PROGRAMMER_ACTIVATED, programmerActivated);
    }
        
    private void setProperty(Property property, Object value) {
        if (!value.equals(properties.get(property))) {
            properties.put(property, value);
            notifyPropertyChanged(property, value);
        }
    }
    
    private <T> T getProperty(Property property) {
        return (T) Objects.requireNonNull(properties.get(property), () -> "Property '" + property.name() +  "' not set");
    }
    
    private <T> Optional<T> getOptionalProperty(Property property) {
        return Optional.ofNullable((T) properties.get(property));
    }
    
    public void addListener(Listener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void removeListener(Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    private void notifyPropertyChanged(Property property, Object ... attributes) {
        synchronized (listeners) {
            listeners.forEach(listener -> listener.propertyChanged(this, property, attributes));        
        }
    }
        
    private static final Map<String, Table> instances = new HashMap<>();
    
    private String name;
    
    private int columnCount = 0;
    private int rowCount = 0;
    
    private Measurement columnMeasurement;
    private Measurement rowMeasurement;

    private float minimum = 0.0f;
    private float maximum = 100.0f;
    private int decimals = 0;
    
    private float[][] fields = null;
    
    private final Map<Property, Object> properties = new HashMap<>();
    private final Collection<Listener> listeners = new ArrayList<>();
}
