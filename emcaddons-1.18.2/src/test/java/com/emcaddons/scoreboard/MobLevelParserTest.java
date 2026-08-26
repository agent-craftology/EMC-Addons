package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobLevelParserTest {

    @Test
    void parsesExampleNamesAndLevels() {
        assertParsed("[RARE] LVL1 Chicken ❤232", 1, 1, 1);
        assertParsed("LVL11", 11, 2, 1);
        assertParsed("LVL17", 17, 2, 7);
        assertParsed("LVL241", 241, 25, 1);
        assertParsed("§eLVL§f1 Wolf", 1, 1, 1);
        assertParsed("lvl 10", 10, 1, 10);
        assertParsed("LVL5 Goat ❤82.04M", 5, 1, 5);
        assertParsed("LVL 5", 5, 1, 5);
        assertParsed("Lv.5", 5, 1, 5);
        assertParsed("[LVL5]", 5, 1, 5);
    }

    @Test
    void rejectsUnparseableLevels() {
        assertTrue(MobLevelParser.parse(null).isEmpty());
        assertTrue(MobLevelParser.parse("").isEmpty());
        assertTrue(MobLevelParser.parse("Chicken").isEmpty());
        assertTrue(MobLevelParser.parse("LVL0 Slime").isEmpty());
        assertTrue(MobLevelParser.parse("LVL-1").isEmpty());
        assertTrue(MobLevelParser.parse("LVL99999999999").isEmpty());
    }

    private static void assertParsed(String raw, int level, int zone, int stage) {
        Optional<MobLevelParser.Result> parsed = MobLevelParser.parse(raw);
        assertTrue(parsed.isPresent(), raw);
        assertEquals(level, parsed.get().level, raw);
        assertEquals(zone, parsed.get().zone, raw);
        assertEquals(stage, parsed.get().stage, raw);
    }
}
