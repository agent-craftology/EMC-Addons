package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SessionEarnedTrackerTest {
    @Test
    void warmupDoesNotConfirmEarnings() {
        SessionEarnedTracker tracker = new SessionEarnedTracker();
        tracker.observeBalance(1.2);
        tracker.observeBalance(1200.0);
        assertEquals(0.0, tracker.earned());
    }

    @Test
    void resetRestartsWarmupWithoutEarnings() {
        SessionEarnedTracker tracker = new SessionEarnedTracker();
        tracker.observeBalance(100.0);
        tracker.reset();
        tracker.observeBalance(99999.0);
        tracker.observeBalance(1.0);
        assertEquals(0.0, tracker.earned());
    }

    @Test
    void heldWalletIsNotSessionEarned() {
        AtomicLong now = new AtomicLong(1_000_000L);
        SessionEarnedTracker tracker = settleBaseline(now, 3076.0);
        for (int i = 0; i < 43; i++) {
            now.addAndGet(1000L);
            tracker.observeBalance(3076.0);
        }
        assertEquals(0.0, tracker.earned());
    }

    @Test
    void zeroFlickerThenRecoveryIsNotEarned() {
        AtomicLong now = new AtomicLong(1_000_000L);
        SessionEarnedTracker tracker = settleBaseline(now, 3076.0);
        now.addAndGet(1000L);
        tracker.observeBalance(0.0);
        now.addAndGet(1000L);
        tracker.observeBalance(3076.0);
        assertEquals(0.0, tracker.earned());
    }

    @Test
    void confirmedIncreaseIsSessionEarned() {
        AtomicLong now = new AtomicLong(1_000_000L);
        SessionEarnedTracker tracker = settleBaseline(now, 3076.0);
        now.addAndGet(1000L);
        tracker.observeBalance(3176.0);
        now.addAndGet(1000L);
        tracker.observeBalance(3176.0);
        assertEquals(100.0, tracker.earned());
    }

    @Test
    void creditsPerHourUsesSessionEarnedNotWallet() {
        double sessionEarned = 100.0;
        double wallet = 3076.0;
        long grindElapsedMs = 43_000L;
        double sessionRate = creditsPerHour(sessionEarned, grindElapsedMs);
        double walletRate = creditsPerHour(wallet, grindElapsedMs);
        assertEquals(sessionEarned / (grindElapsedMs / 3_600_000.0), sessionRate);
        assertNotEquals(walletRate, sessionRate);
        assertEquals(100.0 / (43_000 / 3_600_000.0), sessionRate, 1e-9);
    }

    /** Credits/hr is session earned over grind hours, never currentCredits / time. */
    static double creditsPerHour(double sessionEarned, long grindElapsedMs) {
        return sessionEarned / (grindElapsedMs / 3_600_000.0);
    }

    private static SessionEarnedTracker settleBaseline(AtomicLong now, double wallet) {
        SessionEarnedTracker tracker = new SessionEarnedTracker(now::get);
        tracker.observeBalance(wallet);
        now.addAndGet(2000L);
        tracker.observeBalance(wallet);
        return tracker;
    }
}
