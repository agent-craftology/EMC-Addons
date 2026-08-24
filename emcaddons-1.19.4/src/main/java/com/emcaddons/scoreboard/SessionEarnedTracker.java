package com.emcaddons.scoreboard;

/**
 * Session-earned accumulator with a 1s sample throttle and one-sample lag.
 * Each accepted observation is held as {@code pending} until the next accepted
 * sample confirms it did not drop; confirmed increases are added to {@code earned}.
 */
public final class SessionEarnedTracker {
    private double accepted;
    private double pending;
    private double earned;
    private boolean hasBaseline;
    private long lastSampleMs;

    public void reset() {
        accepted = 0.0;
        pending = 0.0;
        earned = 0.0;
        hasBaseline = false;
        lastSampleMs = 0L;
    }

    /**
     * Record an observed wallet/scoreboard balance. First call only sets the
     * baseline. Later samples closer than 1000 ms to the last accepted sample
     * are ignored.
     */
    public void observeBalance(double newBalance) {
        if (Double.isNaN(newBalance) || Double.isInfinite(newBalance)) return;
        long now = System.currentTimeMillis();
        if (!hasBaseline) {
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
