package com.learnloop.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginThrottleTest {

    @Test
    void blocksAfterTheConfiguredNumberOfFailures() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-26T10:00:00Z"));
        LoginThrottle throttle = new LoginThrottle(3, Duration.ofMinutes(10), clock);
        String key = "alice@example.com|127.0.0.1";

        assertFalse(throttle.isBlocked(key));
        throttle.recordFailure(key);
        throttle.recordFailure(key);
        assertFalse(throttle.isBlocked(key));
        throttle.recordFailure(key);

        assertTrue(throttle.isBlocked(key));
        assertFalse(throttle.isBlocked("other@example.com|127.0.0.1"));
    }

    @Test
    void clearsAUserAfterSuccessfulAuthentication() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-26T10:00:00Z"));
        LoginThrottle throttle = new LoginThrottle(2, Duration.ofMinutes(10), clock);
        String key = "alice@example.com|127.0.0.1";

        throttle.recordFailure(key);
        throttle.recordFailure(key);
        assertTrue(throttle.isBlocked(key));

        throttle.reset(key);
        assertFalse(throttle.isBlocked(key));
    }

    @Test
    void blockExpiresWithTheConfiguredWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-26T10:00:00Z"));
        LoginThrottle throttle = new LoginThrottle(2, Duration.ofMinutes(10), clock);
        String key = "alice@example.com|127.0.0.1";

        throttle.recordFailure(key);
        throttle.recordFailure(key);
        assertTrue(throttle.isBlocked(key));

        clock.advance(Duration.ofMinutes(10));
        assertFalse(throttle.isBlocked(key));
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
