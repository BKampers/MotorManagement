/*
** Copyright © Bart Kampers
*/

package randd.motormanagement.system;

import java.util.*;


public class Table {


    public interface Listener {
        public void created(Table table);
        public void modified(Table table);
        public void modified(Table table, int column, int row);
    }
    
    
    public static Table getInstance(String name) {
        Table instance = instances.get(name);
        if (instance == null) {
            instance = new Table(name);
            instances.put(name, instance);
        }
        return instance;
    }
    
    
    public void setFields(short[][] fields) {
        columnCount = 0;
        rowCount = 0;
        this.fields = fields;
        if (fields != null) {
            rowCount = fields.length;
            if (rowCount > 0) {
                columnCount = fields[0].length;
                for (int row = 1; row < rowCount; ++row) {
                    columnCount = Math.min(columnCount, fields[row].length);
                }
            }
        }
        synchronized (listeners) {
            for (Listener listener : listeners) {
                listener.created(this);
            }
        }
    }
    
    
    public boolean hasFields() {
        return fields != null;
    }
    
    
    public void setField(int column, int row, short value) {
        if (0 <= column && column < columnCount && 0 <= row && row < rowCount) {
            fields[row][column] = value;
            notifyModification(column, row);
        }
        else {
            throw new java.lang.IndexOutOfBoundsException();
        }
    }
    
    
    public short getField(int column, int row) {
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
            notifyModification();
        }
    }

    
    public int getRowIndex() {
        return rowIndex;
    }

    
    public void setRowIndex(int rowIndex) {
        if (this.rowIndex != rowIndex) {
            this.rowIndex = rowIndex;
            notifyModification();
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

    
    private void notifyModification() {
        synchronized (listeners) {
            for (Listener listener : listeners) {
                listener.modified(this);
            }
        }
    }
    
    
    private void notifyModification(int column, int row) {
        synchronized (listeners) {
            for (Listener listener : listeners) {
                listener.modified(this, column, row);
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

    private short[][] fields = null;
    
    private final Collection<Listener> listeners = new ArrayList<>();
}
