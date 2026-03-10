package io.github.vikaschaudharyarit_max.smartdbf.core;

import io.github.vikaschaudharyarit_max.smartdbf.config.DbfConfig;
import io.github.vikaschaudharyarit_max.smartdbf.exception.DbfException;
import io.github.vikaschaudharyarit_max.smartdbf.schema.DbfSchema;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Entry point for opening and inspecting DBF files.
 *
 * <h3>Basic usage</h3>
 * <pre>{@code
 * // Read all records as raw Object[] arrays
 * try (DbfReader reader = Dbf.open("data.dbf")) {
 *     while (reader.hasNext()) {
 *         Object[] row = reader.nextRecord();
 *     }
 * }
 *
 * // Map records to a POJO
 * try (DbfReader reader = Dbf.open("data.dbf")) {
 *     List<MyRecord> records = reader.read(MyRecord.class);
 * }
 *
 * // Open from an InputStream (e.g. AWS S3)
 * try (DbfReader reader = Dbf.open(s3Stream)) {
 *     reader.stream(MyRecord.class).forEach(System.out::println);
 * }
 *
 * // Inspect schema only (no record reading)
 * DbfSchema schema = Dbf.schema("data.dbf");
 * }</pre>
 */
public final class Dbf {

    private Dbf() {}

    /**
     * Opens a DBF file from a local path using UTF-8 encoding.
     *
     * @param path absolute or relative path to the {@code .dbf} file
     * @return a {@link DbfReader} (caller must close it, or use try-with-resources)
     * @throws DbfException if the file cannot be found or read
     */
    public static DbfReader open(String path) {
        return open(path, StandardCharsets.UTF_8, new DbfConfig());
    }

    /**
     * Opens a DBF file from a local path with the specified character encoding.
     * Use this when your DBF file was produced by a system that uses a non-UTF-8 encoding,
     * such as {@code Charset.forName("CP1252")} or {@code Charset.forName("ISO-8859-1")}.
     *
     * @param path    absolute or relative path to the {@code .dbf} file
     * @param charset character encoding to use when reading string fields
     * @return a {@link DbfReader} (caller must close it, or use try-with-resources)
     * @throws DbfException if the file cannot be found or read
     */
    public static DbfReader open(String path, Charset charset) {
        return open(path, charset, new DbfConfig());
    }

    /**
     * Opens a DBF file from a local path with full configuration control.
     *
     * @param path    absolute or relative path to the {@code .dbf} file
     * @param charset character encoding to use when reading string fields
     * @param config  configuration (e.g. buffer size)
     * @return a {@link DbfReader} (caller must close it, or use try-with-resources)
     * @throws DbfException if the file cannot be found or read
     */
    public static DbfReader open(String path, Charset charset, DbfConfig config) {
        try {
            InputStream in = new BufferedInputStream(
                    new FileInputStream(new File(path)), config.getBufferSize());
            return new DbfReader(in, charset);
        } catch (FileNotFoundException e) {
            throw new DbfException("DBF file not found: " + path, e);
        } catch (Exception e) {
            throw new DbfException("Failed to open DBF file: " + path, e);
        }
    }

    /**
     * Opens a DBF file from an arbitrary {@link InputStream} using UTF-8 encoding.
     * Use this for streaming sources such as AWS S3, HTTP responses, or in-memory byte arrays.
     *
     * <p>The stream is automatically wrapped in a {@link BufferedInputStream} if it isn't already.
     *
     * @param inputStream the stream containing the DBF file content
     * @return a {@link DbfReader} (caller must close it, or use try-with-resources)
     */
    public static DbfReader open(InputStream inputStream) {
        return open(inputStream, StandardCharsets.UTF_8, new DbfConfig());
    }

    /**
     * Opens a DBF file from an arbitrary {@link InputStream} with the specified character encoding.
     *
     * @param inputStream the stream containing the DBF file content
     * @param charset     character encoding to use when reading string fields
     * @return a {@link DbfReader} (caller must close it, or use try-with-resources)
     */
    public static DbfReader open(InputStream inputStream, Charset charset) {
        return open(inputStream, charset, new DbfConfig());
    }

    /**
     * Opens a DBF file from an arbitrary {@link InputStream} with full configuration control.
     *
     * @param inputStream the stream containing the DBF file content
     * @param charset     character encoding to use when reading string fields
     * @param config      configuration (e.g. buffer size)
     * @return a {@link DbfReader} (caller must close it, or use try-with-resources)
     */
    public static DbfReader open(InputStream inputStream, Charset charset, DbfConfig config) {
        InputStream in = (inputStream instanceof BufferedInputStream)
                ? inputStream
                : new BufferedInputStream(inputStream, config.getBufferSize());
        return new DbfReader(in, charset);
    }

    /**
     * Reads and returns the {@link DbfSchema} (column names, types, lengths) of a DBF file
     * without reading any records.
     *
     * <p>This is useful for validating file structure before processing.
     *
     * <pre>{@code
     * DbfSchema schema = Dbf.schema("data.dbf");
     * schema.getFields().forEach(f -> System.out.println(f.getName() + " " + f.getType()));
     * }</pre>
     *
     * @param path absolute or relative path to the {@code .dbf} file
     * @return the schema of the DBF file
     * @throws DbfException if the file cannot be found or parsed
     */
    public static DbfSchema schema(String path) {
        try (DbfReader reader = open(path)) {
            return reader.getSchema();
        }
    }

    /**
     * Reads and returns the {@link DbfSchema} from an {@link InputStream} without reading records.
     *
     * @param inputStream the stream containing the DBF file content
     * @return the schema of the DBF file
     * @throws DbfException if the stream cannot be parsed
     */
    public static DbfSchema schema(InputStream inputStream) {
        try (DbfReader reader = open(inputStream)) {
            return reader.getSchema();
        }
    }
}
