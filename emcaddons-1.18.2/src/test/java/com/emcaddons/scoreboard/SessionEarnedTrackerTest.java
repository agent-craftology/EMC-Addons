package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        long[] now = {1_000L};
        SessionEarnedTracker tracker = new SessionEarnedTracker(() -> now[0]);
        tracker.observeBalance(3076.0);
        now[0] += 2_000L;
        tracker.observeBalance(3076.0);
        for (int i = 0; i < 43; i++) {
            now[0] += 1_000L;
            tracker.observeBalance(3076.0);
        }
        assertEquals(0.0, tracker.earned());
    }

    @Test
    void flickerToZeroDoesNotConfirmEarnings() {
        long[] now = {1_000L};
        SessionEarnedTracker tracker = new SessionEarnedTracker(() -> now[0]);
        tracker.observeBalance(3076.0);
        now[0] += 2_000L;
        tracker.observeBalance(3076.0);
        now[0] += 1_000L;
        tracker.observeBalance(0.0);
        now[0] += 1_000L;
        tracker.observeBalance(3076.0);
        assertEquals(0.0, tracker.earned());
    }

    @Test
    void confirmedIncreaseIsSessionEarned() {
        long[] now = {1_000L};
        SessionEarnedTracker tracker = new SessionEarnedTracker(() -> now[0]);
        tracker.observeBalance(3076.0);
        now[0] += 2_000L;
        tracker.observeBalance(3076.0);
        now[0] += 1_000L;
        tracker.observeBalance(3176.0);
        now[0] += 1_000L;
        tracker.observeBalance(3176.0);
        assertEquals(100.0, tracker.earned());
    }

    @Test
    void creditsPerHourUsesSessionEarnedNotWallet() {
        double sessionEarned = 100.0;
        double fromSession = creditsPerHour(sessionEarned, 43_000L);
        double fromWallet = creditsPerHour(3076.0, 43_000L);
        assertEquals(sessionEarned / (43_000 / 3_600_000.0), fromSession, 1e-9);
        assertTrue(fromSession < fromWallet);
    }

    /** Credits/hr = session earned / grind hours. Never wallet / time. */
    private static double creditsPerHour(double sessionEarned, long grindElapsedMs) {
        return sessionEarned / (grindElapsedMs / 3_600_000.0);
    }
}
