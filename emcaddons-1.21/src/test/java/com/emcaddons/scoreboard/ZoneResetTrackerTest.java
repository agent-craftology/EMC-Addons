package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneResetTrackerTest {

    @Test
    void shouldSendQueryIsFalseWhenShowRespawnIsFalse() {
        ZoneResetTracker tracker = new ZoneResetTracker();
        long now = System.currentTimeMillis();
        assertFalse(tracker.shouldSendQuery(true, true, false, now));
    }

    @Test
    void shouldSendQueryIsTrueOnFirstQueryWhenShowRespawnIsTrue() {
        ZoneResetTracker tracker = new ZoneResetTracker();
        long now = System.currentTimeMillis();
        assertTrue(tracker.shouldSendQuery(true, true, true, now));
    }
}
