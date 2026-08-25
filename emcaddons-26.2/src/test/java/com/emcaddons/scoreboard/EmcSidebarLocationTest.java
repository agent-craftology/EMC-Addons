package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmcSidebarLocationTest {
    @Test
    void hubFromLobbyServerAndHubServerLine() {
        assertTrue(EmcSidebar.isHubLine("lobby server"));
        assertTrue(EmcSidebar.isHubLine("server: hub1"));
        assertTrue(EmcSidebar.isHubLine("server:hub1"));
        assertFalse(EmcSidebar.isHubLine("server: dungeons"));
        assertFalse(EmcSidebar.isHubLine("dungeons 1518"));
    }

    @Test
    void dungeonKeywordsIgnoreGameNameCounts() {
        assertTrue(EmcSidebar.hasDungeonCurrencyKeyword("souls 12.3k"));
        assertTrue(EmcSidebar.hasDungeonCurrencyKeyword("rebirth: 4"));
        assertFalse(EmcSidebar.hasDungeonCurrencyKeyword("dungeons 1518"));
        assertFalse(EmcSidebar.hasDungeonCurrencyKeyword("gens 363"));
    }

    @Test
    void classifyPrefersHubOverDungeonKeywords() {
        assertEquals(EmcSidebar.Location.HUB, EmcSidebar.classifyLocation(true, true));
        assertEquals(EmcSidebar.Location.DUNGEONS, EmcSidebar.classifyLocation(false, true));
        assertEquals(EmcSidebar.Location.UNKNOWN, EmcSidebar.classifyLocation(false, false));
    }
}
