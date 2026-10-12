package com.keerthan.httpserver.http;

/** Handles one request. Written as a functional interface so routes can be lambdas. */
@FunctionalInterface
public interface Handler {
    HttpResponse handle(HttpRequest request) throws Exception;
}
