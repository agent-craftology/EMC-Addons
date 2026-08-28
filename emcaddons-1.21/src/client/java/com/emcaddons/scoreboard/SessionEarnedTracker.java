package com.emcaddons.scoreboard;

import java.util.function.LongSupplier;

/**
 * Session-earned accumulator with a 1s sample throttle and one-sample lag.
 * Each accepted observation is held as {@code pending} until the next accepted
 * sample confirms it did not drop; confirmed increases are added to {@code earned}.
 */
public final class SessionEarnedTracker {
    private static final long BASELINE_WARMUP_MS = 2000L;

    private double accepted;
    private double pending;
    private double earned;
    private boolean hasBaseline;
    private long lastSampleMs;
    private long firstSeenMs;

    /** Production default; tests replace this to jump warmup and sample gaps. */
    LongSupplier nowMs = System::currentTimeMillis;

    public void reset() {
        accepted = 0.0;
        pending = 0.0;
        earned = 0.0;
        hasBaseline = false;
        lastSampleMs = 0L;
        firstSeenMs = 0L;
    }

    /**
     * Record an observed wallet/scoreboard balance. The first observation
     * starts a warmup window; baseline is taken from the reading after that
     * window. Later samples closer than 1000 ms to the last accepted sample
     * are ignored.
     */
    public void observeBalance(double newBalance) {
        if (Double.isNaN(newBalance) || Double.isInfinite(newBalance)) return;
        long now = nowMs.getAsLong();
        if (firstSeenMs == 0L) firstSeenMs = now;
        if (!hasBaseline) {
            if (now - firstSeenMs < BASELINE_WARMUP_MS) return;
            accepted = newBalance;
            pending = newBalance;
            hasBaseline = true;
            lastSampleMs = now;
            return;
        }
        // Drop to 0 is a sidebar flicker / failed parse, not a real spend.
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

    /** Confirmed session earnings; does not include the unconfirmed pending sample. */
    public double earned() {
        return earned;
    }
}
