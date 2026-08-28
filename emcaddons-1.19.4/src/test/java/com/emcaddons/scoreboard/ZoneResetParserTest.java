package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneResetParserTest {

    @Test
    void parsesMinutesAndSeconds() {
        assertSeconds("Mobs will respawn in 11 minutes 37 seconds", 11 * 60 + 37);
    }

    @Test
    void parsesSingularUnits() {
        assertSeconds("Mobs will respawn in 1 minute 1 second", 61);
    }

    @Test
    void parsesMinutesOnly() {
        assertSeconds("Mobs will respawn in 11 minutes", 11 * 60);
    }

    @Test
    void parsesSecondsOnly() {
        assertSeconds("Mobs will respawn in 37 seconds", 37);
    }

    @Test
    void parsesOptionalAndBetweenUnits() {
        assertSeconds("Mobs will respawn in 11 minutes and 37 seconds", 11 * 60 + 37);
        assertSeconds("Mobs will respawn in 1 hour and 2 minutes", 3600 + 120);
        assertSeconds("Mobs will respawn in 1 hour and 37 seconds", 3600 + 37);
    }

    @Test
    void parsesOptionalHours() {
        assertSeconds("Mobs will respawn in 1 hour 2 minutes 3 seconds", 3600 + 120 + 3);
        assertSeconds("Mobs will respawn in 2 hours", 7200);
        assertSeconds("Mobs will respawn in 1 hour", 3600);
    }

    @Test
    void stripsColorCodes() {
        assertSeconds("§aMobs will respawn in §e11 minutes §637 seconds", 11 * 60 + 37);
        assertSeconds("§cMobs will respawn in 1 minute 1 second", 61);
    }

    @Test
    void matchesPrefixedText() {
        assertSeconds("[Server] Mobs will respawn in 11 minutes 37 seconds", 11 * 60 + 37);
        assertSeconds(">> Mobs will respawn in 37 seconds <<", 37);
    }

    @Test
    void rejectsUnparseableMessages() {
        assertTrue(ZoneResetParser.parse(null).isEmpty());
        assertTrue(ZoneResetParser.parse("").isEmpty());
        assertTrue(ZoneResetParser.parse("Mobs will respawn in").isEmpty());
        assertTrue(ZoneResetParser.parse("Welcome to the dungeon").isEmpty());
        assertTrue(ZoneResetParser.parse("respawn in 11 minutes").isEmpty());
    }

    private static void assertSeconds(String message, int expected) {
        OptionalInt parsed = ZoneResetParser.parse(message);
        assertTrue(parsed.isPresent(), message);
        assertEquals(expected, parsed.getAsInt(), message);
    }
}
