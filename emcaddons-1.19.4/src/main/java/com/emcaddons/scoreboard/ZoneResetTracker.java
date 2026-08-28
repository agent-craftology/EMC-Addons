package com.emcaddons.scoreboard;

import java.util.OptionalInt;

/**
 * Local countdown for dungeon mob respawn, fed by hidden {@code /zone reset} queries.
 */
public final class ZoneResetTracker {

    public static final long QUERY_COOLDOWN_MS = 90_000L;
    public static final long PENDING_WINDOW_MS = 2_000L;

    private long nextRespawnAtMs;
    private long lastQueryMs;
    private long pendingUntilMs;
    private long lastParseMs;

    public boolean onGameMessage(String text) {
        return onGameMessage(text, System.currentTimeMillis());
    }

    public boolean onGameMessage(String text, long nowMs) {
        OptionalInt seconds = ZoneResetParser.parse(text);
        if (seconds.isEmpty()) return false;
        nextRespawnAtMs = nowMs + seconds.getAsInt() * 1000L;
        lastParseMs = nowMs;
        boolean hide = isPending(nowMs);
        pendingUntilMs = 0L;
        return hide;
    }

    public boolean shouldSendQuery(boolean inDungeons, boolean hudAndCardVisible, boolean showRespawn) {
        return shouldSendQuery(inDungeons, hudAndCardVisible, showRespawn, System.currentTimeMillis());
    }

    public boolean shouldSendQuery(boolean inDungeons, boolean hudAndCardVisible, boolean showRespawn, long nowMs) {
        if (!showRespawn || !inDungeons || !hudAndCardVisible) return false;
        expirePending(nowMs);
        if (isPending(nowMs)) return false;

        boolean firstEnter = lastQueryMs == 0L;
        boolean remainingElapsed = nextRespawnAtMs > 0L && nowMs >= nextRespawnAtMs;
        boolean parseStale = lastParseMs > 0L && nowMs - lastParseMs >= QUERY_COOLDOWN_MS;
        boolean neverParsedRetry = lastParseMs == 0L && lastQueryMs > 0L
                && nowMs - lastQueryMs >= QUERY_COOLDOWN_MS;

        if (!(firstEnter || remainingElapsed || parseStale || neverParsedRetry)) {
            return false;
        }
        if (lastQueryMs > 0L && nowMs - lastQueryMs < QUERY_COOLDOWN_MS) {
            return remainingElapsed && lastParseMs >= lastQueryMs;
        }
        return true;
    }

    public void markQuerySent() {
        markQuerySent(System.currentTimeMillis());
    }

    public void markQuerySent(long nowMs) {
        lastQueryMs = nowMs;
        pendingUntilMs = nowMs + PENDING_WINDOW_MS;
    }

    public void clear() {
        nextRespawnAtMs = 0L;
        lastQueryMs = 0L;
        pendingUntilMs = 0L;
        lastParseMs = 0L;
    }

    public String formatRemaining() {
        return formatRemaining(System.currentTimeMillis());
    }

    public String formatRemaining(long nowMs) {
        if (nextRespawnAtMs <= 0L) return "N/A";
        return EmcStatsScoreboard.formatGrindTime(Math.max(0L, nextRespawnAtMs - nowMs));
    }

    private boolean isPending(long nowMs) {
        return pendingUntilMs > 0L && nowMs < pendingUntilMs;
    }

    private void expirePending(long nowMs) {
        if (pendingUntilMs > 0L && nowMs >= pendingUntilMs) {
            pendingUntilMs = 0L;
        }
    }
}
