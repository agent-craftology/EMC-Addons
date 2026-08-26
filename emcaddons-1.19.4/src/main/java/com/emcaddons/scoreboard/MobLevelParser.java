package com.emcaddons.scoreboard;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses dungeon mob nameplates such as {@code LVL5 Goat ❤82.04M}
 * into level, zone, and stage. Accepts {@code LVL5}, {@code LVL 5}, {@code Lv.5}, and {@code [LVL5]}.
 */
public final class MobLevelParser {

    private static final Pattern LVL_PATTERN = Pattern.compile("LV\\.?L?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private MobLevelParser() {}

    public static Optional<Result> parse(String name) {
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
        return Optional.of(new Result(level, zone, stage));
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
