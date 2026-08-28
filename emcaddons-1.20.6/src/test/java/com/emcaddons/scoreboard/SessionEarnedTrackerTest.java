package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

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
    void heldWalletAfterBaselineIsNotEarned() {
        MutableClock clock = new MutableClock();
        SessionEarnedTracker tracker = new SessionEarnedTracker(clock::now);
        settleBaseline(tracker, clock, 3076.0);
        for (int i = 0; i < 43; i++) {
            clock.advance(1000L);
            tracker.observeBalance(3076.0);
        }
        assertEquals(0.0, tracker.earned());
    }

    @Test
    void zeroFlickerThenRecoveryIsNotEarned() {
        MutableClock clock = new MutableClock();
        SessionEarnedTracker tracker = new SessionEarnedTracker(clock::now);
        settleBaseline(tracker, clock, 3076.0);
        clock.advance(1000L);
        tracker.observeBalance(0.0);
        clock.advance(1000L);
        tracker.observeBalance(3076.0);
        clock.advance(1000L);
        tracker.observeBalance(3076.0);
        assertEquals(0.0, tracker.earned());
    }

    @Test
    void confirmedIncreaseIsEarned() {
        MutableClock clock = new MutableClock();
        SessionEarnedTracker tracker = new SessionEarnedTracker(clock::now);
        settleBaseline(tracker, clock, 3076.0);
        clock.advance(1000L);
        tracker.observeBalance(3176.0);
        clock.advance(1000L);
        tracker.observeBalance(3176.0);
        assertEquals(100.0, tracker.earned());
    }

    @Test
    void creditsPerHourUsesSessionEarnedNotWallet() {
        double sessionEarned = 100.0;
        long grindElapsedMs = 43_000L;
        double sessionRate = creditsPerHour(sessionEarned, grindElapsedMs);
        double walletRate = creditsPerHour(3076.0, grindElapsedMs);
        assertEquals(sessionEarned / (grindElapsedMs / 3_600_000.0), sessionRate);
        assertNotEquals(walletRate, sessionRate);
        assertEquals(0.0, creditsPerHour(0.0, grindElapsedMs));
    }

    /** Same formula as Credits/hr: session earned / grind hours, never wallet / time. */
    static double creditsPerHour(double sessionEarned, long grindElapsedMs) {
        return sessionEarned / (grindElapsedMs / 3_600_000.0);
    }

    private static void settleBaseline(SessionEarnedTracker tracker, MutableClock clock, double balance) {
        tracker.observeBalance(balance);
        clock.advance(2000L);
        tracker.observeBalance(balance);
    }

    private static final class MutableClock {
        private long now = 1_000_000L;

        long now() {
            return now;
        }

        void advance(long ms) {
            now += ms;
        }
    }
}
