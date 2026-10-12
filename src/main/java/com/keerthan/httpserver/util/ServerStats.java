package com.keerthan.httpserver.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/** Lock-free request counters shared by all worker threads. */
public final class ServerStats {

    private final Instant startedAt = Instant.now();
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong activeConnections = new AtomicLong();
    private final ConcurrentHashMap<Integer, LongAdder> byStatus = new ConcurrentHashMap<>();

    public void connectionOpened() {
        activeConnections.incrementAndGet();
    }

    public void connectionClosed() {
        activeConnections.decrementAndGet();
    }

    public void record(int status) {
        totalRequests.incrementAndGet();
        byStatus.computeIfAbsent(status, s -> new LongAdder()).increment();
    }

    public long total() {
        return totalRequests.get();
    }

    public String toJson() {
        Map<String, Object> statuses = new TreeMap<>();
        byStatus.forEach((code, count) -> statuses.put(String.valueOf(code), count.sum()));
        return Json.object(
                "uptimeSeconds", Duration.between(startedAt, Instant.now()).toSeconds(),
                "totalRequests", totalRequests.get(),
                "activeConnections", activeConnections.get(),
                "responsesByStatus", statuses);
    }
}
