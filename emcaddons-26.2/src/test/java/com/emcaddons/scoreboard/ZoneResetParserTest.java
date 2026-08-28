package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneResetParserTest {

    @Test
    void minutesAndSeconds() {
        assertParsed("Mobs will respawn in 11 minutes 37 seconds", 11 * 60 + 37);
    }

    @Test
    void singularUnits() {
        assertParsed("Mobs will respawn in 1 minute 1 second", 61);
    }

    @Test
    void minutesOnly() {
        assertParsed("Mobs will respawn in 11 minutes", 11 * 60);
    }

    @Test
    void secondsOnly() {
        assertParsed("Mobs will respawn in 37 seconds", 37);
    }

    @Test
    void optionalAndBetweenUnits() {
        assertParsed("Mobs will respawn in 11 minutes and 37 seconds", 11 * 60 + 37);
        assertParsed("Mobs will respawn in 1 hour and 2 minutes", 3600 + 120);
    }

    @Test
    void optionalHours() {
        assertParsed("Mobs will respawn in 1 hour 2 minutes 3 seconds", 3600 + 120 + 3);
        assertParsed("Mobs will respawn in 2 hours", 7200);
    }

    @Test
    void stripsColorCodesAndPrefixedText() {
        assertParsed("§aMobs will respawn in §e11 minutes §f37 seconds", 11 * 60 + 37);
        assertParsed("[Dungeon] Mobs will respawn in 11 minutes 37 seconds", 11 * 60 + 37);
        assertParsed("Server: Mobs will respawn in 1 minute 1 second", 61);
    }

    @Test
    void rejectsUnparseableMessages() {
        assertTrue(ZoneResetParser.parse(null).isEmpty());
        assertTrue(ZoneResetParser.parse("").isEmpty());
        assertTrue(ZoneResetParser.parse("hello").isEmpty());
        assertTrue(ZoneResetParser.parse("Mobs will respawn in").isEmpty());
        assertTrue(ZoneResetParser.parse("11 minutes 37 seconds").isEmpty());
    }

    private static void assertParsed(String raw, long seconds) {
        var parsed = ZoneResetParser.parse(raw);
        assertTrue(parsed.isPresent(), raw);
        assertEquals(seconds, parsed.getAsLong(), raw);
    }
}
