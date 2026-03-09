package io.smartdbf.mapper;

import java.lang.reflect.Field;

public class FieldBinding {

    private final int columnIndex;
    private final Field field;

    public FieldBinding(int columnIndex, Field field) {
        this.columnIndex = columnIndex;
        this.field = field;
        this.field.setAccessible(true);
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public Field getField() {
        return field;
    }
}