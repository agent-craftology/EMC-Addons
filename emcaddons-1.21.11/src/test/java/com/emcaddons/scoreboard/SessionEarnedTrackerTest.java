package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SessionEarnedTrackerTest {

    private static final double EPS = 1e-9;

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
        FakeClock clock = new FakeClock();
        SessionEarnedTracker tracker = new SessionEarnedTracker(clock::get);
        lockBaseline(tracker, clock, 3076.0);
        for (int i = 0; i < 43; i++) {
            clock.advance(1000L);
            tracker.observeBalance(3076.0);
        }
        assertEquals(0.0, tracker.earned(), EPS);
    }

    @Test
    void zeroFlickerDoesNotConfirmSpendOrRecovery() {
        FakeClock clock = new FakeClock();
        SessionEarnedTracker tracker = new SessionEarnedTracker(clock::get);
        lockBaseline(tracker, clock, 3076.0);
        clock.advance(1000L);
        tracker.observeBalance(0.0);
        clock.advance(1000L);
        tracker.observeBalance(3076.0);
        clock.advance(1000L);
        tracker.observeBalance(3076.0);
        assertEquals(0.0, tracker.earned(), EPS);
    }

    @Test
    void confirmedIncreaseIsSessionEarned() {
        FakeClock clock = new FakeClock();
        SessionEarnedTracker tracker = new SessionEarnedTracker(clock::get);
        lockBaseline(tracker, clock, 3076.0);
        clock.advance(1000L);
        tracker.observeBalance(3176.0);
        clock.advance(1000L);
        tracker.observeBalance(3176.0);
        assertEquals(100.0, tracker.earned(), EPS);
    }

    @Test
    void creditsPerHourUsesSessionEarnedNotWallet() {
        double sessionEarned = 100.0;
        long grindElapsedMs = 43_000L;
        double hours = grindElapsedMs / 3_600_000.0;
        double creditsPerHour = sessionEarned / hours;
        double walletPerHour = 3076.0 / hours;
        assertEquals(100.0 / (43_000 / 3_600_000.0), creditsPerHour, EPS);
        assertNotEquals(walletPerHour, creditsPerHour, 1.0);
    }

    private static void lockBaseline(SessionEarnedTracker tracker, FakeClock clock, double balance) {
        tracker.observeBalance(balance);
        clock.advance(2000L);
        tracker.observeBalance(balance);
    }

    private static final class FakeClock {
        long now = 10_000L;

        long get() {
            return now;
        }

        void advance(long ms) {
            now += ms;
        }
    }
}
