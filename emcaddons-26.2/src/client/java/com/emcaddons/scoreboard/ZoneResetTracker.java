package com.emcaddons.scoreboard;

import java.util.OptionalLong;
import java.util.function.LongSupplier;

/**
 * Local countdown from {@code /zone reset} replies, plus auto-query cadence.
 */
public final class ZoneResetTracker {
    static final long PENDING_WINDOW_MS = 2_000L;
    static final long DRIFT_REQUERY_MS = 90_000L;

    private final LongSupplier clock;
    private long nextRespawnAtMs;
    private long lastQueryMs;
    private long pendingUntilMs;
    private long lastParseMs;

    public ZoneResetTracker() {
        this(System::currentTimeMillis);
    }

    ZoneResetTracker(LongSupplier clock) {
        this.clock = clock == null ? System::currentTimeMillis : clock;
    }

    public void clear() {
        nextRespawnAtMs = 0L;
        lastQueryMs = 0L;
        pendingUntilMs = 0L;
        lastParseMs = 0L;
    }

    /**
     * Parse every matching message. Returns {@code true} (hide) only when an
     * auto-query is still pending.
     */
    public boolean onGameMessage(String text) {
        OptionalLong seconds = ZoneResetParser.parse(text);
        if (seconds.isEmpty()) return false;
        long now = clock.getAsLong();
        nextRespawnAtMs = now + seconds.getAsLong() * 1000L;
        lastParseMs = now;
        boolean hide = isPending(now);
        pendingUntilMs = 0L;
        return hide;
    }

    public boolean shouldSendQuery(boolean inDungeons, boolean masterVisible, boolean zoneCardVisible, boolean showRespawn) {
        if (!showRespawn || !inDungeons || !masterVisible || !zoneCardVisible) return false;
        long now = clock.getAsLong();
        if (isPending(now)) return false;
        if (lastQueryMs != 0L && now - lastQueryMs < PENDING_WINDOW_MS) return false;
        if (lastQueryMs == 0L) return true;
        if (nextRespawnAtMs != 0L && now >= nextRespawnAtMs) return true;
        if (lastParseMs == 0L) return now - lastQueryMs >= DRIFT_REQUERY_MS;
        return now - lastParseMs >= DRIFT_REQUERY_MS;
    }

    public void markQuerySent() {
        long now = clock.getAsLong();
        lastQueryMs = now;
        pendingUntilMs = now + PENDING_WINDOW_MS;
    }

    public String remainingText() {
        if (nextRespawnAtMs == 0L) return "N/A";
        long remainingMs = nextRespawnAtMs - clock.getAsLong();
        if (remainingMs <= 0L) return "0s";
        return formatDuration(remainingMs);
    }

    private boolean isPending(long now) {
        return pendingUntilMs != 0L && now < pendingUntilMs;
    }

    private static String formatDuration(long ms) {
        long totalSec = Math.max(0L, ms) / 1000L;
        long hours = totalSec / 3600L;
        long minutes = (totalSec % 3600L) / 60L;
        long seconds = totalSec % 60L;
        if (hours > 0) return hours + "h " + pad2(minutes) + "m " + pad2(seconds) + "s";
        if (minutes > 0) return minutes + "m " + pad2(seconds) + "s";
        return seconds + "s";
    }

    private static String pad2(long value) {
        return value < 10 ? "0" + value : Long.toString(value);
    }
}
