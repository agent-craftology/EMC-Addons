package com.emcaddons.scoreboard;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * In-world EMC sidebar HUD: balances, session earned, rates, and optional sparkline.
 */
public final class EmcStatsScoreboard implements StatCardSource {

    public enum Currency { SOULS, ESSENCE, SHARDS }

    public enum HudStat {
        SOULS("Souls", "souls"),
        ESSENCE("Essence", "essence"),
        SHARDS("Shards", "shards"),
        CREDITS("Credits", "credits"),
        SWINGS("Swings", "swings"),
        REBIRTH("Rebirth", "rebirth"),
        GRAPH("Graph", "graph"),
        GRIND_TIME("Grind Time", "grind_time");

        public final String label;
        public final String key;

        HudStat(String label, String key) {
            this.label = label;
            this.key = key;
        }
    }

    public enum GameMode {
        DUNGEONS("Dungeons"),
        GENS("Gens"),
        FACTORIES("Factories"),
        SKYBLOCK("Skyblock"),
        PRISONS("Prisons");

        public final String displayName;

        GameMode(String displayName) {
            this.displayName = displayName;
        }
    }

    private static final int SPARKLINE_CAPACITY = 60;
    private static final long SPARKLINE_INTERVAL_MS = 5_000L;
    private static final long RATE_WARMUP_MS = 30_000L;

    private long sessionStartMs;
    private boolean sessionActive;
    private double currentSouls;
    private double currentEssence;
    private double currentShards;
    private double currentCredits;
    private boolean hasCredits;
    private double currentSwings;
    private final SessionEarnedTracker soulsEarned = new SessionEarnedTracker();
    private final SessionEarnedTracker essenceEarned = new SessionEarnedTracker();
    private final SessionEarnedTracker shardsEarned = new SessionEarnedTracker();
    private final SessionEarnedTracker swingsEarned = new SessionEarnedTracker();
    private int lastWorldIdentity;
    private final Map<HudStat, Boolean> hudStatVisible = new EnumMap<>(HudStat.class);
    private Currency graphCurrency = Currency.SOULS;
    private final double[] sparklineBuffer = new double[SPARKLINE_CAPACITY];
    private int sparklineCount;
    private int sparklineHead;
    private long lastSparklineSampleMs;
    private int rebirthLevel = -1;

    private double cachedSessionSouls;
    private double cachedSoulsPerHour;
    private double cachedSessionEssence;
    private double cachedEssencePerHour;
    private double cachedSessionShards;
    private double cachedShardsPerHour;
    private double cachedSessionSwings;
    private double cachedSwingsPerHour;
    private long cachedActiveMs;
    private long grindAccumulatedMs;
    private long grindResumeMs;
    private boolean grindRunning;
    private final EnumMap<GameMode, ModeBucket> modeBuckets = new EnumMap<>(GameMode.class);
    private GameMode lastMode = GameMode.DUNGEONS;

    public EmcStatsScoreboard() {
        for (HudStat stat : HudStat.values()) hudStatVisible.put(stat, true);
        for (GameMode mode : GameMode.values()) {
            if (mode != GameMode.DUNGEONS) modeBuckets.put(mode, new ModeBucket());
        }
    }

    public boolean isHudStatVisible(HudStat stat) {
        return stat != null && hudStatVisible.getOrDefault(stat, true);
    }

    public void setHudStatVisible(HudStat stat, boolean visible) {
        if (stat != null) hudStatVisible.put(stat, visible);
    }

    public Currency getGraphCurrency() {
        return graphCurrency;
    }

    public void setGraphCurrency(Currency graphCurrency) {
        if (graphCurrency == null) return;
        if (this.graphCurrency != graphCurrency) {
            this.graphCurrency = graphCurrency;
            clearSparkline();
        }
    }

    public void loadHudVisibility(Properties map) {
        if (map == null) return;
        for (HudStat stat : HudStat.values()) {
            String value = map.getProperty("hud.stat." + stat.key);
            if (value == null) {
                if (stat == HudStat.SOULS) value = map.getProperty("hud.currency.souls");
                else if (stat == HudStat.ESSENCE) value = map.getProperty("hud.currency.essence");
                else if (stat == HudStat.SHARDS) value = map.getProperty("hud.currency.shards");
            }
            if (value != null) setHudStatVisible(stat, Boolean.parseBoolean(value));
        }
    }

    public void saveHudVisibility(Properties p) {
        if (p == null) return;
        for (HudStat stat : HudStat.values()) {
            p.setProperty("hud.stat." + stat.key, String.valueOf(isHudStatVisible(stat)));
        }
        p.setProperty("hud.currency.souls", String.valueOf(isHudStatVisible(HudStat.SOULS)));
        p.setProperty("hud.currency.essence", String.valueOf(isHudStatVisible(HudStat.ESSENCE)));
        p.setProperty("hud.currency.shards", String.valueOf(isHudStatVisible(HudStat.SHARDS)));
    }

    @Override
    public double[] sparklineValues() {
        if (!isHudStatVisible(HudStat.GRAPH) || sparklineCount <= 0) return new double[0];
        double[] values = new double[sparklineCount];
        int start = sparklineCount < SPARKLINE_CAPACITY ? 0 : sparklineHead;
        for (int i = 0; i < sparklineCount; i++) {
            values[i] = sparklineBuffer[(start + i) % SPARKLINE_CAPACITY];
        }
        return values;
    }

    @Override
    public String sparklineLabel() {
        switch (graphCurrency) {
            case ESSENCE: return "Essence";
            case SHARDS: return "Shards";
            case SOULS:
            default: return "Souls";
        }
    }

    @Override
    public boolean showIcon() {
        return true;
    }

    @Override
    public String sparklineValueText() {
        return formatMoney(graphMade());
    }

    public void update(MinecraftClient client) {
        if (client == null || client.world == null) {
            pauseGrind();
            refreshCachedRates();
            return;
        }
        if (!sessionActive) startSession(client);

        EmcSidebar.Snapshot snap = EmcSidebar.read(client);
        boolean counting = snap != null && snap.countsStats();
        if (!counting) {
            pauseGrind();
            refreshCachedRates();
            return;
        }
        lastMode = GameMode.DUNGEONS;
        resumeGrind();
        ingestBalances(client, snap);
        if (snap.hasRebirth) rebirthLevel = snap.rebirthLevel;
        if (snap.hasCredits) {
            currentCredits = snap.credits;
            hasCredits = true;
        }
        refreshCachedRates();
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastSparklineSampleMs >= SPARKLINE_INTERVAL_MS) {
            pushSparklineSample(graphMade());
            lastSparklineSampleMs = nowMs;
        }
    }

    /**
     * Clears dungeon session earned, rates, sparkline, and grind time.
     * Does not run automatically on world/server changes.
     */
    public void resetSession() {
        resetSession(GameMode.DUNGEONS);
    }

    public void resetSession(GameMode mode) {
        if (mode == null) return;
        if (mode != GameMode.DUNGEONS) {
            ModeBucket bucket = modeBuckets.get(mode);
            if (bucket != null) bucket.reset();
            return;
        }
        resetEarnedTrackers();
        clearSparkline();
        grindAccumulatedMs = 0L;
        grindResumeMs = grindRunning ? System.currentTimeMillis() : 0L;
        cachedActiveMs = 0L;
        refreshCachedRates();
    }

    public static String formatGrindTime(long elapsedMs) {
        long totalSec = Math.max(0L, elapsedMs / 1000L);
        long hours = totalSec / 3600L;
        long minutes = (totalSec % 3600L) / 60L;
        long seconds = totalSec % 60L;
        if (hours > 0) {
            return hours + "h " + pad2(minutes) + "m " + pad2(seconds) + "s";
        }
        if (minutes > 0) {
            return minutes + "m " + pad2(seconds) + "s";
        }
        return seconds + "s";
    }

    private static String pad2(long value) {
        return value < 10 ? "0" + value : Long.toString(value);
    }

    private void startSession(MinecraftClient client) {
        sessionStartMs = System.currentTimeMillis();
        sessionActive = true;
        resetEarnedTrackers();
        lastWorldIdentity = worldIdentity(client);
        clearSparkline();
        grindAccumulatedMs = 0L;
        grindResumeMs = 0L;
        grindRunning = false;
    }

    private void ingestBalances(MinecraftClient client, EmcSidebar.Snapshot snap) {
        lastWorldIdentity = worldIdentity(client);
        if (snap == null) snap = EmcSidebar.Snapshot.empty();
        if (snap.hasSouls) {
            currentSouls = snap.souls;
            if (sessionActive) soulsEarned.observeBalance(snap.souls);
        }
        if (snap.hasEssence) {
            currentEssence = snap.essence;
            if (sessionActive) essenceEarned.observeBalance(snap.essence);
        }
        if (snap.hasShards) {
            currentShards = snap.shards;
            if (sessionActive) shardsEarned.observeBalance(snap.shards);
        }
        if (snap.hasSwings) {
            currentSwings = snap.swings;
            if (sessionActive) swingsEarned.observeBalance(snap.swings);
        }
    }

    private void refreshCachedRates() {
        cachedSessionSouls = soulsEarned.earned();
        cachedSessionEssence = essenceEarned.earned();
        cachedSessionShards = shardsEarned.earned();
        cachedSessionSwings = swingsEarned.earned();
        cachedActiveMs = grindElapsedMs();
        double activeHours = cachedActiveMs / 3_600_000.0;
        if (cachedActiveMs >= RATE_WARMUP_MS && activeHours > 0) {
            cachedSoulsPerHour = cachedSessionSouls / activeHours;
            cachedEssencePerHour = cachedSessionEssence / activeHours;
            cachedShardsPerHour = cachedSessionShards / activeHours;
            cachedSwingsPerHour = cachedSessionSwings / activeHours;
        } else {
            cachedSoulsPerHour = 0.0;
            cachedEssencePerHour = 0.0;
            cachedShardsPerHour = 0.0;
            cachedSwingsPerHour = 0.0;
        }
    }

    private void resumeGrind() {
        if (grindRunning) return;
        grindResumeMs = System.currentTimeMillis();
        grindRunning = true;
    }

    private void pauseGrind() {
        if (!grindRunning) return;
        grindAccumulatedMs += Math.max(0L, System.currentTimeMillis() - grindResumeMs);
        grindRunning = false;
        grindResumeMs = 0L;
    }

    private long grindElapsedMs() {
        long total = grindAccumulatedMs;
        if (grindRunning) total += Math.max(0L, System.currentTimeMillis() - grindResumeMs);
        return total;
    }

    private static int worldIdentity(MinecraftClient client) {
        return (client == null || client.world == null) ? 0 : System.identityHashCode(client.world);
    }

    private void resetEarnedTrackers() {
        soulsEarned.reset();
        essenceEarned.reset();
        shardsEarned.reset();
        swingsEarned.reset();
        cachedSessionSouls = 0.0;
        cachedSessionEssence = 0.0;
        cachedSessionShards = 0.0;
        cachedSessionSwings = 0.0;
    }

    private static String formatRate(double value, long activeMs) {
        if (activeMs < RATE_WARMUP_MS) return "--";
        return formatMoney(value);
    }

    private static String formatMoney(double money) {
        if (money >= 1_000_000_000_000.0) {
            return String.format("%.2fT", money / 1_000_000_000_000.0);
        } else if (money >= 1_000_000_000.0) {
            return String.format("%.2fB", money / 1_000_000_000.0);
        } else if (money >= 1_000_000.0) {
            return String.format("%.2fM", money / 1_000_000.0);
        } else if (money >= 1_000.0) {
            return String.format("%.2fK", money / 1_000.0);
        } else {
            return String.format("%.2f", money);
        }
    }

    @Override
    public String id() {
        return "emcstats";
    }

    @Override
    public String title() {
        return lastMode.displayName + " Stats";
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
    public List<StatRow> basicRows() {
        List<StatRow> rows = new ArrayList<>();
        appendCurrencyRows(rows);
        return rows;
    }

    @Override
    public List<StatRow> advancedRows() {
        List<StatRow> rows = new ArrayList<>();
        appendCurrencyRows(rows);
        List<StatRow> progression = new ArrayList<>();
        addIf(progression, HudStat.SWINGS, new StatRow("Total swings", formatMoney(currentSwings)));
        addIf(progression, HudStat.SWINGS, new StatRow("Session swings", formatMoney(cachedSessionSwings)));
        addIf(progression, HudStat.SWINGS, new StatRow("Swings/hr", formatRate(cachedSwingsPerHour, cachedActiveMs)));
        addIf(progression, HudStat.REBIRTH, new StatRow("Rebirth", rebirthLevel >= 0 ? String.valueOf(rebirthLevel) : "N/A"));
        addIf(progression, HudStat.CREDITS, new StatRow("Credits", hasCredits ? formatMoney(currentCredits) : "N/A"));
        appendGroup(rows, progression);
        return rows;
    }

    private static void appendGroup(List<StatRow> rows, List<StatRow> group) {
        if (group.isEmpty()) return;
        if (!rows.isEmpty()) rows.add(StatRow.separator());
        rows.addAll(group);
    }

    private void appendCurrencyRows(List<StatRow> rows) {
        addIf(rows, HudStat.GRIND_TIME, new StatRow("Grind Time", formatGrindTime(cachedActiveMs)));
        if (isHudStatVisible(HudStat.SOULS)) {
            rows.add(new StatRow("Souls/hr", formatRate(cachedSoulsPerHour, cachedActiveMs)));
            rows.add(new StatRow("Session souls", formatMoney(cachedSessionSouls)));
        }
        if (isHudStatVisible(HudStat.ESSENCE)) {
            rows.add(new StatRow("Essence/hr", formatRate(cachedEssencePerHour, cachedActiveMs)));
            rows.add(new StatRow("Session essence", formatMoney(cachedSessionEssence)));
        }
        if (isHudStatVisible(HudStat.SHARDS)) {
            rows.add(new StatRow("Shards/hr", formatRate(cachedShardsPerHour, cachedActiveMs)));
            rows.add(new StatRow("Session shards", formatMoney(cachedSessionShards)));
        }
    }

    private void addIf(List<StatRow> rows, HudStat stat, StatRow row) {
        if (isHudStatVisible(stat)) rows.add(row);
    }

    private double graphMade() {
        switch (graphCurrency) {
            case ESSENCE: return essenceEarned.earned();
            case SHARDS: return shardsEarned.earned();
            case SOULS:
            default: return soulsEarned.earned();
        }
    }

    private void pushSparklineSample(double sessionMade) {
        if (sparklineCount > 0) {
            int lastIndex = (sparklineHead - 1 + SPARKLINE_CAPACITY) % SPARKLINE_CAPACITY;
            if (sessionMade < sparklineBuffer[lastIndex]) sessionMade = sparklineBuffer[lastIndex];
        }
        sparklineBuffer[sparklineHead] = sessionMade;
        sparklineHead = (sparklineHead + 1) % SPARKLINE_CAPACITY;
        if (sparklineCount < SPARKLINE_CAPACITY) sparklineCount++;
    }

    private void clearSparkline() {
        sparklineCount = 0;
        sparklineHead = 0;
        lastSparklineSampleMs = 0;
    }

    /** Placeholder until Gens / Factories / Skyblock / Prisons have their own stats. */
    private static final class ModeBucket {
        void reset() {}
    }
}
