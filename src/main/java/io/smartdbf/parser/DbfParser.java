package io.smartdbf.parser;

import io.smartdbf.schema.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DbfParser {

    private final InputStream inputStream;

    private DbfHeader header;
    private DbfSchema schema;

    public DbfParser(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void parse() {

        try {

            byte[] headerBytes = new byte[32];
            inputStream.read(headerBytes);

            int numberOfRecords =
                    ((headerBytes[7] & 0xFF) << 24)
                            | ((headerBytes[6] & 0xFF) << 16)
                            | ((headerBytes[5] & 0xFF) << 8)
                            | (headerBytes[4] & 0xFF);

            int headerLength =
                    ((headerBytes[9] & 0xFF) << 8)
                            | (headerBytes[8] & 0xFF);

            int recordLength =
                    ((headerBytes[11] & 0xFF) << 8)
                            | (headerBytes[10] & 0xFF);

            header = new DbfHeader(numberOfRecords, headerLength, recordLength);

            List<DbfField> fields = new ArrayList<>();

            while (true) {

                byte[] fieldBytes = new byte[32];
                inputStream.read(fieldBytes);

                if (fieldBytes[0] == 0x0D) {
                    break;
                }

                String name = new String(fieldBytes, 0, 11).trim();
                char type = (char) fieldBytes[11];
                int length = fieldBytes[16] & 0xFF;
                int decimalCount = fieldBytes[17] & 0xFF;

                fields.add(new DbfField(name, type, length, decimalCount));
            }

            schema = new DbfSchema(fields);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse DBF", e);
        }
    }

    public DbfHeader getHeader() {
        return header;
    }

    public DbfSchema getSchema() {
        return schema;
    }
}