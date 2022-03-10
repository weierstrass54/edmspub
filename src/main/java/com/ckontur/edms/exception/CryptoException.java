package com.ckontur.edms.exception;

public class CryptoException extends RuntimeException {
    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable t) {
        super(message, t);
    }
}
