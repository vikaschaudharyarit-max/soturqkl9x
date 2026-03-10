package io.github.vikaschaudharyarit_max.smartdbf.core;

import io.github.vikaschaudharyarit_max.smartdbf.exception.DbfException;
import io.github.vikaschaudharyarit_max.smartdbf.mapper.DbfMapper;
import io.github.vikaschaudharyarit_max.smartdbf.parser.DbfParser;
import io.github.vikaschaudharyarit_max.smartdbf.schema.DbfField;
import io.github.vikaschaudharyarit_max.smartdbf.schema.DbfHeader;
import io.github.vikaschaudharyarit_max.smartdbf.schema.DbfSchema;
import io.github.vikaschaudharyarit_max.smartdbf.streaming.DbfRecordIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Reads records from an open DBF file stream.
 *
 * <p>Obtain an instance via {@link Dbf#open(String)} or {@link Dbf#open(java.io.InputStream)}.
 * Always close the reader when done — either explicitly via {@link #close()} or via
 * try-with-resources since {@code DbfReader} implements {@link AutoCloseable}.
 *
 * <pre>{@code
 * try (DbfReader reader = Dbf.open("data.dbf")) {
 *     while (reader.hasNext()) {
 *         Object[] row = reader.nextRecord();
 *     }
 * }
 * }</pre>
 *
 * <p>Field types are converted as follows:
 * <ul>
 *   <li>{@code C} (Character) → {@link String}</li>
 *   <li>{@code N} (Numeric) → {@link java.math.BigDecimal} (preserves scale)</li>
 *   <li>{@code D} (Date, yyyyMMdd) → {@link LocalDate}</li>
 *   <li>{@code L} (Logical) → {@link Boolean}</li>
 *   <li>All others → {@link String}</li>
 * </ul>
 * Empty / blank fields return {@code null}.
 */
public class DbfReader implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DbfReader.class);
    private static final DateTimeFormatter DBF_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InputStream inputStream;
    private final Charset charset;

    private final DbfHeader header;
    private final DbfSchema schema;

    private int currentRecord = 0;

    DbfReader(InputStream inputStream, Charset charset) {
        this.inputStream = inputStream;
        this.charset = charset;

        DbfParser parser = new DbfParser(inputStream);
        parser.parse();

        this.header = parser.getHeader();
        this.schema = parser.getSchema();

        log.debug("Opened DBF: {} records, {} fields, charset={}",
                header.getNumberOfRecords(), schema.getFields().size(), charset.name());
    }

    /**
     * Returns the schema (column names, types, lengths) of this DBF file.
     */
    public DbfSchema getSchema() {
        return schema;
    }

    /**
     * Returns {@code true} if there are more records to read.
     */
    public boolean hasNext() {
        return currentRecord < header.getNumberOfRecords();
    }

    /**
     * Reads and returns the next record as an {@code Object[]} array.
     * The array indices correspond to {@link DbfSchema#getFields()}.
     * Deleted records (marked with {@code 0x2A}) are skipped automatically.
     *
     * @return the next record's field values
     * @throws DbfException if the record cannot be read
     */
    public Object[] nextRecord() {
        try {
            byte[] recordBytes = new byte[header.getRecordLength()];
            readFully(recordBytes);
            currentRecord++;

            if (recordBytes[0] == 0x2A) {
                log.debug("Skipping deleted record at index {}", currentRecord);
                return nextRecord();
            }

            List<DbfField> fields = schema.getFields();
            Object[] values = new Object[fields.size()];
            int offset = 1;

            for (int i = 0; i < fields.size(); i++) {
                DbfField field = fields.get(i);
                String raw = new String(recordBytes, offset, field.getLength(), charset).trim();
                values[i] = convertValue(field, raw);
                offset += field.getLength();
            }

            log.trace("Read record {}/{}", currentRecord, header.getNumberOfRecords());
            return values;

        } catch (DbfException e) {
            throw e;
        } catch (Exception e) {
            throw new DbfException(
                    "Failed reading record " + currentRecord + " of " + header.getNumberOfRecords(), e);
        }
    }

    /**
     * Converts a raw trimmed string value from the DBF byte stream into the appropriate Java type.
     *
     * <ul>
     *   <li>C → String</li>
     *   <li>N → BigDecimal (preserves decimal scale; null if blank/unparseable)</li>
     *   <li>D → LocalDate parsed from "yyyyMMdd" (null if blank/invalid)</li>
     *   <li>L → Boolean (Y/T = true, N/F = false, null if blank)</li>
     *   <li>* → String (fallback for memo, binary, etc.)</li>
     * </ul>
     */
    private Object convertValue(DbfField field, String value) {
        if (value.isEmpty()) {
            return null;
        }

        switch (field.getType()) {
            case 'C':
                return value;

            case 'N':
            case 'F':
                try {
                    return new java.math.BigDecimal(value.trim());
                } catch (NumberFormatException e) {
                    log.warn("Could not parse numeric value '{}' for field '{}', returning null",
                            value, field.getName());
                    return null;
                }

            case 'D':
                try {
                    return LocalDate.parse(value.trim(), DBF_DATE);
                } catch (DateTimeParseException e) {
                    log.warn("Could not parse date value '{}' for field '{}', returning null",
                            value, field.getName());
                    return null;
                }

            case 'L':
                char c = value.charAt(0);
                if (c == 'Y' || c == 'y' || c == 'T' || c == 't') return Boolean.TRUE;
                if (c == 'N' || c == 'n' || c == 'F' || c == 'f') return Boolean.FALSE;
                return null;

            default:
                return value;
        }
    }

    private void readFully(byte[] buffer) throws Exception {
        int bytesRead = 0;
        while (bytesRead < buffer.length) {
            int n = inputStream.read(buffer, bytesRead, buffer.length - bytesRead);
            if (n == -1) {
                throw new DbfException(
                        "Unexpected end of DBF stream at byte " + bytesRead +
                        " (expected record length " + buffer.length + ")");
            }
            bytesRead += n;
        }
    }

    /**
     * Returns a sequential {@link Stream} of raw {@code Object[]} records.
     * The stream closes the underlying reader when the terminal operation completes or the
     * stream is closed.
     */
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

    /**
     * Reads all remaining records and maps them to instances of {@code type} using
     * {@link io.github.vikaschaudharyarit_max.smartdbf.annotation.DbfColumn} annotations.
     *
     * @param type the target POJO class (must have a public no-arg constructor)
     * @return list of mapped instances
     */
    public <T> List<T> read(Class<T> type) {
        DbfMapper<T> mapper = new DbfMapper<>(type, this);
        List<T> result = new ArrayList<>();
        while (hasNext()) {
            result.add(mapper.map(nextRecord()));
        }
        log.debug("Read {} records into {}", result.size(), type.getSimpleName());
        return result;
    }

    /**
     * Returns a sequential {@link Stream} of records mapped to instances of {@code type}.
     *
     * @param type the target POJO class (must have a public no-arg constructor)
     */
    public <T> Stream<T> stream(Class<T> type) {
        DbfMapper<T> mapper = new DbfMapper<>(type, this);
        return stream().map(mapper::map);
    }

    /**
     * Closes the underlying input stream. Safe to call multiple times.
     */
    @Override
    public void close() {
        try {
            inputStream.close();
            log.debug("DbfReader closed");
        } catch (Exception ignored) {}
    }
}
