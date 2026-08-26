package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobLevelParserTest {

    @Test
    void rareChickenLvl1IsZone1Stage1() {
        MobLevelParser.Result r = MobLevelParser.parse("§7[RARE] LVL1 Chicken ❤232").orElseThrow();
        assertEquals(1, r.level);
        assertEquals(1, r.zone);
        assertEquals(1, r.stage);
    }

    @Test
    void lvl11IsZone2Stage1() {
        MobLevelParser.Result r = MobLevelParser.parse("LVL11").orElseThrow();
        assertEquals(11, r.level);
        assertEquals(2, r.zone);
        assertEquals(1, r.stage);
    }

    @Test
    void lvl17IsZone2Stage7() {
        MobLevelParser.Result r = MobLevelParser.parse("LVL17").orElseThrow();
        assertEquals(17, r.level);
        assertEquals(2, r.zone);
        assertEquals(7, r.stage);
    }

    @Test
    void lvl21And31AreNextZonesStage1() {
        MobLevelParser.Result r21 = MobLevelParser.parse("LVL21").orElseThrow();
        assertEquals(21, r21.level);
        assertEquals(3, r21.zone);
        assertEquals(1, r21.stage);

        MobLevelParser.Result r31 = MobLevelParser.parse("LVL31").orElseThrow();
        assertEquals(31, r31.level);
        assertEquals(4, r31.zone);
        assertEquals(1, r31.stage);
    }

    @Test
    void lvl240IsZone24Stage10() {
        MobLevelParser.Result r = MobLevelParser.parse("LVL240").orElseThrow();
        assertEquals(240, r.level);
        assertEquals(24, r.zone);
        assertEquals(10, r.stage);
    }

    @Test
    void lvl241HasNoCap() {
        MobLevelParser.Result r = MobLevelParser.parse("LVL241").orElseThrow();
        assertEquals(241, r.level);
        assertEquals(25, r.zone);
        assertEquals(1, r.stage);
    }

    @Test
    void unparseableNamesAreEmpty() {
        assertTrue(MobLevelParser.parse("LVL0").isEmpty());
        assertTrue(MobLevelParser.parse("[RARE] Chicken ❤232").isEmpty());
        assertTrue(MobLevelParser.parse("").isEmpty());
        assertTrue(MobLevelParser.parse("§7§a").isEmpty());
    }

    @Test
    void goatNameplateIsZone1Stage5() {
        MobLevelParser.Result r = MobLevelParser.parse("LVL5 Goat ❤82.04M").orElseThrow();
        assertEquals(5, r.level);
        assertEquals(1, r.zone);
        assertEquals(5, r.stage);
    }

    @Test
    void spacedAndBracketedLvlFormats() {
        MobLevelParser.Result spaced = MobLevelParser.parse("LVL 5").orElseThrow();
        assertEquals(5, spaced.level);
        assertEquals(1, spaced.zone);
        assertEquals(5, spaced.stage);

        MobLevelParser.Result dotted = MobLevelParser.parse("Lv.5").orElseThrow();
        assertEquals(5, dotted.level);
        assertEquals(1, dotted.zone);
        assertEquals(5, dotted.stage);

        MobLevelParser.Result bracketed = MobLevelParser.parse("[LVL5]").orElseThrow();
        assertEquals(5, bracketed.level);
        assertEquals(1, bracketed.zone);
        assertEquals(5, bracketed.stage);
    }
}
