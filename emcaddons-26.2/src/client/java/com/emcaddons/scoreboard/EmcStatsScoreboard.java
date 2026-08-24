package com.emcaddons.scoreboard;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Currency HUD sourced from the EMC sidebar. Always active; visibility is
 * controlled by {@link HudLayoutManager} master + card visible flags.
 */
public final class EmcStatsScoreboard implements StatCardSource {

    public enum Currency { SOULS, ESSENCE, SHARDS, CREDITS, MONEY }

    public enum HudStat {
        SOULS("Souls", "souls"),
        ESSENCE("Essence", "essence"),
        SHARDS("Shards", "shards"),
        CREDITS("Credits", "credits"),
        MONEY("Money", "money"),
        SWINGS("Swings", "swings"),
        REBIRTH("Rebirth", "rebirth"),
        GRAPH("Graph", "graph");

        public final String label;
        public final String key;

        HudStat(String label, String key) {
            this.label = label;
            this.key = key;
        }
    }

    private final Map<HudStat, Boolean> hudStatVisible = new EnumMap<>(HudStat.class);
    private Currency graphCurrency = Currency.SOULS;

    private double currentMoney;
    private double currentSouls;
    private double currentEssence;
    private double currentShards;
    private double currentCredits;
    private double currentSwings;
    private int rebirthLevel = -1;

    private final SessionEarnedTracker moneyEarned = new SessionEarnedTracker();
    private final SessionEarnedTracker soulsEarned = new SessionEarnedTracker();
    private final SessionEarnedTracker essenceEarned = new SessionEarnedTracker();
    private final SessionEarnedTracker shardsEarned = new SessionEarnedTracker();
    private final SessionEarnedTracker creditsEarned = new SessionEarnedTracker();
    private final SessionEarnedTracker swingsEarned = new SessionEarnedTracker();
    private int lastWorldIdentity;
    private long sessionStartMs;

    private String soulsPerHourText = "--";
    private String essencePerHourText = "--";
    private String shardsPerHourText = "--";
    private String creditsPerHourText = "--";
    private String moneyPerHourText = "--";
    private String swingsPerHourText = "--";
    private String sessionSoulsText = "0.00";
    private String sessionEssenceText = "0.00";
    private String sessionShardsText = "0.00";
    private String sessionCreditsText = "0.00";
    private String sessionMoneyText = "$0.00";
    private String sessionSwingsText = "0.00";
    private String totalMoneyText = "$0.00";
    private String totalSwingsText = "0.00";

    private static final int SPARKLINE_CAPACITY = 60;
    private static final long SPARKLINE_INTERVAL_MS = 5000L;
    private static final long RATE_WARMUP_MS = 30_000L;
    private final double[] sparklineBuffer = new double[SPARKLINE_CAPACITY];
    private int sparklineCount;
    private int sparklineHead;
    private long lastSparklineSampleMs;

    public EmcStatsScoreboard() {
        for (HudStat stat : HudStat.values()) hudStatVisible.put(stat, true);
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
        Currency next = graphCurrency != null ? graphCurrency : Currency.SOULS;
        if (this.graphCurrency != next) {
            this.graphCurrency = next;
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

    public void update(Minecraft client) {
        EmcSidebar.Snapshot snap = EmcSidebar.read(client);
        ingestBalances(client, snap);
        if (snap != null && snap.hasRebirth) rebirthLevel = snap.rebirthLevel;

        long elapsedMs = sessionStartMs > 0 ? System.currentTimeMillis() - sessionStartMs : 0;
        double sessionMoney = moneyEarned.earned();
        double sessionSoulsMade = soulsEarned.earned();
        double sessionEssenceMade = essenceEarned.earned();
        double sessionShardsMade = shardsEarned.earned();
        double sessionCreditsMade = creditsEarned.earned();
        double sessionSwingsMade = swingsEarned.earned();
        double hours = elapsedMs / 3_600_000.0;
        boolean rateReady = elapsedMs >= RATE_WARMUP_MS && hours > 0;

        sessionMoneyText = "$" + formatMoney(sessionMoney);
        totalMoneyText = "$" + formatMoney(currentMoney);
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
            moneyPerHourText = "$" + formatMoney(sessionMoney / hours);
            swingsPerHourText = formatMoney(sessionSwingsMade / hours);
        } else {
            soulsPerHourText = "--";
            essencePerHourText = "--";
            shardsPerHourText = "--";
            creditsPerHourText = "--";
            moneyPerHourText = "--";
            swingsPerHourText = "--";
        }

        if (worldIdentity(client) != 0) {
            long nowMs = System.currentTimeMillis();
            if (lastSparklineSampleMs == 0 || nowMs - lastSparklineSampleMs >= SPARKLINE_INTERVAL_MS) {
                lastSparklineSampleMs = nowMs;
                pushSparklineSample(graphMade());
            }
        }
    }

    @Override
    public String id() {
        return "emcstats";
    }

    @Override
    public String title() {
        return "EMC Stats";
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
        if (!isHudStatVisible(HudStat.GRAPH) || sparklineCount <= 0) {
            return new double[0];
        }
        double[] copy = new double[sparklineCount];
        int start = sparklineCount < SPARKLINE_CAPACITY ? 0 : sparklineHead;
        for (int i = 0; i < sparklineCount; i++) {
            copy[i] = sparklineBuffer[(start + i) % SPARKLINE_CAPACITY];
        }
        return copy;
    }

    @Override
    public String sparklineLabel() {
        if (graphCurrency == Currency.ESSENCE) return "Essence";
        if (graphCurrency == Currency.SHARDS) return "Shards";
        if (graphCurrency == Currency.CREDITS) return "Credits";
        if (graphCurrency == Currency.MONEY) return "Money";
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
            rows.add(new StatRow("Credits/hr", creditsPerHourText));
            rows.add(new StatRow("Session credits", sessionCreditsText));
        }
        if (isHudStatVisible(HudStat.MONEY)) {
            rows.add(new StatRow("Money/hr", moneyPerHourText));
            rows.add(new StatRow("Session money", sessionMoneyText));
            rows.add(new StatRow("Total money", totalMoneyText));
        }
    }

    private void addIf(List<StatRow> rows, HudStat stat, StatRow row) {
        if (isHudStatVisible(stat)) rows.add(row);
    }

    private void ingestBalances(Minecraft client, EmcSidebar.Snapshot snap) {
        syncWorld(client);
        if (lastWorldIdentity == 0) return;
        if (snap == null) snap = EmcSidebar.Snapshot.empty();
        if (snap.hasMoney) {
            currentMoney = snap.money;
            moneyEarned.observeBalance(snap.money);
        }
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
            creditsEarned.observeBalance(snap.credits);
        }
        if (snap.hasSwings) {
            currentSwings = snap.swings;
            swingsEarned.observeBalance(snap.swings);
        }
    }

    private void syncWorld(Minecraft client) {
        int id = worldIdentity(client);
        if (id == lastWorldIdentity) return;
        if (lastWorldIdentity != 0) {
            resetEarnedTrackers();
            clearSparkline();
        }
        lastWorldIdentity = id;
        sessionStartMs = id == 0 ? 0 : System.currentTimeMillis();
        if (id != 0) {
            currentMoney = 0;
            currentSouls = 0;
            currentEssence = 0;
            currentShards = 0;
            currentCredits = 0;
            currentSwings = 0;
            rebirthLevel = -1;
        }
    }

    private static int worldIdentity(Minecraft client) {
        return (client == null || client.level == null) ? 0 : System.identityHashCode(client.level);
    }

    private void resetEarnedTrackers() {
        moneyEarned.reset();
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
        sessionMoneyText = "$0.00";
        soulsPerHourText = "--";
        essencePerHourText = "--";
        shardsPerHourText = "--";
        creditsPerHourText = "--";
        moneyPerHourText = "--";
        swingsPerHourText = "--";
    }

    private double graphMade() {
        if (graphCurrency == Currency.ESSENCE) return essenceEarned.earned();
        if (graphCurrency == Currency.SHARDS) return shardsEarned.earned();
        if (graphCurrency == Currency.CREDITS) return creditsEarned.earned();
        if (graphCurrency == Currency.MONEY) return moneyEarned.earned();
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
    }

    private void clearSparkline() {
        sparklineCount = 0;
        sparklineHead = 0;
        lastSparklineSampleMs = 0;
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
