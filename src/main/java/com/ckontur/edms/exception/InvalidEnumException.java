package com.ckontur.edms.exception;

public class InvalidEnumException extends RuntimeException {

    public InvalidEnumException(String message) {
        super(message);
    }

    public InvalidEnumException(String message, Throwable t) {
        super(message, t);
    }

}
