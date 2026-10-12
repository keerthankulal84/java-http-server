package com.keerthan.httpserver;

import java.nio.file.Path;

/**
 * Entry point.
 * Usage: java -cp out com.keerthan.httpserver.Main [path/to/server.properties]
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT [%4$s] %5$s%n");

        Path configFile = args.length > 0 ? Path.of(args[0]) : Path.of("server.properties");
        ServerConfig config = ServerConfig.load(configFile);

        HttpServer server = new HttpServer(config);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "shutdown"));
        server.start();
        System.out.println("Open http://localhost:" + server.getPort() + " (Ctrl+C to stop)");
    }
}
