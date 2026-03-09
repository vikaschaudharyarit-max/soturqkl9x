package io.smartdbf.schema;

public class DbfField {

    private final String name;
    private final char type;
    private final int length;
    private final int decimalCount;

    public DbfField(String name, char type, int length, int decimalCount) {
        this.name = name;
        this.type = type;
        this.length = length;
        this.decimalCount = decimalCount;
    }

    public String getName() {
        return name;
    }

    public char getType() {
        return type;
    }

    public int getLength() {
        return length;
    }

    public int getDecimalCount() {
        return decimalCount;
    }

    @Override
    public String toString() {
        return name + " (" + type + "," + length + ")";
    }
}