package com.emcaddons.scoreboard;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses {@code Mobs will respawn in …} chat from {@code /zone reset}. Pure Java; no Minecraft types.
 */
public final class ZoneResetParser {

    private static final Pattern COLOR_CODES = Pattern.compile("§.");
    private static final Pattern RESPAWN = Pattern.compile(
            "mobs will respawn in\\s+"
                    + "(?:(\\d+)\\s+hours?)?"
                    + "(?:\\s*(?:and\\s+)?(\\d+)\\s+minutes?)?"
                    + "(?:\\s*(?:and\\s+)?(\\d+)\\s+seconds?)?",
            Pattern.CASE_INSENSITIVE
    );

    private ZoneResetParser() {}

    /**
     * @return remaining seconds until mobs respawn, or empty if the text does not match
     */
    public static OptionalInt parse(String message) {
        if (message == null || message.isEmpty()) return OptionalInt.empty();
        String stripped = COLOR_CODES.matcher(message).replaceAll("");
        Matcher matcher = RESPAWN.matcher(stripped);
        if (!matcher.find()) return OptionalInt.empty();
        String hours = matcher.group(1);
        String minutes = matcher.group(2);
        String seconds = matcher.group(3);
        if (hours == null && minutes == null && seconds == null) return OptionalInt.empty();
        long total = 0L;
        try {
            if (hours != null) total += Long.parseLong(hours) * 3600L;
            if (minutes != null) total += Long.parseLong(minutes) * 60L;
            if (seconds != null) total += Long.parseLong(seconds);
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
        if (total < 0L || total > Integer.MAX_VALUE) return OptionalInt.empty();
        return OptionalInt.of((int) total);
    }
}
