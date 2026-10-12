package com.keerthan.httpserver.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An immutable, parsed HTTP/1.1 request.
 * The parser is written by hand on top of raw bytes (no framework) so the protocol is visible.
 */
public record HttpRequest(String method,
                          String path,
                          Map<String, String> query,
                          Map<String, String> headers,
                          byte[] body,
                          String clientIp) {

    private static final int MAX_LINE = 8 * 1024;
    private static final int MAX_HEADERS = 100;
    private static final int MAX_BODY = 1024 * 1024; // 1 MB

    /** Case-insensitive header lookup. */
    public String header(String name) {
        return headers.get(name.toLowerCase());
    }

    public String bodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    /**
     * Parses one request from the stream.
     *
     * @return the request, or {@code null} if the client closed the connection without sending anything
     * @throws HttpException on malformed input (400), unsupported version (505) or oversized body (413)
     */
    public static HttpRequest parse(InputStream in, String clientIp) throws IOException {
        String requestLine = readLine(in);
        if (requestLine == null || requestLine.isEmpty()) {
            return null;
        }

        String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            throw new HttpException(400, "Malformed request line");
        }
        String method = parts[0].toUpperCase();
        String target = parts[1];
        if (!parts[2].startsWith("HTTP/1.")) {
            throw new HttpException(505, "HTTP version not supported");
        }

        String path = target;
        Map<String, String> query = new LinkedHashMap<>();
        int q = target.indexOf('?');
        if (q >= 0) {
            path = target.substring(0, q);
            query = parseQuery(target.substring(q + 1));
        }
        path = decode(path);
        if (!path.startsWith("/")) {
            throw new HttpException(400, "Path must start with '/'");
        }

        Map<String, String> headers = new LinkedHashMap<>();
        String line;
        while ((line = readLine(in)) != null && !line.isEmpty()) {
            if (headers.size() >= MAX_HEADERS) {
                throw new HttpException(400, "Too many headers");
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                throw new HttpException(400, "Malformed header");
            }
            headers.put(line.substring(0, colon).trim().toLowerCase(), line.substring(colon + 1).trim());
        }

        byte[] body = new byte[0];
        String len = headers.get("content-length");
        if (len != null) {
            int length;
            try {
                length = Integer.parseInt(len);
            } catch (NumberFormatException e) {
                throw new HttpException(400, "Invalid Content-Length");
            }
            if (length < 0) {
                throw new HttpException(400, "Invalid Content-Length");
            }
            if (length > MAX_BODY) {
                throw new HttpException(413, "Request body too large");
            }
            body = in.readNBytes(length);
            if (body.length < length) {
                throw new HttpException(400, "Incomplete request body");
            }
        }
        return new HttpRequest(method, path, query, headers, body, clientIp);
    }

    static Map<String, String> parseQuery(String raw) {
        Map<String, String> result = new LinkedHashMap<>();
        if (raw.isEmpty()) {
            return result;
        }
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            result.put(decode(key), decode(value));
        }
        return result;
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new HttpException(400, "Bad URL encoding");
        }
    }

    /** Reads bytes up to CRLF (or LF). Returns null on immediate end of stream. */
    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        boolean any = false;
        while ((b = in.read()) != -1) {
            any = true;
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                buf.write(b);
            }
            if (buf.size() > MAX_LINE) {
                throw new HttpException(400, "Line too long");
            }
        }
        return any ? buf.toString(StandardCharsets.ISO_8859_1) : null;
    }
}
