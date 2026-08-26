package com.emcaddons.scoreboard;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses dungeon mob custom names such as {@code [RARE] LVL1 Chicken ❤232}.
 * Minecraft-free so it can be unit-tested without the game.
 */
public final class MobLevelParser {
    private static final Pattern LVL_PATTERN =
            Pattern.compile("(?:\\[?\\s*)?(?:LVL|Lv)\\.?\\s*(\\d+)(?:\\s*\\])?", Pattern.CASE_INSENSITIVE);

    private MobLevelParser() {}

    public static Optional<Parsed> parse(String name) {
        if (name == null || name.isEmpty()) return Optional.empty();
        String stripped = EmcSidebar.normalizeSmallCaps(name).replaceAll("§.", "");
        Matcher matcher = LVL_PATTERN.matcher(stripped);
        if (!matcher.find()) return Optional.empty();
        int level;
        try {
            level = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        if (level < 1) return Optional.empty();
        int zone = ((level - 1) / 10) + 1;
        int stage = ((level - 1) % 10) + 1;
        return Optional.of(new Parsed(level, zone, stage));
    }

    public static final class Parsed {
        public final int level;
        public final int zone;
        public final int stage;

        public Parsed(int level, int zone, int stage) {
            this.level = level;
            this.zone = zone;
            this.stage = stage;
        }
    }
}
