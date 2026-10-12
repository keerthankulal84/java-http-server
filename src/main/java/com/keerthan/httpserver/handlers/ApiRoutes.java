package com.keerthan.httpserver.handlers;

import com.keerthan.httpserver.http.HttpResponse;
import com.keerthan.httpserver.http.Router;
import com.keerthan.httpserver.util.Json;
import com.keerthan.httpserver.util.ServerStats;

import java.time.Instant;

/** The JSON REST endpoints exposed by the server. */
public final class ApiRoutes {

    private ApiRoutes() {
    }

    public static void register(Router router, ServerStats stats) {
        router.get("/api/health", req -> HttpResponse.json(200, Json.object("status", "UP")));

        router.get("/api/time", req -> HttpResponse.json(200, Json.object("utc", Instant.now().toString())));

        router.get("/api/hello", req -> {
            String name = req.query().getOrDefault("name", "World");
            return HttpResponse.json(200, Json.object("message", "Hello, " + name + "!"));
        });

        router.get("/api/stats", req -> HttpResponse.json(200, stats.toJson()));

        router.post("/api/echo", req -> {
            String body = req.bodyAsString();
            return HttpResponse.json(200, Json.object(
                    "received", body,
                    "length", req.body().length,
                    "contentType", String.valueOf(req.header("content-type")),
                    "thread", Thread.currentThread().getName()));
        });
    }
}
