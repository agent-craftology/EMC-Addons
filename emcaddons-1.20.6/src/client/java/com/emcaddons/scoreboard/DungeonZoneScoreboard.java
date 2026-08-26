package com.emcaddons.scoreboard;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        return List.of(
                new StatRow("Zone", zoneText),
                new StatRow("Stage", stageText)
        );
    }

    @Override
    public List<StatRow> advancedRows() {
        return List.of(
                new StatRow("Zone", zoneText),
                new StatRow("Stage", stageText),
                new StatRow("Level", levelText)
        );
    }
}
