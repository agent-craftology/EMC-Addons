package com.emcaddons.scoreboard;

import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses {@code /zone reset} replies such as
 * {@code Mobs will respawn in 11 minutes 37 seconds}.
 * Minecraft-free so unit tests can call it without a game runtime.
 */
public final class ZoneResetParser {
    private static final Pattern ANCHOR = Pattern.compile(
            "respawn\\s+in\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOURS = Pattern.compile(
            "(\\d+)\\s+hours?", Pattern.CASE_INSENSITIVE);
    private static final Pattern MINUTES = Pattern.compile(
            "(\\d+)\\s+minutes?", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECONDS = Pattern.compile(
            "(\\d+)\\s+seconds?", Pattern.CASE_INSENSITIVE);

    private ZoneResetParser() {}

    public static OptionalLong parse(String text) {
        if (text == null || text.isEmpty()) return OptionalLong.empty();
        String stripped = text.replaceAll("§.", "");
        Matcher anchor = ANCHOR.matcher(stripped);
        if (!anchor.find()) return OptionalLong.empty();
        String rest = stripped.substring(anchor.end());
        boolean any = false;
        long total = 0L;
        Matcher hours = HOURS.matcher(rest);
        if (hours.find()) {
            total += parseGroup(hours) * 3600L;
            any = true;
        }
        Matcher minutes = MINUTES.matcher(rest);
        if (minutes.find()) {
            total += parseGroup(minutes) * 60L;
            any = true;
        }
        Matcher seconds = SECONDS.matcher(rest);
        if (seconds.find()) {
            total += parseGroup(seconds);
            any = true;
        }
        return any ? OptionalLong.of(total) : OptionalLong.empty();
    }

    private static long parseGroup(Matcher matcher) {
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
