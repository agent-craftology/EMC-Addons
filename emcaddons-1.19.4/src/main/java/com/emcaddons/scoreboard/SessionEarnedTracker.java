package com.emcaddons.scoreboard;

import java.util.function.LongSupplier;

/**
 * Session-earned accumulator with a 1s sample throttle and one-sample lag.
 * Each accepted observation is held as {@code pending} until the next accepted
 * sample confirms it did not drop; confirmed increases are added to {@code earned}.
 */
public final class SessionEarnedTracker {
    private static final long BASELINE_WARMUP_MS = 2000L;

    private final LongSupplier clock;
    private double accepted;
    private double pending;
    private double earned;
    private boolean hasBaseline;
    private long lastSampleMs;
    private long firstSeenMs;

    public SessionEarnedTracker() {
        this(System::currentTimeMillis);
    }

    SessionEarnedTracker(LongSupplier clock) {
        this.clock = clock != null ? clock : System::currentTimeMillis;
    }

    public void reset() {
        accepted = 0.0;
        pending = 0.0;
        earned = 0.0;
        hasBaseline = false;
        lastSampleMs = 0L;
        firstSeenMs = 0L;
    }

    /**
     * Record an observed wallet/scoreboard balance. The first settled reading
     * after a 2s warm-up becomes the baseline. Later samples closer than 1000 ms
     * to the last accepted sample are ignored.
     */
    public void observeBalance(double newBalance) {
        if (Double.isNaN(newBalance) || Double.isInfinite(newBalance)) return;
        long now = nowMs();
        if (!hasBaseline) {
            if (firstSeenMs == 0L) firstSeenMs = now;
            if (now - firstSeenMs < BASELINE_WARMUP_MS) return;
            accepted = newBalance;
            pending = newBalance;
            hasBaseline = true;
            lastSampleMs = now;
            return;
        }
        // Positive baseline + 0 is sidebar flicker / failed parse, not a real spend to empty.
        if (accepted > 0.0 && newBalance == 0.0) return;
        if (now - lastSampleMs < 1000L) return;
        lastSampleMs = now;
        if (newBalance >= pending) {
            if (pending > accepted) {
                earned += pending - accepted;
            }
            accepted = pending;
            pending = newBalance;
        } else {
            pending = newBalance;
        }
    }

    long nowMs() {
        return clock.getAsLong();
    }

    /** Confirmed session earnings; does not include the unconfirmed pending sample. */
    public double earned() {
        return earned;
    }
}
