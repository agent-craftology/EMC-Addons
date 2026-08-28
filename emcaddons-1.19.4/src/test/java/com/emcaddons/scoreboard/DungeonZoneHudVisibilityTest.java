package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonZoneHudVisibilityTest {

    @Test
    void defaultsOnAndMissingKeysStayTrue() {
        DungeonZoneScoreboard board = new DungeonZoneScoreboard();
        assertTrue(board.isShowZoneStage());
        assertTrue(board.isShowRespawn());
        board.loadHudVisibility(new Properties());
        assertTrue(board.isShowZoneStage());
        assertTrue(board.isShowRespawn());
        board.loadHudVisibility(null);
        assertTrue(board.isShowZoneStage());
        assertTrue(board.isShowRespawn());
    }

    @Test
    void loadAndSaveHudVisibilityKeys() {
        DungeonZoneScoreboard board = new DungeonZoneScoreboard();
        Properties in = new Properties();
        in.setProperty("hud.dungeonzone.showZoneStage", "false");
        in.setProperty("hud.dungeonzone.showRespawn", "false");
        board.loadHudVisibility(in);
        assertFalse(board.isShowZoneStage());
        assertFalse(board.isShowRespawn());

        Properties out = new Properties();
        board.saveHudVisibility(out);
        assertEquals("false", out.getProperty("hud.dungeonzone.showZoneStage"));
        assertEquals("false", out.getProperty("hud.dungeonzone.showRespawn"));
    }

    @Test
    void rowsHonorTogglesAndLevelStaysAdvancedOnly() {
        DungeonZoneScoreboard board = new DungeonZoneScoreboard();
        board.setShowZoneStage(false);
        assertEquals(List.of("Respawn"), labels(board.basicRows()));
        assertEquals(List.of("Respawn", "Level"), labels(board.advancedRows()));

        board.setShowZoneStage(true);
        board.setShowRespawn(false);
        assertEquals(List.of("Zone", "Stage"), labels(board.basicRows()));
        assertEquals(List.of("Zone", "Stage", "Level"), labels(board.advancedRows()));
    }

    private static List<String> labels(List<StatRow> rows) {
        return rows.stream().map(row -> row.label).toList();
    }
}
