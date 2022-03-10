package com.ckontur.edms.exception;

public class InvalidDocumentException extends RuntimeException {
    public InvalidDocumentException(String message) {
        super(message);
    }

    public InvalidDocumentException(Throwable t) {
        this(t.getMessage(), t);
    }

    public InvalidDocumentException(String message, Throwable t) {
        super(message, t);
    }
}
