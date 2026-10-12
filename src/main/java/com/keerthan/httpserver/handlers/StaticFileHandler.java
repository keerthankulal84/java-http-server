package com.keerthan.httpserver.handlers;

import com.keerthan.httpserver.http.Handler;
import com.keerthan.httpserver.http.HttpRequest;
import com.keerthan.httpserver.http.HttpResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serves files from a folder. Protects against path traversal ("/../secret") and keeps small files
 * in a thread-safe in-memory cache that is invalidated when the file's modified time changes.
 */
public final class StaticFileHandler implements Handler {

    private record CachedFile(byte[] data, long lastModified) {
    }

    private static final long MAX_CACHED_BYTES = 512 * 1024;
    private static final Map<String, String> MIME = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "application/javascript; charset=utf-8",
            "json", "application/json; charset=utf-8",
            "png", "image/png",
            "jpg", "image/jpeg",
            "svg", "image/svg+xml",
            "txt", "text/plain; charset=utf-8");

    private final Path root;
    private final ConcurrentHashMap<Path, CachedFile> cache = new ConcurrentHashMap<>();

    public StaticFileHandler(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public HttpResponse handle(HttpRequest request) throws IOException {
        String requested = request.path().equals("/") ? "/index.html" : request.path();
        Path file = root.resolve(requested.substring(1)).normalize();

        if (!file.startsWith(root)) {
            return HttpResponse.error(403, "Forbidden");
        }
        if (!Files.isRegularFile(file)) {
            return HttpResponse.error(404, "Not found: " + request.path());
        }

        byte[] data = read(file);
        return HttpResponse.of(200, contentType(file), data);
    }

    private byte[] read(Path file) throws IOException {
        long modified = Files.getLastModifiedTime(file).toMillis();
        CachedFile cached = cache.get(file);
        if (cached != null && cached.lastModified() == modified) {
            return cached.data();
        }
        byte[] data = Files.readAllBytes(file);
        if (data.length <= MAX_CACHED_BYTES) {
            cache.put(file, new CachedFile(data, modified));
        }
        return data;
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
        return MIME.getOrDefault(ext, "application/octet-stream");
    }
}
