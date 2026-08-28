package com.emcaddons.scoreboard;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * HUD card for dungeon zone/stage inferred from nearby mob nameplates.
 * Always active; visibility is controlled by {@link HudLayoutManager}.
 */
public final class DungeonZoneScoreboard implements StatCardSource {
    private static final long SCAN_INTERVAL_MS = 250L;
    private static final long STALE_TIMEOUT_MS = 15_000L;
    public static final double SCAN_RANGE = 24.0;

    private final ZoneResetTracker respawnTracker = new ZoneResetTracker();
    private String zoneText = "N/A";
    private String stageText = "N/A";
    private String levelText = "N/A";
    private long lastScanMs;
    private long lastMatchMs;
    private boolean showZoneStage = true;
    private boolean showRespawn = true;

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
        long now = System.currentTimeMillis();
        if (lastScanMs != 0L && now - lastScanMs < SCAN_INTERVAL_MS) return;
        lastScanMs = now;
        scanNearby(client, now);
    }

    private void scanNearby(MinecraftClient client, long now) {
        Box box = client.player.getBoundingBox().expand(SCAN_RANGE);
        List<Entity> entities = client.world.getEntitiesByClass(
                Entity.class, box, entity -> entity != client.player);

        Map<Integer, Integer> counts = new HashMap<>();
        Map<Integer, Double> nearestDist = new HashMap<>();
        Map<Integer, MobLevelParser.Parsed> parsedByLevel = new HashMap<>();

        for (Entity entity : entities) {
            Optional<String> text = NameplateText.of(entity);
            if (text.isEmpty()) continue;
            Optional<MobLevelParser.Parsed> parsed = MobLevelParser.parse(text.get());
            if (parsed.isEmpty()) continue;
            MobLevelParser.Parsed result = parsed.get();
            double dist = entity.squaredDistanceTo(client.player);
            counts.merge(result.level, 1, Integer::sum);
            Double prevDist = nearestDist.get(result.level);
            if (prevDist == null || dist < prevDist) {
                nearestDist.put(result.level, dist);
                parsedByLevel.put(result.level, result);
            }
        }

        if (counts.isEmpty()) {
            if (lastMatchMs != 0L && now - lastMatchMs >= STALE_TIMEOUT_MS) {
                zoneText = "N/A";
                stageText = "N/A";
                levelText = "N/A";
                lastMatchMs = 0L;
            }
            return;
        }

        int bestCount = -1;
        double bestDist = Double.POSITIVE_INFINITY;
        MobLevelParser.Parsed best = null;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            int level = entry.getKey();
            int count = entry.getValue();
            double dist = nearestDist.get(level);
            if (count > bestCount || (count == bestCount && dist < bestDist)) {
                bestCount = count;
                bestDist = dist;
                best = parsedByLevel.get(level);
            }
        }
        if (best != null) {
            lastMatchMs = now;
            zoneText = String.valueOf(best.zone);
            stageText = String.valueOf(best.stage);
            levelText = String.valueOf(best.level);
        }
    }

    private void clear() {
        zoneText = "N/A";
        stageText = "N/A";
        levelText = "N/A";
        lastScanMs = 0L;
        lastMatchMs = 0L;
        respawnTracker.clear();
    }

    public boolean onGameMessage(String text) {
        return respawnTracker.onGameMessage(text);
    }

    public boolean shouldSendQuery(MinecraftClient client, boolean masterVisible, boolean zoneCardVisible) {
        if (client == null || client.world == null || client.player == null) return false;
        boolean inDungeons = EmcSidebar.read(client).location == EmcSidebar.Location.DUNGEONS;
        return respawnTracker.shouldSendQuery(inDungeons, masterVisible, zoneCardVisible, showRespawn);
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
        List<StatRow> rows = new ArrayList<>();
        if (showZoneStage) {
            rows.add(new StatRow("Zone", zoneText));
            rows.add(new StatRow("Stage", stageText));
        }
        if (showRespawn) {
            rows.add(new StatRow("Respawn", respawnTracker.remainingText()));
        }
        return rows;
    }

    @Override
    public List<StatRow> advancedRows() {
        List<StatRow> rows = new ArrayList<>(basicRows());
        rows.add(new StatRow("Level", levelText));
        return rows;
    }
}
