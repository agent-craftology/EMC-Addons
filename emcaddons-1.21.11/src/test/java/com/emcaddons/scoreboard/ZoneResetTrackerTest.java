package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneResetTrackerTest {

    @Test
    void shouldNotQueryWhenShowRespawnFalse() {
        long[] now = {1_000L};
        ZoneResetTracker tracker = new ZoneResetTracker(() -> now[0]);
        assertFalse(tracker.shouldSendQuery(true, true, true, false));
    }

    @Test
    void shouldQueryWhenShowRespawnTrueAndNoPriorQuery() {
        long[] now = {1_000L};
        ZoneResetTracker tracker = new ZoneResetTracker(() -> now[0]);
        assertTrue(tracker.shouldSendQuery(true, true, true, true));
    }

    @Test
    void otherGatesStillBlockQueryWhenShowRespawnTrue() {
        long[] now = {1_000L};
        ZoneResetTracker tracker = new ZoneResetTracker(() -> now[0]);
        assertFalse(tracker.shouldSendQuery(false, true, true, true));
        assertFalse(tracker.shouldSendQuery(true, false, true, true));
        assertFalse(tracker.shouldSendQuery(true, true, false, true));
    }
}
