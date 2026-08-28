package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneResetTrackerTest {

    @Test
    void shouldSendQueryFalseWhenShowRespawnOff() {
        ZoneResetTracker tracker = new ZoneResetTracker();
        assertFalse(tracker.shouldSendQuery(true, true, true, false));
    }

    @Test
    void shouldSendQueryTrueOnFirstEnterWhenShowRespawnOn() {
        ZoneResetTracker tracker = new ZoneResetTracker();
        assertTrue(tracker.shouldSendQuery(true, true, true, true));
    }
}
