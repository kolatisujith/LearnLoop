package com.learnloop.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class LoginThrottle {

    public static final int DEFAULT_MAX_FAILURES = 5;
    public static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);

    private final int maxFailures;
    private final Duration window;
    private final Clock clock;
    private final Map<String, FailureWindow> failures = new ConcurrentHashMap<>();

    public LoginThrottle() {
        this(DEFAULT_MAX_FAILURES, DEFAULT_WINDOW, Clock.systemUTC());
    }

    public LoginThrottle(int maxFailures, Duration window, Clock clock) {
        if (maxFailures < 1) {
            throw new IllegalArgumentException("Maximum failures must be at least one.");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Throttle window must be positive.");
        }
        this.maxFailures = maxFailures;
        this.window = window;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized boolean isBlocked(String key) {
        if (key == null) {
            return false;
        }
        FailureWindow entry = failures.get(key);
        if (entry == null) {
            return false;
        }
        Instant now = clock.instant();
        if (entry.blockedUntil != null && now.isBefore(entry.blockedUntil)) {
            return true;
        }
        if (entry.isExpired(now, window)) {
            failures.remove(key);
        }
        return false;
    }

    public synchronized int recordFailure(String key) {
        if (key == null) {
            return 0;
        }
        Instant now = clock.instant();
        FailureWindow entry = failures.get(key);
        if (entry == null || entry.isExpired(now, window)) {
            entry = new FailureWindow();
        }
        entry.failures++;
        entry.lastFailure = now;
        if (entry.failures >= maxFailures) {
            entry.blockedUntil = now.plus(window);
        }
        failures.put(key, entry);
        if (failures.size() > 10_000) {
            failures.entrySet().removeIf(item -> item.getValue().isExpired(now, window));
        }
        return entry.failures;
    }

    public synchronized void reset(String key) {
        if (key != null) {
            failures.remove(key);
        }
    }

    public synchronized void clear() {
        failures.clear();
    }

    private static final class FailureWindow {
        private int failures;
        private Instant lastFailure;
        private Instant blockedUntil;

        private boolean isExpired(Instant now, Duration window) {
            if (blockedUntil != null && now.isBefore(blockedUntil)) {
                return false;
            }
            return lastFailure == null || !now.isBefore(lastFailure.plus(window));
        }
    }
}
