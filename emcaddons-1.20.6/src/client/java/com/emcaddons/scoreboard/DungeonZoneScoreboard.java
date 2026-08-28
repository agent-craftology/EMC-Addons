package com.emcaddons.scoreboard;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * HUD card for dungeon zone/stage inferred from nearby named mobs.
 */
public final class DungeonZoneScoreboard implements StatCardSource {
    private static final long SCAN_INTERVAL_MS = 250L;
    private static final long STALE_MS = 15_000L;
    private static final double SCAN_RANGE = 24.0;

    private String zoneText = "N/A";
    private String stageText = "N/A";
    private String levelText = "N/A";
    private long lastScanMs;
    private long lastMatchMs;
    private boolean showZoneStage = true;
    private boolean showRespawn = true;
    private final ZoneResetTracker respawnTracker = new ZoneResetTracker();

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
        showZoneStage = zoneStage == null || Boolean.parseBoolean(zoneStage);
        String respawn = map.getProperty("hud.dungeonzone.showRespawn");
        showRespawn = respawn == null || Boolean.parseBoolean(respawn);
    }

    public void saveHudVisibility(Properties p) {
        if (p == null) return;
        p.setProperty("hud.dungeonzone.showZoneStage", String.valueOf(showZoneStage));
        p.setProperty("hud.dungeonzone.showRespawn", String.valueOf(showRespawn));
    }

    public void update(MinecraftClient client) {
        if (client == null || client.world == null || client.player == null) {
            clear();
            return;
        }
        EmcSidebar.Snapshot snap = EmcSidebar.read(client);
        if (snap.location == EmcSidebar.Location.HUB) {
            clear();
            return;
        }
        if (!showZoneStage) return;
        long nowMs = System.currentTimeMillis();
        if (lastScanMs != 0L && nowMs - lastScanMs < SCAN_INTERVAL_MS) return;
        lastScanMs = nowMs;
        scan(client, nowMs);
    }

    private void scan(MinecraftClient client, long nowMs) {
        var player = client.player;
        List<Entity> nearby = client.world.getEntitiesByClass(
                Entity.class,
                player.getBoundingBox().expand(SCAN_RANGE),
                entity -> entity != player
        );

        Map<Integer, Integer> counts = new HashMap<>();
        Map<Integer, Double> nearestDistByLevel = new HashMap<>();
        Map<Integer, MobLevelParser.Result> resultByLevel = new HashMap<>();

        for (Entity entity : nearby) {
            Optional<String> text = NameplateText.of(entity);
            if (text.isEmpty()) continue;
            Optional<MobLevelParser.Result> parsed = MobLevelParser.parse(text.get());
            if (parsed.isEmpty()) continue;
            MobLevelParser.Result result = parsed.get();
            double dist = player.squaredDistanceTo(entity);
            counts.merge(result.level, 1, Integer::sum);
            Double prevDist = nearestDistByLevel.get(result.level);
            if (prevDist == null || dist < prevDist) {
                nearestDistByLevel.put(result.level, dist);
                resultByLevel.put(result.level, result);
            }
        }

        if (counts.isEmpty()) {
            expireIfStale(nowMs);
            return;
        }

        int bestCount = -1;
        double bestDist = Double.POSITIVE_INFINITY;
        MobLevelParser.Result chosen = null;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            int level = entry.getKey();
            int count = entry.getValue();
            double dist = nearestDistByLevel.get(level);
            if (count > bestCount || (count == bestCount && dist < bestDist)) {
                bestCount = count;
                bestDist = dist;
                chosen = resultByLevel.get(level);
            }
        }

        if (chosen == null) {
            expireIfStale(nowMs);
            return;
        }

        zoneText = String.valueOf(chosen.zone);
        stageText = String.valueOf(chosen.stage);
        levelText = String.valueOf(chosen.level);
        lastMatchMs = nowMs;
    }

    private void expireIfStale(long nowMs) {
        if (lastMatchMs != 0L && nowMs - lastMatchMs < STALE_MS) return;
        zoneText = "N/A";
        stageText = "N/A";
        levelText = "N/A";
        lastMatchMs = 0L;
    }

    private void clear() {
        zoneText = "N/A";
        stageText = "N/A";
        levelText = "N/A";
        lastScanMs = 0L;
        lastMatchMs = 0L;
        respawnTracker.clear();
    }

    /**
     * @return {@code true} if the matching auto-query reply should be hidden from chat
     */
    public boolean onGameMessage(String message) {
        return respawnTracker.onGameMessage(message);
    }

    public boolean shouldSendQuery(MinecraftClient client, boolean masterVisible, boolean cardVisible) {
        if (!masterVisible || !cardVisible) return false;
        if (client == null || client.world == null || client.player == null) return false;
        boolean inDungeons = EmcSidebar.read(client).location == EmcSidebar.Location.DUNGEONS;
        return respawnTracker.shouldSendQuery(inDungeons, true, showRespawn, System.currentTimeMillis());
    }

    public void markQuerySent() {
        respawnTracker.markQuerySent(System.currentTimeMillis());
    }

    private String respawnText() {
        return respawnTracker.displayText(System.currentTimeMillis());
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
        return GuiDraw.Icon.MARK;
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
        return zoneRows(false);
    }

    @Override
    public List<StatRow> advancedRows() {
        return zoneRows(true);
    }

    private List<StatRow> zoneRows(boolean advanced) {
        List<StatRow> rows = new ArrayList<>();
        if (showZoneStage) {
            rows.add(new StatRow("Zone", zoneText));
            rows.add(new StatRow("Stage", stageText));
        }
        if (showRespawn) {
            rows.add(new StatRow("Respawn", respawnText()));
        }
        if (advanced) {
            rows.add(new StatRow("Level", levelText));
        }
        return rows;
    }
}
