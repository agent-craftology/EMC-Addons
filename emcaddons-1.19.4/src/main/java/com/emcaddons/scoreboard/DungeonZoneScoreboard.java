package com.emcaddons.scoreboard;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * HUD card for dungeon zone/stage inferred from nearby mob nameplates.
 */
public final class DungeonZoneScoreboard implements StatCardSource {

    private static final long SCAN_INTERVAL_MS = 250L;
    private static final long STALE_TIMEOUT_MS = 15_000L;
    private static final double SCAN_RANGE = 24.0;

    private int zone = -1;
    private int stage = -1;
    private int level = -1;
    private long lastScanMs;
    private long lastMatchMs;
    private boolean showZoneStage = true;
    private boolean showRespawn = true;
    private final ZoneResetTracker respawnTracker = new ZoneResetTracker();

    public void update(MinecraftClient client) {
        if (client == null || client.world == null || client.player == null) {
            clear();
            return;
        }
        EmcSidebar.Snapshot snap = EmcSidebar.read(client);
        if (snap != null && snap.location == EmcSidebar.Location.HUB) {
            clear();
            return;
        }
        if (!showZoneStage) return;
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastScanMs < SCAN_INTERVAL_MS) return;
        lastScanMs = nowMs;
        scan(client, nowMs);
    }

    private void scan(MinecraftClient client, long nowMs) {
        PlayerEntity player = client.player;
        List<Entity> entities = client.world.getEntitiesByClass(
                Entity.class,
                player.getBoundingBox().expand(SCAN_RANGE),
                entity -> entity != player
        );
        Map<Integer, Integer> counts = new HashMap<>();
        Map<Integer, Double> nearestDist = new HashMap<>();
        Map<Integer, MobLevelParser.Result> byLevel = new HashMap<>();
        for (Entity entity : entities) {
            Optional<String> text = NameplateText.of(entity);
            if (text.isEmpty()) continue;
            Optional<MobLevelParser.Result> parsed = MobLevelParser.parse(text.get());
            if (parsed.isEmpty()) continue;
            MobLevelParser.Result result = parsed.get();
            double dist = player.squaredDistanceTo(entity);
            counts.merge(result.level, 1, Integer::sum);
            Double prev = nearestDist.get(result.level);
            if (prev == null || dist < prev) {
                nearestDist.put(result.level, dist);
                byLevel.put(result.level, result);
            }
        }
        if (byLevel.isEmpty()) {
            if (nowMs - lastMatchMs >= STALE_TIMEOUT_MS) {
                zone = -1;
                stage = -1;
                level = -1;
            }
            return;
        }
        int bestLevel = -1;
        int bestCount = -1;
        double bestDist = Double.MAX_VALUE;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            int lvl = entry.getKey();
            int count = entry.getValue();
            double dist = nearestDist.get(lvl);
            if (count > bestCount || (count == bestCount && dist < bestDist)) {
                bestCount = count;
                bestDist = dist;
                bestLevel = lvl;
            }
        }
        MobLevelParser.Result chosen = byLevel.get(bestLevel);
        lastMatchMs = nowMs;
        level = chosen.level;
        zone = chosen.zone;
        stage = chosen.stage;
    }

    private void clear() {
        zone = -1;
        stage = -1;
        level = -1;
        lastScanMs = 0L;
        lastMatchMs = 0L;
        respawnTracker.clear();
    }

    public boolean onGameMessage(String message) {
        return respawnTracker.onGameMessage(message);
    }

    public boolean shouldSendQuery(MinecraftClient client, boolean hudAndCardVisible) {
        if (!hudAndCardVisible || client == null || client.world == null || client.player == null) {
            return false;
        }
        EmcSidebar.Snapshot snap = EmcSidebar.read(client);
        boolean inDungeons = snap != null && snap.location == EmcSidebar.Location.DUNGEONS;
        return respawnTracker.shouldSendQuery(inDungeons, true, showRespawn);
    }

    public boolean isShowZoneStage() {
        return showZoneStage;
    }

    public void setShowZoneStage(boolean showZoneStage) {
        this.showZoneStage = showZoneStage;
    }

    public boolean isShowRespawn() {
        return showRespawn;
    }

    public void setShowRespawn(boolean showRespawn) {
        this.showRespawn = showRespawn;
    }

    public void loadHudVisibility(Properties map) {
        if (map == null) return;
        String zoneStage = map.getProperty("hud.dungeonzone.showZoneStage");
        if (zoneStage != null) showZoneStage = Boolean.parseBoolean(zoneStage);
        String respawn = map.getProperty("hud.dungeonzone.showRespawn");
        if (respawn != null) showRespawn = Boolean.parseBoolean(respawn);
    }

    public void saveHudVisibility(Properties p) {
        if (p == null) return;
        p.setProperty("hud.dungeonzone.showZoneStage", String.valueOf(showZoneStage));
        p.setProperty("hud.dungeonzone.showRespawn", String.valueOf(showRespawn));
    }

    public void markQuerySent() {
        respawnTracker.markQuerySent();
    }

    @Override
    public String id() {
        return "dungeonzone";
    }

    @Override
    public String title() {
        return "Zone";
    }

    @Override
    public GuiDraw.Icon icon() {
        return GuiDraw.Icon.BOX;
    }

    @Override
    public int accentColor() {
        return GuiTheme.ACCENT;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public boolean showIcon() {
        return false;
    }

    @Override
    public List<StatRow> basicRows() {
        List<StatRow> rows = new ArrayList<>();
        if (showZoneStage) {
            rows.add(new StatRow("Zone", formatOrNa(zone)));
            rows.add(new StatRow("Stage", formatOrNa(stage)));
        }
        if (showRespawn) {
            rows.add(new StatRow("Respawn", respawnTracker.formatRemaining()));
        }
        return rows;
    }

    @Override
    public List<StatRow> advancedRows() {
        List<StatRow> rows = new ArrayList<>();
        if (showZoneStage) {
            rows.add(new StatRow("Zone", formatOrNa(zone)));
            rows.add(new StatRow("Stage", formatOrNa(stage)));
        }
        if (showRespawn) {
            rows.add(new StatRow("Respawn", respawnTracker.formatRemaining()));
        }
        rows.add(new StatRow("Level", formatOrNa(level)));
        return rows;
    }

    private static String formatOrNa(int value) {
        return value >= 1 ? String.valueOf(value) : "N/A";
    }
}
