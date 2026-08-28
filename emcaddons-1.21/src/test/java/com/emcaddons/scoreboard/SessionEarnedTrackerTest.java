package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionEarnedTrackerTest {
    private static final double EPS = 1e-6;

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
        AtomicLong now = new AtomicLong(1_000_000L);
        SessionEarnedTracker tracker = trackerAt(now);
        establishBaseline(tracker, now, 3076.0);
        now.addAndGet(43_000L);
        tracker.observeBalance(3076.0);
        assertEquals(0.0, tracker.earned());
    }

    @Test
    void zeroFlickerDoesNotCountAsSpendThenEarn() {
        AtomicLong now = new AtomicLong(1_000_000L);
        SessionEarnedTracker tracker = trackerAt(now);
        establishBaseline(tracker, now, 3076.0);
        now.addAndGet(1000L);
        tracker.observeBalance(0.0);
        now.addAndGet(1000L);
        tracker.observeBalance(3076.0);
        assertEquals(0.0, tracker.earned());
    }

    @Test
    void confirmedIncreaseIsEarned() {
        AtomicLong now = new AtomicLong(1_000_000L);
        SessionEarnedTracker tracker = trackerAt(now);
        establishBaseline(tracker, now, 3076.0);
        now.addAndGet(1000L);
        tracker.observeBalance(3176.0);
        now.addAndGet(1000L);
        tracker.observeBalance(3176.0);
        assertEquals(100.0, tracker.earned(), EPS);
    }

    @Test
    void creditsPerHourUsesSessionEarnedNotWallet() {
        double sessionEarned = 100.0;
        long grindMs = 43_000L;
        double sessionRate = creditsPerHour(sessionEarned, grindMs);
        double walletRate = creditsPerHour(3076.0, grindMs);
        assertEquals(sessionEarned / (grindMs / 3_600_000.0), sessionRate, EPS);
        assertTrue(walletRate > sessionRate);
        assertTrue(Math.abs(sessionRate - walletRate) > 1.0);
    }

    /** Credits/hr from session earnings only — never currentCredits / time. */
    static double creditsPerHour(double sessionCredits, long grindElapsedMs) {
        return sessionCredits / (grindElapsedMs / 3_600_000.0);
    }

    private static SessionEarnedTracker trackerAt(AtomicLong now) {
        SessionEarnedTracker tracker = new SessionEarnedTracker();
        tracker.nowMs = now::get;
        return tracker;
    }

    private static void establishBaseline(SessionEarnedTracker tracker, AtomicLong now, double wallet) {
        tracker.observeBalance(wallet);
        now.addAndGet(2000L);
        tracker.observeBalance(wallet);
        assertEquals(0.0, tracker.earned());
    }
}
