package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmcSidebarLocationTest {

    @Test
    void lobbyServerIsHub() {
        assertEquals(EmcSidebar.Location.HUB, EmcSidebar.detectLocation("LOBBY SERVER"));
    }

    @Test
    void serverHub1IsHubEvenWithDungeonCounts() {
        assertEquals(EmcSidebar.Location.HUB, EmcSidebar.detectLocation(
                "SERVER: Hub1",
                "DUNGEONS 1518",
                "GENS 363"
        ));
    }

    @Test
    void smallCapsServerHubIsHub() {
        String server = EmcSidebar.normalizeSmallCaps("ꜱᴇʀᴠᴇʀ: ʜᴜʙ1");
        assertEquals("server: hub1", server.toLowerCase());
        assertEquals(EmcSidebar.Location.HUB, EmcSidebar.detectLocation("ꜱᴇʀᴠᴇʀ: ʜᴜʙ1"));
    }

    @Test
    void dungeonCurrenciesAreDungeons() {
        assertEquals(EmcSidebar.Location.DUNGEONS, EmcSidebar.detectLocation(
                "Souls: 12.5K",
                "Essence: 800",
                "Rebirth: 3"
        ));
    }

    @Test
    void emptyIsUnknown() {
        assertEquals(EmcSidebar.Location.UNKNOWN, EmcSidebar.detectLocation());
        assertEquals(EmcSidebar.Location.UNKNOWN, EmcSidebar.detectLocation("Online players"));
    }

    @Test
    void grindTimeFormat() {
        assertEquals("45s", EmcStatsScoreboard.formatGrindTime(45_000L));
        assertEquals("12m 05s", EmcStatsScoreboard.formatGrindTime((12 * 60 + 5) * 1000L));
        assertEquals("1h 23m 45s", EmcStatsScoreboard.formatGrindTime((3600 + 23 * 60 + 45) * 1000L));
    }
}
