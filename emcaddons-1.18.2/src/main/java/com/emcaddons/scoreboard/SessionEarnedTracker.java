package com.emcaddons.scoreboard;

import java.util.function.LongSupplier;

/**
 * Session-earned accumulator with a 1s sample throttle and one-sample lag.
 * Each accepted observation is held as {@code pending} until the next accepted
 * sample confirms it did not drop; confirmed increases are added to {@code earned}.
 */
public final class SessionEarnedTracker {
    private static final long BASELINE_WARMUP_MS = 2000L;

    LongSupplier nowMs = System::currentTimeMillis;

    private double accepted;
    private double pending;
    private double earned;
    private boolean hasBaseline;
    private long lastSampleMs;
    private long firstSeenMs;

    public SessionEarnedTracker() {
    }

    SessionEarnedTracker(LongSupplier clock) {
        this.nowMs = clock != null ? clock : System::currentTimeMillis;
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
        // A drop to 0 with a positive baseline is a missing parse / sidebar flicker, not a spend.
        if (hasBaseline && accepted > 0.0 && newBalance == 0.0) return;
        long now = nowMs.getAsLong();
        if (!hasBaseline) {
            if (firstSeenMs == 0L) firstSeenMs = now;
            if (now - firstSeenMs < BASELINE_WARMUP_MS) return;
            accepted = newBalance;
            pending = newBalance;
            hasBaseline = true;
            lastSampleMs = now;
            return;
        }
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
