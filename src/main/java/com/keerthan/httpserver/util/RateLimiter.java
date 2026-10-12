package com.keerthan.httpserver.util;

import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe fixed-window rate limiter keyed by client IP. */
public final class RateLimiter {

    private static final class Window {
        long start;
        int count;

        Window(long start) {
            this.start = start;
        }
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMillis;

    public RateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    /** Returns true if this request is within the limit. A limit of 0 or less disables limiting. */
    public boolean allow(String key) {
        return allow(key, System.currentTimeMillis());
    }

    boolean allow(String key, long now) {
        if (maxRequests <= 0) {
            return true;
        }
        Window w = windows.computeIfAbsent(key, k -> new Window(now));
        synchronized (w) {
            if (now - w.start >= windowMillis) {
                w.start = now;
                w.count = 0;
            }
            w.count++;
            return w.count <= maxRequests;
        }
    }
}
