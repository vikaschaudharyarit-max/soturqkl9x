package io.github.vikaschaudharyarit_max.smartdbf.core;

import io.github.vikaschaudharyarit_max.smartdbf.parser.DbfParser;
import io.github.vikaschaudharyarit_max.smartdbf.schema.*;
import io.github.vikaschaudharyarit_max.smartdbf.streaming.DbfRecordIterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.Spliterators;
import java.util.Spliterator;
import io.github.vikaschaudharyarit_max.smartdbf.mapper.DbfMapper;
import java.util.ArrayList;
import java.util.List;

import java.io.InputStream;

public class DbfReader {

    private final InputStream inputStream;

    private DbfParser parser;
    private DbfHeader header;
    private DbfSchema schema;

    private int currentRecord = 0;

    public DbfReader(InputStream inputStream) {

        this.inputStream = inputStream;

        parser = new DbfParser(inputStream);
        parser.parse();

        header = parser.getHeader();
        schema = parser.getSchema();
    }

    public DbfSchema getSchema() {
        return schema;
    }

    public boolean hasNext() {
        return currentRecord < header.getNumberOfRecords();
    }

    public Object[] nextRecord() {

        try {

            byte[] recordBytes = new byte[header.getRecordLength()];
            readFully(recordBytes);

            currentRecord++;

            if (recordBytes[0] == 0x2A) {
                return nextRecord(); // skip deleted
            }

            Object[] values = new Object[schema.getFields().size()];

            int offset = 1;

            for (int i = 0; i < schema.getFields().size(); i++) {

                DbfField field = schema.getFields().get(i);

                int length = field.getLength();

                String raw = new String(recordBytes, offset, length).trim();

                values[i] = convertValue(field, raw);

                offset += length;
            }

            currentRecord++;

            return values;

        } catch (Exception e) {
            throw new RuntimeException("Failed reading record", e);
        }
    }

    private Object convertValue(DbfField field, String value) {

        if (value.isEmpty()) {
            return null;
        }

        switch (field.getType()) {

            case 'C':
                return value;

            case 'N':
                try {
                    return value.isEmpty() ? null : Double.parseDouble(value.trim());
                } catch (Exception e) {
                    return null;
                }

            case 'L':
                return value.equalsIgnoreCase("Y")
                        || value.equalsIgnoreCase("T");

            case 'D':
                return value;

            default:
                return value;
        }
    }

    public void readFully(byte[] buffer) throws Exception {

        int bytesRead = 0;

        while (bytesRead < buffer.length) {

            int n = inputStream.read(buffer, bytesRead, buffer.length - bytesRead);

            if (n == -1) {
                throw new RuntimeException("Unexpected end of DBF file");
            }

            bytesRead += n;
        }
    }

    public Stream<Object[]> stream() {

        DbfRecordIterator iterator = new DbfRecordIterator(this);

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        iterator,
                        Spliterator.ORDERED | Spliterator.NONNULL
                ),
                false
        ).onClose(this::close);
    }

    public void close() {
        try {
            inputStream.close();
        } catch (Exception ignored) {}
    }

    public <T> List<T> read(Class<T> type) {

        DbfMapper<T> mapper = new DbfMapper<>(type, this);

        List<T> result = new ArrayList<>();

        while (hasNext()) {

            Object[] row = nextRecord();

            result.add(mapper.map(row));
        }

        return result;
    }

    public <T> Stream<T> stream(Class<T> type) {

        DbfMapper<T> mapper = new DbfMapper<>(type, this);
        
        return stream().map(mapper::map);
    }
}
