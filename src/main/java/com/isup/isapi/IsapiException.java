package com.isup.isapi;

public class IsapiException extends RuntimeException {
    private final int statusCode;

    public IsapiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public IsapiException(String message) {
        super(message);
        this.statusCode = 0;
    }

    public int getStatusCode() { return statusCode; }
}
