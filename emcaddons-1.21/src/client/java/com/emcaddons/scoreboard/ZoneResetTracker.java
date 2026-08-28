package com.emcaddons.scoreboard;

import java.util.Optional;

/**
 * Tracks dungeon mob respawn countdown from auto {@code /zone reset} replies.
 * Minecraft-free so query/hide logic can be reasoned about without the game.
 */
public final class ZoneResetTracker {
    static final long PENDING_MS = 2_000L;
    static final long REPARSE_MS = 90_000L;

    private long nextRespawnAtMs;
    private long lastQueryMs;
    private long pendingUntilMs;
    private long lastParseMs;

    public void clear() {
        nextRespawnAtMs = 0L;
        lastQueryMs = 0L;
        pendingUntilMs = 0L;
        lastParseMs = 0L;
    }

    /**
     * Parse every matching message. Returns {@code true} (hide) only while a
     * query is pending — manual {@code /zone reset} replies stay visible.
     */
    public boolean onGameMessage(String message) {
        Optional<Integer> parsed = ZoneResetParser.parse(message);
        if (parsed.isEmpty()) return false;
        long nowMs = System.currentTimeMillis();
        expirePending(nowMs);
        boolean hide = pendingUntilMs != 0L;
        nextRespawnAtMs = nowMs + parsed.get() * 1000L;
        lastParseMs = nowMs;
        pendingUntilMs = 0L;
        return hide;
    }

    public boolean shouldSendQuery(boolean inDungeons, boolean hudVisible, boolean showRespawn, long nowMs) {
        expirePending(nowMs);
        if (!inDungeons || !hudVisible || !showRespawn) return false;
        if (pendingUntilMs != 0L) return false;
        if (lastQueryMs == 0L) return true;

        boolean remainingExpired = nextRespawnAtMs != 0L && nowMs >= nextRespawnAtMs;
        boolean neverParsed = lastParseMs == 0L;
        boolean parseStale = lastParseMs != 0L && nowMs - lastParseMs >= REPARSE_MS;
        if (!remainingExpired && !neverParsed && !parseStale) return false;

        boolean noParseSinceLastQuery = lastParseMs < lastQueryMs;
        if (noParseSinceLastQuery && nowMs - lastQueryMs < REPARSE_MS) return false;
        return true;
    }

    public void markQuerySent(long nowMs) {
        lastQueryMs = nowMs;
        pendingUntilMs = nowMs + PENDING_MS;
    }

    public String displayText(long nowMs) {
        if (nextRespawnAtMs == 0L) return "N/A";
        long remainingMs = Math.max(0L, nextRespawnAtMs - nowMs);
        return formatRemaining(remainingMs);
    }

    static String formatRemaining(long ms) {
        long totalSec = Math.max(0L, ms / 1000L);
        long h = totalSec / 3600L;
        long m = (totalSec % 3600L) / 60L;
        long s = totalSec % 60L;
        if (h > 0) return String.format("%dh %02dm %02ds", h, m, s);
        if (m > 0) return String.format("%dm %02ds", m, s);
        return s + "s";
    }

    private void expirePending(long nowMs) {
        if (pendingUntilMs != 0L && nowMs >= pendingUntilMs) {
            pendingUntilMs = 0L;
        }
    }
}
