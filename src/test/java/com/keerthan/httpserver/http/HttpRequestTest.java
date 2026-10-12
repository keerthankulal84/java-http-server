package com.keerthan.httpserver.http;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpRequestTest {

    private static HttpRequest parse(String raw) throws IOException {
        return HttpRequest.parse(new ByteArrayInputStream(raw.getBytes(StandardCharsets.ISO_8859_1)), "127.0.0.1");
    }

    @Test
    void parsesGetWithQueryAndHeaders() throws IOException {
        HttpRequest r = parse("GET /api/hello?name=Ann%20Lee&x=1 HTTP/1.1\r\nHost: localhost\r\nUser-Agent: test\r\n\r\n");
        assertEquals("GET", r.method());
        assertEquals("/api/hello", r.path());
        assertEquals("Ann Lee", r.query().get("name"));
        assertEquals("1", r.query().get("x"));
        assertEquals("localhost", r.header("HOST"));
        assertEquals(0, r.body().length);
    }

    @Test
    void parsesPostBodyUsingContentLength() throws IOException {
        HttpRequest r = parse("POST /api/echo HTTP/1.1\r\nContent-Length: 5\r\n\r\nhello");
        assertEquals("POST", r.method());
        assertEquals("hello", r.bodyAsString());
    }

    @Test
    void emptyStreamReturnsNull() throws IOException {
        assertNull(parse(""));
    }

    @Test
    void malformedRequestLineIsBadRequest() {
        HttpException e = assertThrows(HttpException.class, () -> parse("GARBAGE\r\n\r\n"));
        assertEquals(400, e.status());
    }

    @Test
    void unsupportedVersionIs505() {
        HttpException e = assertThrows(HttpException.class, () -> parse("GET / HTTP/2.0\r\n\r\n"));
        assertEquals(505, e.status());
    }

    @Test
    void oversizedBodyIs413() {
        HttpException e = assertThrows(HttpException.class,
                () -> parse("POST / HTTP/1.1\r\nContent-Length: 99999999\r\n\r\n"));
        assertEquals(413, e.status());
    }

    @Test
    void truncatedBodyIsBadRequest() {
        HttpException e = assertThrows(HttpException.class,
                () -> parse("POST / HTTP/1.1\r\nContent-Length: 10\r\n\r\nabc"));
        assertEquals(400, e.status());
    }
}
