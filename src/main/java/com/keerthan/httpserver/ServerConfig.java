package com.keerthan.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Server settings, loaded from a .properties file (falls back to defaults). */
public record ServerConfig(int port, int threads, String publicDir, int rateLimitPerMinute, int readTimeoutMillis) {

    public static ServerConfig defaults() {
        return new ServerConfig(8080, 8, "public", 120, 5000);
    }

    /** Loads config from the given file if it exists, otherwise from the bundled server.properties, otherwise defaults. */
    public static ServerConfig load(Path file) throws IOException {
        Properties p = new Properties();
        if (file != null && Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            }
        } else {
            try (InputStream in = ServerConfig.class.getResourceAsStream("/server.properties")) {
                if (in != null) {
                    p.load(in);
                }
            }
        }
        ServerConfig d = defaults();
        return new ServerConfig(
                Integer.parseInt(p.getProperty("server.port", String.valueOf(d.port))),
                Integer.parseInt(p.getProperty("server.threads", String.valueOf(d.threads))),
                p.getProperty("server.publicDir", d.publicDir),
                Integer.parseInt(p.getProperty("server.rateLimitPerMinute", String.valueOf(d.rateLimitPerMinute))),
                Integer.parseInt(p.getProperty("server.readTimeoutMillis", String.valueOf(d.readTimeoutMillis))));
    }
}
