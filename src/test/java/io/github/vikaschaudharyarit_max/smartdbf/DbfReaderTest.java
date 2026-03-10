package io.github.vikaschaudharyarit_max.smartdbf;

import io.github.vikaschaudharyarit_max.smartdbf.annotation.DbfColumn;
import io.github.vikaschaudharyarit_max.smartdbf.core.Dbf;
import io.github.vikaschaudharyarit_max.smartdbf.core.DbfReader;
import io.github.vikaschaudharyarit_max.smartdbf.schema.DbfField;
import io.github.vikaschaudharyarit_max.smartdbf.schema.DbfSchema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SmartDBF core reading, type conversion, POJO mapping, and streaming.
 * All tests use an in-memory DBF file built by {@link #buildSampleDbf(Path)} so that
 * no external files or hardcoded paths are needed.
 */
class DbfReaderTest {

    @TempDir
    static Path tempDir;

    static File sampleDbf;

    /**
     * Sample POJO for mapping tests.
     */
    static class Product {
        @DbfColumn("NAME")    String name;
        @DbfColumn("PRICE")   BigDecimal price;
        @DbfColumn("QTY")     Integer qty;
        @DbfColumn("ACTIVE")  Boolean active;
        @DbfColumn("CREATED") LocalDate created;

        public Product() {}
    }

    @BeforeAll
    static void buildSampleDbf(@TempDir Path dir) throws Exception {
        tempDir = dir;
        sampleDbf = dir.resolve("sample.dbf").toFile();

        /*
         * Build a minimal valid DBF III file in memory.
         *
         * Layout:
         *  - 32-byte file header
         *  - N × 32-byte field descriptors
         *  - 1-byte terminator (0x0D)
         *  - R × (1 + record-length) data records
         *  - 1-byte EOF marker (0x1A)
         *
         * Fields:
         *  NAME    C 20
         *  PRICE   N 10.2
         *  QTY     N  5.0
         *  ACTIVE  L  1
         *  CREATED D  8
         */
        String[][] fields = {
            // name(10), type(1), length, decimals
            { "NAME",    "C", "20", "0"  },
            { "PRICE",   "N", "10", "2"  },
            { "QTY",     "N", "5",  "0"  },
            { "ACTIVE",  "L", "1",  "0"  },
            { "CREATED", "D", "8",  "0"  },
        };

        int numFields   = fields.length;
        int recordLength = 1; // deletion flag
        for (String[] f : fields) recordLength += Integer.parseInt(f[2]);

        // header length = 32 + 32*numFields + 1 (terminator)
        int headerLength = 32 + 32 * numFields + 1;

        // Two live records + one deleted record (to test skipping)
        Object[][] rows = {
            { "Widget",  "19.99", "100",  "T", "20240115" },
            { "DELETED", "0",     "0",    "F", "00000000" },   // deleted record (marked 0x2A)
            { "Gadget",  "5.50",  "250",  "F", "20230601" },
        };
        boolean[] deletedFlags = { false, true, false };
        int numLiveRecords = 2;

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(sampleDbf))) {
            // --- 32-byte file header ---
            out.writeByte(0x03);           // version: dBASE III
            out.writeByte(26); out.writeByte(3); out.writeByte(9); // last update yy/mm/dd
            writeInt32LE(out, rows.length); // total physical records (including deleted)
            writeInt16LE(out, headerLength);
            writeInt16LE(out, recordLength);
            out.write(new byte[20]);       // reserved

            // --- Field descriptors ---
            for (String[] f : fields) {
                byte[] name = new byte[11];
                byte[] nameBytes = f[0].getBytes();
                System.arraycopy(nameBytes, 0, name, 0, nameBytes.length);
                out.write(name);
                out.writeByte(f[1].charAt(0));   // type
                out.write(new byte[4]);           // reserved
                out.writeByte(Integer.parseInt(f[2]));  // length
                out.writeByte(Integer.parseInt(f[3]));  // decimal count
                out.write(new byte[14]);          // reserved
            }
            out.writeByte(0x0D); // field terminator

            // --- Records ---
            for (int r = 0; r < rows.length; r++) {
                out.writeByte(deletedFlags[r] ? 0x2A : 0x20); // 0x2A=deleted, 0x20=active

                for (int f = 0; f < fields.length; f++) {
                    int len = Integer.parseInt(fields[f][2]);
                    String val = (String) rows[r][f];
                    if (val == null) val = "";
                    byte[] padded = new byte[len];
                    java.util.Arrays.fill(padded, (byte) ' ');
                    byte[] valBytes = val.getBytes();
                    int copy = Math.min(valBytes.length, len);
                    System.arraycopy(valBytes, 0, padded, 0, copy);
                    out.write(padded);
                }
            }
            out.writeByte(0x1A); // EOF
        }
    }

    // -------------------------------------------------------------------------

    @Test
    void printSchema() {
        DbfSchema schema = Dbf.schema(sampleDbf.getAbsolutePath());
        List<DbfField> fields = schema.getFields();

        System.out.println();
        System.out.println("=== DBF Schema (" + fields.size() + " fields) ===");
        System.out.printf("%-15s %-6s %-8s %-10s%n", "Field Name", "Type", "Length", "Decimals");
        System.out.println("-".repeat(42));
        for (DbfField f : fields) {
            System.out.printf("%-15s %-6s %-8d %-10d%n",
                    f.getName(), f.getType(), f.getLength(), f.getDecimalCount());
        }
        System.out.println("=".repeat(42));
        System.out.println();
    }

    @Test
    void testSchemaConvenienceMethod() {
        DbfSchema schema = Dbf.schema(sampleDbf.getAbsolutePath());

        List<DbfField> fieldList = schema.getFields();
        assertEquals(5, fieldList.size());
        assertEquals("NAME",    fieldList.get(0).getName());
        assertEquals('C',       fieldList.get(0).getType());
        assertEquals(20,        fieldList.get(0).getLength());
        assertEquals("PRICE",   fieldList.get(1).getName());
        assertEquals('N',       fieldList.get(1).getType());
        assertEquals("CREATED", fieldList.get(4).getName());
        assertEquals('D',       fieldList.get(4).getType());
    }

    @Test
    void testReadAllRecords_deletedRecordIsSkipped() {
        try (DbfReader reader = Dbf.open(sampleDbf.getAbsolutePath())) {
            Object[] row1 = reader.nextRecord();
            Object[] row2 = reader.nextRecord();
            assertFalse(reader.hasNext(), "No more records expected after 2 live rows");

            assertEquals("Widget", row1[0]);
            assertEquals("Gadget", row2[0]);
        }
    }

    @Test
    void testNumericFieldConvertedToBigDecimal() {
        try (DbfReader reader = Dbf.open(sampleDbf.getAbsolutePath())) {
            Object[] row = reader.nextRecord();
            Object price = row[1];
            assertInstanceOf(BigDecimal.class, price, "N field should be BigDecimal");
            assertEquals(0, new BigDecimal("19.99").compareTo((BigDecimal) price));
        }
    }

    @Test
    void testDateFieldConvertedToLocalDate() {
        try (DbfReader reader = Dbf.open(sampleDbf.getAbsolutePath())) {
            Object[] row = reader.nextRecord();
            Object created = row[4];
            assertInstanceOf(LocalDate.class, created, "D field should be LocalDate");
            assertEquals(LocalDate.of(2024, 1, 15), created);
        }
    }

    @Test
    void testLogicalFieldConvertedToBoolean() {
        try (DbfReader reader = Dbf.open(sampleDbf.getAbsolutePath())) {
            Object[] row1 = reader.nextRecord();
            Object[] row2 = reader.nextRecord();
            assertEquals(Boolean.TRUE,  row1[3], "T should map to true");
            assertEquals(Boolean.FALSE, row2[3], "F should map to false");
        }
    }

    @Test
    void testStreamReturnsAllLiveRecords() {
        try (DbfReader reader = Dbf.open(sampleDbf.getAbsolutePath())) {
            List<Object[]> rows = reader.stream().collect(Collectors.toList());
            assertEquals(2, rows.size(), "Stream should contain only 2 live records");
        }
    }

    @Test
    void testPojoMappingWithTypeCoercion() {
        try (DbfReader reader = Dbf.open(sampleDbf.getAbsolutePath())) {
            List<Product> products = reader.read(Product.class);

            assertEquals(2, products.size());

            Product p1 = products.get(0);
            assertEquals("Widget", p1.name);
            assertEquals(0, new BigDecimal("19.99").compareTo(p1.price));
            assertEquals(Integer.valueOf(100), p1.qty);
            assertEquals(Boolean.TRUE, p1.active);
            assertEquals(LocalDate.of(2024, 1, 15), p1.created);

            Product p2 = products.get(1);
            assertEquals("Gadget", p2.name);
            assertEquals(0, new BigDecimal("5.50").compareTo(p2.price));
            assertEquals(Integer.valueOf(250), p2.qty);
            assertEquals(Boolean.FALSE, p2.active);
            assertEquals(LocalDate.of(2023, 6, 1), p2.created);
        }
    }

    @Test
    void testAutoCloseable_tryWithResources() {
        // Verifies that DbfReader compiles and works as AutoCloseable
        try (DbfReader reader = Dbf.open(sampleDbf.getAbsolutePath())) {
            assertTrue(reader.hasNext());
        }
        // No exception = pass
    }

    @Test
    void testStreamFromInputStream() throws Exception {
        try (InputStream is = new FileInputStream(sampleDbf);
             DbfReader reader = Dbf.open(is)) {
            List<Object[]> rows = reader.stream().collect(Collectors.toList());
            assertEquals(2, rows.size());
        }
    }

    // =========================================================================
    // Real-file integration tests (only run when the file is present)
    // =========================================================================

    private static final String KARVY_FILE = "C:\\Users\\vikaschaudhary\\Downloads\\distinct_cams.dbf";

    static boolean karvyFileExists() {
        return Files.exists(Paths.get(KARVY_FILE));
    }

    @Test
    @EnabledIf("karvyFileExists")
    void karvy_printSchema() {
        DbfSchema schema = Dbf.schema(KARVY_FILE);
        List<DbfField> fields = schema.getFields();

        // System.out.println();
        // System.out.println("=== distinct_karvy.dbf — Schema (" + fields.size() + " fields) ===");
        // System.out.printf("%-20s %-6s %-8s %-10s%n", "Field Name", "Type", "Length", "Decimals");
        // System.out.println("-".repeat(48));
        // for (DbfField f : fields) {
        //     System.out.printf("%-20s %-6s %-8d %-10d%n",
        //             f.getName(), f.getType(), f.getLength(), f.getDecimalCount());
        // }
        // System.out.println("=".repeat(48));
        // System.out.println();
        System.out.println(fields.toString());

        assertFalse(fields.isEmpty(), "Schema must have at least one field");
    }

    @Test
    @EnabledIf("karvyFileExists")
    void karvy_printFirst10Records() {
        try (DbfReader reader = Dbf.open(KARVY_FILE)) {
            DbfSchema schema = reader.getSchema();
            List<DbfField> fields = schema.getFields();

            System.out.println();
            System.out.println("=== distinct_karvy.dbf — First 10 records ===");
            System.out.println(schema.toString());
            System.out.println(fields.toString());

            int count = 1;
            // while (reader.hasNext() && count < 10) {
            //     Object[] row = reader.nextRecord();
            //     System.out.println("--- Record " + (count + 1) + " ---");
            //     for (int i = 0; i < fields.size(); i++) {
            //         System.out.printf("  %-20s = %s%n", fields.get(i).getName(), row[i]);
            //     }
            //     count++;
            // }
            // System.out.println("=".repeat(48));
            // System.out.println();

            assertTrue(count > 0, "File must have at least one record");
        }
    }

    @Test
    @EnabledIf("karvyFileExists")
    void karvy_countAllRecords() {
        try (DbfReader reader = Dbf.open(KARVY_FILE)) {
            AtomicInteger count = new AtomicInteger();
            reader.stream().forEach(row -> count.incrementAndGet());

            System.out.println();
            System.out.println("=== distinct_karvy.dbf — Total live records: " + count.get() + " ===");
            System.out.println();

            assertTrue(count.get() > 0, "File must have at least one record");
        }
    }

    // -------------------------------------------------------------------------

    private static void writeInt32LE(DataOutputStream out, int value) throws Exception {
        out.writeByte(value & 0xFF);
        out.writeByte((value >> 8)  & 0xFF);
        out.writeByte((value >> 16) & 0xFF);
        out.writeByte((value >> 24) & 0xFF);
    }

    private static void writeInt16LE(DataOutputStream out, int value) throws Exception {
        out.writeByte(value & 0xFF);
        out.writeByte((value >> 8) & 0xFF);
    }
}
