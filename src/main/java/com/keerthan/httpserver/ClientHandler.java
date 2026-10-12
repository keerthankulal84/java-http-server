package com.keerthan.httpserver;

import com.keerthan.httpserver.http.HttpException;
import com.keerthan.httpserver.http.HttpRequest;
import com.keerthan.httpserver.http.HttpResponse;
import com.keerthan.httpserver.http.Router;
import com.keerthan.httpserver.util.RateLimiter;
import com.keerthan.httpserver.util.ServerStats;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Runs on a worker thread: reads one request from a socket, routes it, writes the response. */
final class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final Router router;
    private final RateLimiter limiter;
    private final ServerStats stats;
    private final int readTimeoutMillis;

    ClientHandler(Socket socket, Router router, RateLimiter limiter, ServerStats stats, int readTimeoutMillis) {
        this.socket = socket;
        this.router = router;
        this.limiter = limiter;
        this.stats = stats;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public void run() {
        stats.connectionOpened();
        long start = System.nanoTime();
        String ip = socket.getInetAddress().getHostAddress();
        String summary = "-";
        HttpResponse response;

        try (socket) {
            socket.setSoTimeout(readTimeoutMillis);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            try {
                HttpRequest request = HttpRequest.parse(in, ip);
                if (request == null) {
                    return; // client connected and closed without sending anything
                }
                summary = request.method() + " " + request.path();
                response = limiter.allow(ip) ? route(request) : HttpResponse.error(429, "Too many requests");
            } catch (HttpException e) {
                response = HttpResponse.error(e.status(), e.getMessage());
            } catch (SocketTimeoutException e) {
                response = HttpResponse.error(400, "Request timed out");
            }

            response.writeTo(out);
            stats.record(response.status());
            long ms = (System.nanoTime() - start) / 1_000_000;
            LOG.info(ip + " \"" + summary + "\" " + response.status() + " " + response.body().length + "B " + ms + "ms [" + Thread.currentThread().getName() + "]");
        } catch (IOException e) {
            LOG.log(Level.FINE, "Connection error from " + ip, e);
        } finally {
            stats.connectionClosed();
        }
    }

    private HttpResponse route(HttpRequest request) {
        try {
            return router.route(request);
        } catch (HttpException e) {
            return HttpResponse.error(e.status(), e.getMessage());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unhandled error for " + request.path(), e);
            return HttpResponse.error(500, "Internal server error");
        }
    }
}
