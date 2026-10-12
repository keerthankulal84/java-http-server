package com.keerthan.httpserver.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    @Test
    void blocksAfterLimitAndResetsAfterWindow() {
        RateLimiter limiter = new RateLimiter(3, 1000);
        assertTrue(limiter.allow("a", 0));
        assertTrue(limiter.allow("a", 10));
        assertTrue(limiter.allow("a", 20));
        assertFalse(limiter.allow("a", 30));
        assertTrue(limiter.allow("a", 1500)); // new window
    }

    @Test
    void clientsAreCountedSeparately() {
        RateLimiter limiter = new RateLimiter(1, 1000);
        assertTrue(limiter.allow("a", 0));
        assertFalse(limiter.allow("a", 1));
        assertTrue(limiter.allow("b", 1));
    }

    @Test
    void zeroLimitDisablesLimiting() {
        RateLimiter limiter = new RateLimiter(0, 1000);
        for (int i = 0; i < 1000; i++) {
            assertTrue(limiter.allow("a", i));
        }
    }
}
