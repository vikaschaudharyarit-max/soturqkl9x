package io.github.vikaschaudharyarit_max.smartdbf.core;

import io.github.vikaschaudharyarit_max.smartdbf.exception.DbfException;

import java.io.*;

public class Dbf {

    private Dbf() {}

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
}
