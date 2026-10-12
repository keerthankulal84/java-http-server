package com.keerthan.httpserver.http;

/** Thrown while parsing or routing a request; carries the HTTP status to send back. */
public class HttpException extends RuntimeException {
    private final int status;

    public HttpException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
