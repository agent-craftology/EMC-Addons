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
        assertParsed("lvl 17 mob", 17, 2, 7);
        assertParsed("§cLVL§a241 Boss", 241, 25, 1);
        assertParsed("LVL5 Goat ❤82.04M", 5, 1, 5);
        assertParsed("LVL 5", 5, 1, 5);
        assertParsed("Lv.5", 5, 1, 5);
        assertParsed("[LVL5]", 5, 1, 5);
        assertParsed("LVL5", 5, 1, 5);
    }

    @Test
    void rejectsUnparseableLevels() {
        assertTrue(MobLevelParser.parse(null).isEmpty());
        assertTrue(MobLevelParser.parse("").isEmpty());
        assertTrue(MobLevelParser.parse("Chicken ❤232").isEmpty());
        assertTrue(MobLevelParser.parse("LVL0 Chicken").isEmpty());
        assertTrue(MobLevelParser.parse("LVL-1 Chicken").isEmpty());
        assertTrue(MobLevelParser.parse("LVL99999999999").isEmpty());
    }

    private static void assertParsed(String name, int level, int zone, int stage) {
        Optional<MobLevelParser.Result> parsed = MobLevelParser.parse(name);
        assertTrue(parsed.isPresent(), name);
        MobLevelParser.Result result = parsed.get();
        assertEquals(level, result.level, name);
        assertEquals(zone, result.zone, name);
        assertEquals(stage, result.stage, name);
    }
}
