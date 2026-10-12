package com.keerthan.httpserver;

import com.keerthan.httpserver.handlers.ApiRoutes;
import com.keerthan.httpserver.handlers.StaticFileHandler;
import com.keerthan.httpserver.http.Router;
import com.keerthan.httpserver.util.RateLimiter;
import com.keerthan.httpserver.util.ServerStats;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The server: one acceptor thread hands each accepted socket to a fixed pool of worker threads.
 */
public final class HttpServer implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(HttpServer.class.getName());

    private final ServerConfig config;
    private final ServerStats stats = new ServerStats();
    private final Router router = new Router();
    private final RateLimiter limiter;

    private ServerSocket serverSocket;
    private ExecutorService workers;
    private Thread acceptor;
    private volatile boolean running;

    public HttpServer(ServerConfig config) {
        this.config = config;
        this.limiter = new RateLimiter(config.rateLimitPerMinute(), 60_000);
        ApiRoutes.register(router, stats);
        router.fallback(new StaticFileHandler(Path.of(config.publicDir())));
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(config.port());
        workers = Executors.newFixedThreadPool(config.threads(), new ThreadFactory() {
            private final AtomicInteger n = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "worker-" + n.getAndIncrement());
            }
        });
        running = true;
        acceptor = new Thread(this::acceptLoop, "acceptor");
        acceptor.start();
        LOG.info("Server listening on port " + getPort() + " with " + config.threads() + " worker threads");
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                workers.execute(new ClientHandler(socket, router, limiter, stats, config.readTimeoutMillis()));
            } catch (RejectedExecutionException e) {
                break; // pool already shut down
            } catch (IOException e) {
                if (running) {
                    LOG.log(Level.WARNING, "Accept failed", e);
                }
            }
        }
    }

    /** The actual port (useful when configured with port 0). */
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public ServerStats stats() {
        return stats;
    }

    @Override
    public void close() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // closing anyway
        }
        workers.shutdown();
        try {
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.info("Server stopped");
    }
}
