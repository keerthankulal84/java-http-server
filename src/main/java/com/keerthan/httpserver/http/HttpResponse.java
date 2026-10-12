package com.keerthan.httpserver.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** A simple HTTP/1.1 response that is written to the socket with Connection: close. */
public final class HttpResponse {

    private final int status;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final byte[] body;

    private HttpResponse(int status, byte[] body, String contentType) {
        this.status = status;
        this.body = body;
        headers.put("Content-Type", contentType);
    }

    public static HttpResponse of(int status, String contentType, byte[] body) {
        return new HttpResponse(status, body, contentType);
    }

    public static HttpResponse text(int status, String text) {
        return new HttpResponse(status, text.getBytes(StandardCharsets.UTF_8), "text/plain; charset=utf-8");
    }

    public static HttpResponse json(int status, String json) {
        return new HttpResponse(status, json.getBytes(StandardCharsets.UTF_8), "application/json; charset=utf-8");
    }

    public static HttpResponse error(int status, String message) {
        return json(status, "{\"status\":" + status + ",\"error\":" + com.keerthan.httpserver.util.Json.quote(message) + "}");
    }

    public HttpResponse header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public int status() {
        return status;
    }

    public byte[] body() {
        return body;
    }

    public void writeTo(OutputStream out) throws IOException {
        StringBuilder head = new StringBuilder();
        head.append("HTTP/1.1 ").append(status).append(' ').append(reason(status)).append("\r\n");
        head.append("Date: ").append(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC))).append("\r\n");
        head.append("Server: KeerthanJavaServer/1.0\r\n");
        head.append("Content-Length: ").append(body.length).append("\r\n");
        head.append("Connection: close\r\n");
        for (Map.Entry<String, String> h : headers.entrySet()) {
            head.append(h.getKey()).append(": ").append(h.getValue()).append("\r\n");
        }
        head.append("\r\n");
        out.write(head.toString().getBytes(StandardCharsets.ISO_8859_1));
        out.write(body);
        out.flush();
    }

    static String reason(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 413 -> "Payload Too Large";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 505 -> "HTTP Version Not Supported";
            default -> "Status " + status;
        };
    }
}
