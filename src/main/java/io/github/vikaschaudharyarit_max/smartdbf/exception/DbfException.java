package io.github.vikaschaudharyarit_max.smartdbf.exception;

public class DbfException extends RuntimeException {

    public DbfException(String message) {
        super(message);
    }

    public DbfException(String message, Throwable cause) {
        super(message, cause);
    }

}
