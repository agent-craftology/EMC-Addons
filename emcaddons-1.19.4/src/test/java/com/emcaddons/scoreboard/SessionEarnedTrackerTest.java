package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        AtomicLong clock = new AtomicLong(1_000_000L);
        SessionEarnedTracker tracker = new SessionEarnedTracker(clock::get);
        tracker.observeBalance(3076.0);
        clock.addAndGet(2_000L);
        tracker.observeBalance(3076.0);
        clock.addAndGet(43_000L);
        tracker.observeBalance(3076.0);
        assertEquals(0.0, tracker.earned());
    }

    @Test
    void zeroFlickerDoesNotConfirmWalletAsEarned() {
        AtomicLong clock = new AtomicLong(1_000_000L);
        SessionEarnedTracker tracker = new SessionEarnedTracker(clock::get);
        tracker.observeBalance(3076.0);
        clock.addAndGet(2_000L);
        tracker.observeBalance(3076.0);
        clock.addAndGet(1_000L);
        tracker.observeBalance(0.0);
        clock.addAndGet(1_000L);
        tracker.observeBalance(3076.0);
        clock.addAndGet(1_000L);
        tracker.observeBalance(3076.0);
        assertEquals(0.0, tracker.earned());
    }

    @Test
    void confirmedIncreaseCountsAsSessionEarned() {
        AtomicLong clock = new AtomicLong(1_000_000L);
        SessionEarnedTracker tracker = new SessionEarnedTracker(clock::get);
        tracker.observeBalance(3076.0);
        clock.addAndGet(2_000L);
        tracker.observeBalance(3076.0);
        clock.addAndGet(1_000L);
        tracker.observeBalance(3176.0);
        clock.addAndGet(1_000L);
        tracker.observeBalance(3176.0);
        assertEquals(100.0, tracker.earned());
    }

    @Test
    void creditsPerHourUsesSessionEarnedNotWallet() {
        double sessionEarned = 100.0;
        long grindMs = 43_000L;
        double fromSession = EmcStatsScoreboard.creditsPerHour(sessionEarned, grindMs);
        double fromWallet = EmcStatsScoreboard.creditsPerHour(3076.0, grindMs);
        assertEquals(sessionEarned / (grindMs / 3_600_000.0), fromSession, 1e-6);
        assertNotEquals(fromWallet, fromSession, 1.0);
        assertTrue(fromSession < fromWallet);
        assertEquals(0.0, EmcStatsScoreboard.creditsPerHour(sessionEarned, 29_000L));
    }
}
