package io.github.vikaschaudharyarit_max.smartdbf.core;

import io.github.vikaschaudharyarit_max.smartdbf.exception.DbfException;

import java.io.*;

public class Dbf {

    private Dbf() {}

    /**
     * Opens a DBF file from a local file path.
     *
     * @param path absolute or relative path to the .dbf file on the local file system
     * @return DbfReader to read records (caller must call {@link DbfReader#close()} when done)
     * @throws DbfException if the file cannot be opened or read
     */
    public static DbfReader open(String path) {

        try {

            InputStream inputStream =
                    new BufferedInputStream(
                            new FileInputStream(new File(path))
                    );

            return new DbfReader(inputStream);

        } catch (Exception e) {
            throw new DbfException("Failed to open DBF file: " + path, e);
        }
    }

    /**
     * Opens a DBF file from an arbitrary input stream (e.g. from AWS S3, HTTP, or in-memory).
     * The stream is wrapped in a BufferedInputStream. The caller is responsible for closing
     * the returned {@link DbfReader} when done; closing the reader does not close the supplied stream.
     *
     * @param inputStream the stream containing the DBF file content (e.g. from S3 getObject())
     * @return DbfReader to read records (caller must call {@link DbfReader#close()} when done)
     */
    public static DbfReader open(InputStream inputStream) {
        InputStream in = inputStream instanceof BufferedInputStream
                ? inputStream
                : new BufferedInputStream(inputStream);
        return new DbfReader(in);
    }
}
