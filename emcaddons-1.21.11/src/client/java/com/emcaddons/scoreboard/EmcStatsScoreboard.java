package com.emcaddons.scoreboard;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Currency HUD sourced from the EMC sidebar. Always active; visibility is
 * controlled by {@link HudLayoutManager} master + card visible flags.
 */
public final class EmcStatsScoreboard implements StatCardSource {

    public enum Currency { SOULS, ESSENCE, SHARDS, CREDITS }

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

    private final Map<HudStat, Boolean> hudStatVisible = new EnumMap<>(HudStat.class);
    private Currency graphCurrency = Currency.SOULS;

    private double currentSouls;
    private double currentEssence;
    private double currentShards;
    private double currentCredits;
    private boolean hasCredits;
    private long lastCreditsSeenMs;
    private double currentSwings;
    private int rebirthLevel = -1;

    private final SessionEarnedTracker soulsEarned = new SessionEarnedTracker();
    private final SessionEarnedTracker essenceEarned = new SessionEarnedTracker();
    private final SessionEarnedTracker shardsEarned = new SessionEarnedTracker();
    private final SessionEarnedTracker creditsEarned = new SessionEarnedTracker();
    private final SessionEarnedTracker swingsEarned = new SessionEarnedTracker();
    private final Map<String, ModeBucket> modeBuckets = new HashMap<>();
    private String titleMode = "Dungeons";
    private long grindAccumulatedMs;
    private long grindSegmentStartMs;
    private String grindTimeText = "0s";

    private String soulsPerHourText = "--";
    private String essencePerHourText = "--";
    private String shardsPerHourText = "--";
    private String creditsPerHourText = "--";
    private String swingsPerHourText = "--";
    private String sessionSoulsText = "0.00";
    private String sessionEssenceText = "0.00";
    private String sessionShardsText = "0.00";
    private String sessionCreditsText = "0.00";
    private String sessionSwingsText = "0.00";
    private String totalSwingsText = "0.00";

    private static final int SPARKLINE_CAPACITY = 60;
    private static final long SPARKLINE_INTERVAL_MS = 5000L;
    private static final long RATE_WARMUP_MS = 30_000L;
    private static final long CREDITS_STALE_MS = 3000L;
    private static final double[] EMPTY_SPARKLINE = new double[0];
    private final double[] sparklineBuffer = new double[SPARKLINE_CAPACITY];
    private int sparklineCount;
    private int sparklineHead;
    private long lastSparklineSampleMs;
    private int sparklineVersion;
    private double[] sparklineSnapshot = EMPTY_SPARKLINE;
    private int sparklineSnapshotVersion = -1;
    private StatCard.GraphQuality graphQuality = StatCard.GraphQuality.HIGH;

    public EmcStatsScoreboard() {
        for (HudStat stat : HudStat.values()) hudStatVisible.put(stat, true);
        for (String id : new String[] {"gens", "factories", "skyblock", "prisons"}) {
            modeBuckets.put(id, new ModeBucket());
        }
    }

    public boolean isHudStatVisible(HudStat stat) {
        return stat != null && hudStatVisible.getOrDefault(stat, true);
    }

    public void setHudStatVisible(HudStat stat, boolean visible) {
        if (stat == null) return;
        Boolean previous = hudStatVisible.put(stat, visible);
        if (stat == HudStat.GRAPH && (previous == null || previous != visible)) sparklineVersion++;
    }

    public Currency getGraphCurrency() {
        return graphCurrency;
    }

    public void setGraphCurrency(Currency graphCurrency) {
        Currency next = graphCurrency != null ? graphCurrency : Currency.SOULS;
        if (this.graphCurrency != next) {
            this.graphCurrency = next;
            clearSparkline();
        }
    }

    @Override
    public StatCard.GraphQuality graphQuality() {
        return graphQuality;
    }

    public StatCard.GraphQuality getGraphQuality() {
        return graphQuality;
    }

    public void setGraphQuality(StatCard.GraphQuality quality) {
        this.graphQuality = quality != null ? quality : StatCard.GraphQuality.HIGH;
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

    public void update(MinecraftClient client) {
        EmcSidebar.Snapshot snap = EmcSidebar.read(client);
        boolean inWorld = worldIdentity(client) != 0;
        boolean dungeons = inWorld && snap != null && snap.location == EmcSidebar.Location.DUNGEONS;
        if (dungeons) titleMode = "Dungeons";

        if (dungeons) {
            ingestBalances(client, snap);
            if (snap.hasRebirth) rebirthLevel = snap.rebirthLevel;
        }

        long elapsedMs = tickGrindTime(dungeons);
        double sessionSoulsMade = soulsEarned.earned();
        double sessionEssenceMade = essenceEarned.earned();
        double sessionShardsMade = shardsEarned.earned();
        double sessionCreditsMade = creditsEarned.earned();
        double sessionSwingsMade = swingsEarned.earned();
        double hours = elapsedMs / 3_600_000.0;
        boolean rateReady = elapsedMs >= RATE_WARMUP_MS && hours > 0;

        sessionSoulsText = formatMoney(sessionSoulsMade);
        sessionEssenceText = formatMoney(sessionEssenceMade);
        sessionShardsText = formatMoney(sessionShardsMade);
        sessionCreditsText = formatMoney(sessionCreditsMade);
        sessionSwingsText = formatMoney(sessionSwingsMade);
        totalSwingsText = formatMoney(currentSwings);
        if (rateReady) {
            soulsPerHourText = formatMoney(sessionSoulsMade / hours);
            essencePerHourText = formatMoney(sessionEssenceMade / hours);
            shardsPerHourText = formatMoney(sessionShardsMade / hours);
            creditsPerHourText = formatMoney(sessionCreditsMade / hours);
            swingsPerHourText = formatMoney(sessionSwingsMade / hours);
        } else {
            soulsPerHourText = "--";
            essencePerHourText = "--";
            shardsPerHourText = "--";
            creditsPerHourText = "--";
            swingsPerHourText = "--";
        }

        if (dungeons) {
            long nowMs = System.currentTimeMillis();
            if (lastSparklineSampleMs == 0 || nowMs - lastSparklineSampleMs >= SPARKLINE_INTERVAL_MS) {
                lastSparklineSampleMs = nowMs;
                pushSparklineSample(graphMade());
            }
        }
    }

    /**
     * Clears dungeon session earned, rates, sparkline, and grind time.
     * Does not run on world change; Hub time is already excluded by pause.
     */
    public void resetSession() {
        resetEarnedTrackers();
        clearSparkline();
        grindAccumulatedMs = 0L;
        grindSegmentStartMs = 0L;
        grindTimeText = "0s";
    }

    /** Placeholder bucket for Coming Soon gamemodes until they have stats. */
    public void resetModeBucket(String modeId) {
        if (modeId == null || modeId.isBlank()) return;
        modeBuckets.put(modeId, new ModeBucket());
    }

    @Override
    public String id() {
        return "emcstats";
    }

    @Override
    public String title() {
        return titleMode + " Stats";
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
        appendCurrencyRows(rows);
        return rows;
    }

    @Override
    public List<StatRow> advancedRows() {
        List<StatRow> rows = new ArrayList<>();
        appendCurrencyRows(rows);
        addIf(rows, HudStat.SWINGS, new StatRow("Total swings", totalSwingsText));
        addIf(rows, HudStat.SWINGS, new StatRow("Session swings", sessionSwingsText));
        addIf(rows, HudStat.SWINGS, new StatRow("Swings/hr", swingsPerHourText));
        addIf(rows, HudStat.REBIRTH, new StatRow("Rebirth", rebirthLevel >= 0 ? String.valueOf(rebirthLevel) : "N/A"));
        return rows;
    }

    @Override
    public double[] sparklineValues() {
        if (sparklineSnapshotVersion != sparklineVersion) {
            rebuildSparklineSnapshot();
            sparklineSnapshotVersion = sparklineVersion;
        }
        return sparklineSnapshot;
    }

    @Override
    public int sparklineVersion() {
        return sparklineVersion;
    }

    private void rebuildSparklineSnapshot() {
        if (!isHudStatVisible(HudStat.GRAPH) || sparklineCount <= 0) {
            sparklineSnapshot = EMPTY_SPARKLINE;
            return;
        }
        if (sparklineSnapshot.length != sparklineCount) sparklineSnapshot = new double[sparklineCount];
        int start = sparklineCount < SPARKLINE_CAPACITY ? 0 : sparklineHead;
        for (int i = 0; i < sparklineCount; i++) {
            sparklineSnapshot[i] = sparklineBuffer[(start + i) % SPARKLINE_CAPACITY];
        }
    }

    @Override
    public String sparklineLabel() {
        if (graphCurrency == Currency.ESSENCE) return "Essence";
        if (graphCurrency == Currency.SHARDS) return "Shards";
        if (graphCurrency == Currency.CREDITS) return "Credits";
        return "Souls";
    }

    @Override
    public String sparklineValueText() {
        return formatMoney(graphMade());
    }

    private void appendCurrencyRows(List<StatRow> rows) {
        if (isHudStatVisible(HudStat.SOULS)) {
            rows.add(new StatRow("Souls/hr", soulsPerHourText));
            rows.add(new StatRow("Session souls", sessionSoulsText));
        }
        if (isHudStatVisible(HudStat.ESSENCE)) {
            rows.add(new StatRow("Essence/hr", essencePerHourText));
            rows.add(new StatRow("Session essence", sessionEssenceText));
        }
        if (isHudStatVisible(HudStat.SHARDS)) {
            rows.add(new StatRow("Shards/hr", shardsPerHourText));
            rows.add(new StatRow("Session shards", sessionShardsText));
        }
        if (isHudStatVisible(HudStat.CREDITS)) {
            rows.add(new StatRow("Credits", hasCredits ? formatMoney(currentCredits) : "N/A"));
            rows.add(new StatRow("Credits/hr", creditsPerHourText));
            rows.add(new StatRow("Session credits", sessionCreditsText));
        }
        addIf(rows, HudStat.GRIND_TIME, new StatRow("Grind Time", grindTimeText));
    }

    private void addIf(List<StatRow> rows, HudStat stat, StatRow row) {
        if (isHudStatVisible(stat)) rows.add(row);
    }

    private void ingestBalances(MinecraftClient client, EmcSidebar.Snapshot snap) {
        if (worldIdentity(client) == 0) return;
        if (snap == null) snap = EmcSidebar.Snapshot.empty();
        if (snap.hasSouls) {
            currentSouls = snap.souls;
            soulsEarned.observeBalance(snap.souls);
        }
        if (snap.hasEssence) {
            currentEssence = snap.essence;
            essenceEarned.observeBalance(snap.essence);
        }
        if (snap.hasShards) {
            currentShards = snap.shards;
            shardsEarned.observeBalance(snap.shards);
        }
        if (snap.hasCredits) {
            currentCredits = snap.credits;
            hasCredits = true;
            lastCreditsSeenMs = System.currentTimeMillis();
            creditsEarned.observeBalance(snap.credits);
        } else if (hasCredits && lastCreditsSeenMs != 0L
                && System.currentTimeMillis() - lastCreditsSeenMs >= CREDITS_STALE_MS) {
            hasCredits = false;
        }
        if (snap.hasSwings) {
            currentSwings = snap.swings;
            swingsEarned.observeBalance(snap.swings);
        }
    }

    private long tickGrindTime(boolean running) {
        long now = System.currentTimeMillis();
        if (running) {
            if (grindSegmentStartMs == 0L) grindSegmentStartMs = now;
        } else if (grindSegmentStartMs != 0L) {
            grindAccumulatedMs += now - grindSegmentStartMs;
            grindSegmentStartMs = 0L;
        }
        long total = grindAccumulatedMs;
        if (grindSegmentStartMs != 0L) total += now - grindSegmentStartMs;
        grindTimeText = formatGrindTime(total);
        return total;
    }

    private static String formatGrindTime(long ms) {
        long totalSec = Math.max(0L, ms / 1000L);
        long h = totalSec / 3600L;
        long m = (totalSec % 3600L) / 60L;
        long s = totalSec % 60L;
        if (h > 0) return h + "h " + pad2(m) + "m " + pad2(s) + "s";
        if (m > 0) return m + "m " + pad2(s) + "s";
        return s + "s";
    }

    private static String pad2(long n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    private static int worldIdentity(MinecraftClient client) {
        return (client == null || client.world == null) ? 0 : System.identityHashCode(client.world);
    }

    private static final class ModeBucket {
        /** Placeholder until this gamemode has session stats. */
    }

    private void resetEarnedTrackers() {
        soulsEarned.reset();
        essenceEarned.reset();
        shardsEarned.reset();
        creditsEarned.reset();
        swingsEarned.reset();
        sessionSoulsText = "0.00";
        sessionEssenceText = "0.00";
        sessionShardsText = "0.00";
        sessionCreditsText = "0.00";
        sessionSwingsText = "0.00";
        soulsPerHourText = "--";
        essencePerHourText = "--";
        shardsPerHourText = "--";
        creditsPerHourText = "--";
        swingsPerHourText = "--";
    }

    private double graphMade() {
        if (graphCurrency == Currency.ESSENCE) return essenceEarned.earned();
        if (graphCurrency == Currency.SHARDS) return shardsEarned.earned();
        if (graphCurrency == Currency.CREDITS) return creditsEarned.earned();
        return soulsEarned.earned();
    }

    private void pushSparklineSample(double sessionMade) {
        if (sparklineCount > 0) {
            int lastIndex = (sparklineHead - 1 + SPARKLINE_CAPACITY) % SPARKLINE_CAPACITY;
            if (sessionMade < sparklineBuffer[lastIndex]) sessionMade = sparklineBuffer[lastIndex];
        }
        sparklineBuffer[sparklineHead] = sessionMade;
        sparklineHead = (sparklineHead + 1) % SPARKLINE_CAPACITY;
        if (sparklineCount < SPARKLINE_CAPACITY) sparklineCount++;
        sparklineVersion++;
    }

    private void clearSparkline() {
        sparklineCount = 0;
        sparklineHead = 0;
        lastSparklineSampleMs = 0;
        sparklineVersion++;
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
}
