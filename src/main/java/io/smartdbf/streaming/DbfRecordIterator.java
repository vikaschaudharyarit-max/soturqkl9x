package io.smartdbf.streaming;

import io.smartdbf.core.DbfReader;

import java.util.Iterator;

public class DbfRecordIterator implements Iterator<Object[]> {

    private final DbfReader reader;

    public DbfRecordIterator(DbfReader reader) {
        this.reader = reader;
    }

    @Override
    public boolean hasNext() {
        return reader.hasNext();
    }

    @Override
    public Object[] next() {
        return reader.nextRecord();
    }
}