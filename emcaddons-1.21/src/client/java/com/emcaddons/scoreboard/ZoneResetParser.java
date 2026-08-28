package com.emcaddons.scoreboard;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses dungeon {@code /zone reset} chat such as
 * {@code Mobs will respawn in 11 minutes 37 seconds}.
 * Minecraft-free so it can be unit-tested without the game.
 */
public final class ZoneResetParser {
    private static final Pattern PREFIX = Pattern.compile(
            "mobs\\s+will\\s+respawn\\s+in\\s+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern UNIT = Pattern.compile(
            "(\\d+)\\s+(hours?|minutes?|seconds?)",
            Pattern.CASE_INSENSITIVE);

    private ZoneResetParser() {}

    /**
     * @return remaining seconds until respawn, or empty if the line does not match
     */
    public static Optional<Integer> parse(String message) {
        if (message == null || message.isEmpty()) return Optional.empty();
        String stripped = message.replaceAll("§.", "");
        Matcher prefix = PREFIX.matcher(stripped);
        if (!prefix.find()) return Optional.empty();
        String rest = stripped.substring(prefix.end());
        Matcher units = UNIT.matcher(rest);
        int total = 0;
        boolean found = false;
        while (units.find()) {
            int amount;
            try {
                amount = Integer.parseInt(units.group(1));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
            String unit = units.group(2).toLowerCase(Locale.ROOT);
            if (unit.startsWith("hour")) {
                total += amount * 3600;
            } else if (unit.startsWith("minute")) {
                total += amount * 60;
            } else if (unit.startsWith("second")) {
                total += amount;
            }
            found = true;
        }
        if (!found) return Optional.empty();
        return Optional.of(total);
    }
}
