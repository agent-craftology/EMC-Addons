package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobLevelParserTest {

    @Test
    void rareChickenLvl1IsZone1Stage1() {
        assertParsed("§7[RARE] LVL1 Chicken ❤232", 1, 1, 1);
    }

    @Test
    void lvl11IsZone2Stage1() {
        assertParsed("LVL11", 11, 2, 1);
    }

    @Test
    void lvl17IsZone2Stage7() {
        assertParsed("LVL17", 17, 2, 7);
    }

    @Test
    void lvl21And31AreNextZonesStage1() {
        assertParsed("LVL21", 21, 3, 1);
        assertParsed("LVL31", 31, 4, 1);
    }

    @Test
    void lvl240IsZone24Stage10() {
        assertParsed("LVL240", 240, 24, 10);
    }

    @Test
    void lvl241HasNoCap() {
        assertParsed("LVL241", 241, 25, 1);
    }

    @Test
    void goatNameplateAndLooserFormats() {
        assertParsed("LVL5 Goat ❤82.04M", 5, 1, 5);
        assertParsed("LVL 5", 5, 1, 5);
        assertParsed("Lv.5", 5, 1, 5);
        assertParsed("[LVL5]", 5, 1, 5);
        assertParsed("§eLVL§f1 Wolf", 1, 1, 1);
        assertParsed("lvl 10", 10, 1, 10);
    }

    @Test
    void unparseableNamesAreEmpty() {
        assertTrue(MobLevelParser.parse(null).isEmpty());
        assertTrue(MobLevelParser.parse("").isEmpty());
        assertTrue(MobLevelParser.parse("LVL0").isEmpty());
        assertTrue(MobLevelParser.parse("[RARE] Chicken ❤232").isEmpty());
        assertTrue(MobLevelParser.parse("§7§a").isEmpty());
        assertTrue(MobLevelParser.parse("Chicken").isEmpty());
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
