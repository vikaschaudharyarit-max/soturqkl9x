package io.github.vikaschaudharyarit_max.smartdbf.parser;

import io.github.vikaschaudharyarit_max.smartdbf.exception.DbfException;
import io.github.vikaschaudharyarit_max.smartdbf.schema.DbfField;
import io.github.vikaschaudharyarit_max.smartdbf.schema.DbfHeader;
import io.github.vikaschaudharyarit_max.smartdbf.schema.DbfSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parses the binary header and field descriptor array from a DBF file stream.
 *
 * <p>The DBF III/IV header layout is:
 * <ol>
 *   <li>32-byte file header block (version, record count, header length, record length)</li>
 *   <li>N × 32-byte field descriptor blocks</li>
 *   <li>1-byte field terminator (0x0D), possibly followed by padding to {@code headerLength}</li>
 * </ol>
 *
 * <p>This parser reads exactly {@code headerLength} bytes from the stream so that the stream
 * is positioned precisely at the first data record when {@link #parse()} returns, regardless
 * of how much padding follows the field terminator.
 */
public class DbfParser {

    private static final Logger log = LoggerFactory.getLogger(DbfParser.class);

    private static final int FILE_HEADER_SIZE    = 32;
    private static final int FIELD_DESCRIPTOR_SIZE = 32;
    private static final byte FIELD_TERMINATOR   = 0x0D;

    private final InputStream inputStream;
    private DbfHeader header;
    private DbfSchema schema;

    public DbfParser(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Parses the DBF file header and field descriptors from the stream.
     * When this method returns, the stream is positioned at the first data record.
     *
     * @throws DbfException if the stream is too short, corrupt, or unreadable
     */
    public void parse() {
        try {
            // --- 1. Read the 32-byte file header ---
            byte[] fileHeader = readExactly(FILE_HEADER_SIZE, "file header");

            int numberOfRecords =
                    ((fileHeader[7] & 0xFF) << 24)
                    | ((fileHeader[6] & 0xFF) << 16)
                    | ((fileHeader[5] & 0xFF) << 8)
                    |  (fileHeader[4] & 0xFF);

            int headerLength =
                    ((fileHeader[9] & 0xFF) << 8)
                    |  (fileHeader[8] & 0xFF);

            int recordLength =
                    ((fileHeader[11] & 0xFF) << 8)
                    |  (fileHeader[10] & 0xFF);

            header = new DbfHeader(numberOfRecords, headerLength, recordLength);
            log.debug("DBF header: records={}, headerLength={}, recordLength={}",
                    numberOfRecords, headerLength, recordLength);

            // --- 2. Read the entire field descriptor section in one go ---
            // This guarantees the stream ends up exactly at the first data record.
            int fieldSectionSize = headerLength - FILE_HEADER_SIZE;
            if (fieldSectionSize <= 0) {
                throw new DbfException(
                        "Invalid DBF headerLength=" + headerLength + "; must be > " + FILE_HEADER_SIZE);
            }

            byte[] fieldSection = readExactly(fieldSectionSize, "field descriptors");

            // --- 3. Parse field descriptors until terminator ---
            List<DbfField> fields = new ArrayList<>();
            int offset = 0;

            while (offset + FIELD_DESCRIPTOR_SIZE <= fieldSection.length) {
                if (fieldSection[offset] == FIELD_TERMINATOR) {
                    break;
                }

                byte[] fd = Arrays.copyOfRange(fieldSection, offset, offset + FIELD_DESCRIPTOR_SIZE);
                String name = new String(fd, 0, 11).replace("\0", "").trim();
                char   type = (char) fd[11];
                int    length      = fd[16] & 0xFF;
                int    decimalCount = fd[17] & 0xFF;

                fields.add(new DbfField(name, type, length, decimalCount));
                log.debug("Field: name='{}', type='{}', length={}, decimals={}",
                        name, type, length, decimalCount);

                offset += FIELD_DESCRIPTOR_SIZE;
            }

            schema = new DbfSchema(fields);
            log.debug("Parsed {} field(s) from DBF header", fields.size());

        } catch (DbfException e) {
            throw e;
        } catch (Exception e) {
            throw new DbfException("Failed to parse DBF file header", e);
        }
    }

    /** Reads exactly {@code length} bytes, throwing if the stream ends prematurely. */
    private byte[] readExactly(int length, String section) throws Exception {
        byte[] buf = new byte[length];
        int total = 0;
        while (total < length) {
            int n = inputStream.read(buf, total, length - total);
            if (n == -1) {
                throw new DbfException(
                        "Unexpected end of stream while reading " + section +
                        " (read " + total + " of " + length + " bytes)");
            }
            total += n;
        }
        return buf;
    }

    public DbfHeader getHeader() {
        return header;
    }

    public DbfSchema getSchema() {
        return schema;
    }
}
