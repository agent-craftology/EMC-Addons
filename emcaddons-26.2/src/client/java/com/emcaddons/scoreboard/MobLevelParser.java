package com.emcaddons.scoreboard;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses dungeon mob nameplates such as {@code [RARE] LVL1 Chicken ❤232}
 * or {@code LVL5 Goat ❤82.04M} into level, zone, and stage.
 * Minecraft-free so unit tests can call it without a game runtime.
 */
public final class MobLevelParser {
    private static final Pattern LVL_PATTERN = Pattern.compile(
            "(?:\\[?\\s*)?(?:LVL|Lv)\\.?\\s*(\\d+)(?:\\s*\\])?",
            Pattern.CASE_INSENSITIVE);

    private MobLevelParser() {}

    public static Optional<Result> parse(String name) {
        if (name == null || name.isEmpty()) return Optional.empty();
        String stripped = EmcSidebar.normalizeSmallCaps(name).replaceAll("§.", "");
        Matcher matcher = LVL_PATTERN.matcher(stripped);
        if (!matcher.find()) return Optional.empty();
        try {
            int level = Integer.parseInt(matcher.group(1));
            if (level < 1) return Optional.empty();
            int zone = ((level - 1) / 10) + 1;
            int stage = ((level - 1) % 10) + 1;
            return Optional.of(new Result(level, zone, stage));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static final class Result {
        public final int level;
        public final int zone;
        public final int stage;

        public Result(int level, int zone, int stage) {
            this.level = level;
            this.zone = zone;
            this.stage = stage;
        }
    }
}
