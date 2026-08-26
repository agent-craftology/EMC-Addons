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

/**
 * HUD card that infers dungeon Zone/Stage from nearby mob custom names.
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
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastScanMs < SCAN_INTERVAL_MS) return;
        lastScanMs = nowMs;
        scan(client);
    }

    private void scan(MinecraftClient client) {
        PlayerEntity player = client.player;
        List<Entity> entities = client.world.getEntitiesByClass(
                Entity.class,
                player.getBoundingBox().expand(SCAN_RANGE),
                entity -> entity != player);
        Map<Integer, Integer> counts = new HashMap<>();
        Map<Integer, Double> nearestDistByLevel = new HashMap<>();
        Map<Integer, MobLevelParser.Result> resultByLevel = new HashMap<>();
        for (Entity entity : entities) {
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
        if (resultByLevel.isEmpty()) {
            if (lastMatchMs != 0L && System.currentTimeMillis() - lastMatchMs >= STALE_TIMEOUT_MS) {
                zone = -1;
                stage = -1;
                level = -1;
                lastMatchMs = 0L;
            }
            return;
        }
        int bestCount = 0;
        double bestDist = Double.MAX_VALUE;
        MobLevelParser.Result best = null;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            int count = entry.getValue();
            double dist = nearestDistByLevel.get(entry.getKey());
            if (count > bestCount || (count == bestCount && dist < bestDist)) {
                bestCount = count;
                bestDist = dist;
                best = resultByLevel.get(entry.getKey());
            }
        }
        if (best == null) return;
        level = best.level;
        zone = best.zone;
        stage = best.stage;
        lastMatchMs = System.currentTimeMillis();
    }

    private void clear() {
        zone = -1;
        stage = -1;
        level = -1;
        lastScanMs = 0L;
        lastMatchMs = 0L;
    }

    private static String display(int value) {
        return value >= 1 ? String.valueOf(value) : "N/A";
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
        rows.add(new StatRow("Zone", display(zone)));
        rows.add(new StatRow("Stage", display(stage)));
        return rows;
    }

    @Override
    public List<StatRow> advancedRows() {
        List<StatRow> rows = new ArrayList<>();
        rows.add(new StatRow("Zone", display(zone)));
        rows.add(new StatRow("Stage", display(stage)));
        rows.add(new StatRow("Level", display(level)));
        return rows;
    }
}
