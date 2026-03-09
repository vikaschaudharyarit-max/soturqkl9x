package io.github.vikaschaudharyarit_max.smartdbf.schema;

public class DbfHeader {

    private final int numberOfRecords;
    private final int headerLength;
    private final int recordLength;

    public DbfHeader(int numberOfRecords, int headerLength, int recordLength) {
        this.numberOfRecords = numberOfRecords;
        this.headerLength = headerLength;
        this.recordLength = recordLength;
    }

    public int getNumberOfRecords() {
        return numberOfRecords;
    }

    public int getHeaderLength() {
        return headerLength;
    }

    public int getRecordLength() {
        return recordLength;
    }
}
