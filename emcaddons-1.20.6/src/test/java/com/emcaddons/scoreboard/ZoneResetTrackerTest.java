package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneResetTrackerTest {

    @Test
    void doesNotQueryWhenShowRespawnIsOff() {
        ZoneResetTracker tracker = new ZoneResetTracker();
        assertFalse(tracker.shouldSendQuery(true, true, false, 1_000L));
    }

    @Test
    void queriesWhenShowRespawnIsOnAndNeverQueried() {
        ZoneResetTracker tracker = new ZoneResetTracker();
        assertTrue(tracker.shouldSendQuery(true, true, true, 1_000L));
    }
}
