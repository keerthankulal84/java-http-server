package com.keerthan.httpserver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/** Starts the real server on a random port and talks to it with the JDK's own HttpClient. */
class ServerIntegrationTest {

    @TempDir
    Path publicDir;

    private HttpServer server;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void start() throws Exception {
        Files.writeString(publicDir.resolve("index.html"), "<h1>hi</h1>");
        Files.writeString(publicDir.resolve("note.txt"), "plain");
        server = new HttpServer(new ServerConfig(0, 4, publicDir.toString(), 0, 2000));
        server.start();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void servesStaticIndexAtRoot() throws Exception {
        HttpResponse<String> r = get("/");
        assertEquals(200, r.statusCode());
        assertEquals("<h1>hi</h1>", r.body());
        assertTrue(r.headers().firstValue("content-type").orElse("").startsWith("text/html"));
    }

    @Test
    void helloEndpointReturnsJson() throws Exception {
        HttpResponse<String> r = get("/api/hello?name=Java");
        assertEquals(200, r.statusCode());
        assertEquals("{\"message\":\"Hello, Java!\"}", r.body());
    }

    @Test
    void echoEndpointReturnsBody() throws Exception {
        HttpResponse<String> r = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + "/api/echo"))
                        .POST(HttpRequest.BodyPublishers.ofString("abc")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("\"received\":\"abc\""));
    }

    @Test
    void unknownPathIs404AndWrongMethodIs405() throws Exception {
        assertEquals(404, get("/missing.html").statusCode());
        HttpResponse<String> r = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + "/api/health"))
                        .POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(405, r.statusCode());
    }

    @Test
    void pathTraversalIsBlocked() throws Exception {
        assertEquals(403, get("/%2e%2e/secret.txt").statusCode());
    }

    @Test
    void handlesManyConcurrentRequests() throws Exception {
        List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            futures.add(client.sendAsync(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + "/api/health")).build(),
                    HttpResponse.BodyHandlers.ofString()));
        }
        for (CompletableFuture<HttpResponse<String>> f : futures) {
            assertEquals(200, f.get().statusCode());
        }
        assertTrue(server.stats().total() >= 50);
    }
}
