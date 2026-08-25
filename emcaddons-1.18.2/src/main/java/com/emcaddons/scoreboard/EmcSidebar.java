package com.emcaddons.scoreboard;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Single-pass reader for the EMC sidebar. Position is preferred over label keywords.
 * 1.18.2 version.
 */
public final class EmcSidebar {
    private static final int SCORE_CREDITS = 3;
    private static final int SCORE_SHARDS = 4;
    private static final int SCORE_ESSENCE = 5;
    private static final int SCORE_SOULS = 6;
    private static final int SCORE_SWINGS = 11;
    private static final int SCORE_REBIRTH = 12;

    private static final Pattern REBIRTH_PATTERN =
            Pattern.compile("rebirth\\s*:\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);

    private EmcSidebar() {}

    public static Snapshot read(MinecraftClient client) {
        if (client == null || client.world == null) return Snapshot.empty();

        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(1); // SIDEBAR
        if (objective == null) return Snapshot.empty();

        double soulsPos = 0, essencePos = 0, shardsPos = 0, creditsPos = 0, swingsPos = 0;
        int rebirthPos = -1;
        boolean hasSoulsPos = false, hasEssencePos = false, hasShardsPos = false;
        boolean hasCreditsPos = false, hasSwingsPos = false, hasRebirthPos = false;

        double soulsKw = 0, essenceKw = 0, shardsKw = 0, creditsKw = 0, swingsKw = 0;
        int rebirthKw = -1;
        boolean hasSoulsKw = false, hasEssenceKw = false, hasShardsKw = false;
        boolean hasCreditsKw = false, hasSwingsKw = false, hasRebirthKw = false;

        boolean hub = false;
        boolean dungeonKw = false;

        for (ScoreboardPlayerScore score : scoreboard.getAllPlayerScores(objective)) {
            String ownerName = score.getPlayerName();
            String text = reconstructLine(scoreboard, ownerName);
            String stripped = text.replaceAll("§.", "");
            String normalized = normalizeSmallCaps(stripped);
            String lower = normalized.toLowerCase();
            if (isHubLine(lower)) hub = true;
            if (isDungeonKeyword(lower)) dungeonKw = true;
            if (!containsDigit(normalized)) continue;

            int scoreValue = score.getScore();
            boolean assignedByPosition = true;

            switch (scoreValue) {
                case SCORE_SOULS:
                    soulsPos = parseAmount(text);
                    hasSoulsPos = true;
                    break;
                case SCORE_ESSENCE:
                    essencePos = parseAmount(text);
                    hasEssencePos = true;
                    break;
                case SCORE_SHARDS:
                    shardsPos = parseAmount(text);
                    hasShardsPos = true;
                    break;
                case SCORE_CREDITS:
                    creditsPos = parseAmount(text);
                    hasCreditsPos = true;
                    break;
                case SCORE_SWINGS:
                    swingsPos = parseAmount(text);
                    hasSwingsPos = true;
                    break;
                case SCORE_REBIRTH:
                    rebirthPos = (int) parseAmount(text);
                    hasRebirthPos = true;
                    break;
                default:
                    assignedByPosition = false;
                    break;
            }

            if (assignedByPosition) continue;

            if (!hasSoulsKw && lower.contains("souls")) {
                soulsKw = parseAmount(text);
                hasSoulsKw = true;
            }
            if (!hasEssenceKw && lower.contains("essence")) {
                essenceKw = parseAmount(text);
                hasEssenceKw = true;
            }
            if (!hasShardsKw && lower.contains("shards")) {
                shardsKw = parseAmount(text);
                hasShardsKw = true;
            }
            if (!hasCreditsKw && lower.contains("credits")) {
                creditsKw = parseAmount(text);
                hasCreditsKw = true;
            }
            if (!hasSwingsKw && lower.contains("swings") && !lower.contains("swing rate")) {
                swingsKw = parseAmount(text);
                hasSwingsKw = true;
            }
            if (!hasRebirthKw) {
                Matcher rebirth = REBIRTH_PATTERN.matcher(lower);
                if (rebirth.find()) {
                    try {
                        rebirthKw = (int) Double.parseDouble(rebirth.group(1));
                        hasRebirthKw = true;
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        Location location = hub ? Location.HUB : (dungeonKw ? Location.DUNGEONS : Location.UNKNOWN);
        return new Snapshot(
                hasSoulsPos ? soulsPos : soulsKw,
                hasEssencePos ? essencePos : essenceKw,
                hasShardsPos ? shardsPos : shardsKw,
                hasCreditsPos ? creditsPos : creditsKw,
                hasSwingsPos ? swingsPos : swingsKw,
                hasRebirthPos ? rebirthPos : (hasRebirthKw ? rebirthKw : -1),
                hasSoulsPos || hasSoulsKw,
                hasEssencePos || hasEssenceKw,
                hasShardsPos || hasShardsKw,
                hasCreditsPos || hasCreditsKw,
                hasSwingsPos || hasSwingsKw,
                hasRebirthPos || hasRebirthKw,
                location
        );
    }

    public static String normalizeSmallCaps(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\u1D00': sb.append('a'); break; // ᴀ
                case '\u0299': sb.append('b'); break; // ʙ
                case '\u1D04': sb.append('c'); break; // ᴄ
                case '\u1D05': sb.append('d'); break; // ᴅ
                case '\u1D07': sb.append('e'); break; // ᴇ
                case '\u1D0A': sb.append('j'); break; // ᴊ
                case '\u1D0B': sb.append('k'); break; // ᴋ
                case '\u029F': sb.append('l'); break; // ʟ
                case '\u1D0D': sb.append('m'); break; // ᴍ
                case '\u0274': sb.append('n'); break; // ɴ
                case '\u1D0F': sb.append('o'); break; // ᴏ
                case '\u1D18': sb.append('p'); break; // ᴘ
                case '\u0280': sb.append('r'); break; // ʀ
                case '\uA731': sb.append('s'); break; // ꜱ
                case '\u1D1B': sb.append('t'); break; // ᴛ
                case '\u1D1C': sb.append('u'); break; // ᴜ
                case '\u1D21': sb.append('w'); break; // ᴡ
                case '\u028F': sb.append('y'); break; // ʏ
                case '\u1D22': sb.append('z'); break; // ᴢ
                case '\u0262': sb.append('g'); break; // ɢ
                case '\u029C': sb.append('h'); break; // ʜ
                case '\u026A': sb.append('i'); break; // ɪ
                case '\uA730': sb.append('f'); break; // ꜰ
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }

    public static double parseAmount(String text) {
        if (text == null || text.isEmpty()) return 0.0;
        text = text.replaceAll("§.", "").replace("$", "").replace(",", "").trim();

        Pattern decimalPattern = Pattern.compile("(\\d+\\.\\d+)\\s*([KMBTkmbt])");
        Matcher m1 = decimalPattern.matcher(text);
        if (m1.find()) {
            try {
                double number = Double.parseDouble(m1.group(1));
                String suffix = m1.group(2).toUpperCase();
                double multiplier = 1.0;
                switch (suffix) {
                    case "K": multiplier = 1_000.0; break;
                    case "M": multiplier = 1_000_000.0; break;
                    case "B": multiplier = 1_000_000_000.0; break;
                    case "T": multiplier = 1_000_000_000_000.0; break;
                    default: break;
                }
                return number * multiplier;
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        Pattern intPattern = Pattern.compile("(\\d+)\\s*([KMBTkmbt])");
        Matcher m2 = intPattern.matcher(text);
        if (m2.find()) {
            try {
                double number = Double.parseDouble(m2.group(1));
                String suffix = m2.group(2).toUpperCase();
                double multiplier = 1.0;
                switch (suffix) {
                    case "K": multiplier = 1_000.0; break;
                    case "M": multiplier = 1_000_000.0; break;
                    case "B": multiplier = 1_000_000_000.0; break;
                    case "T": multiplier = 1_000_000_000_000.0; break;
                    default: break;
                }
                return number * multiplier;
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        Pattern simplePattern = Pattern.compile("\\d+\\.?\\d*");
        Matcher simpleMatcher = simplePattern.matcher(text);
        if (simpleMatcher.find()) {
            try {
                return Double.parseDouble(simpleMatcher.group());
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        return 0.0;
    }

    private static String reconstructLine(Scoreboard scoreboard, String ownerName) {
        String text = ownerName;
        try {
            for (String teamName : scoreboard.getTeamNames()) {
                net.minecraft.scoreboard.Team team = scoreboard.getTeam(teamName);
                if (team != null && team.getPlayerList().contains(ownerName)) {
                    text = team.getPrefix().getString() + ownerName + team.getSuffix().getString();
                    break;
                }
            }
        } catch (Exception ignored) {}
        return text;
    }

    private static boolean containsDigit(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') return true;
        }
        return false;
    }

    /** Hub sidebar after {@link #normalizeSmallCaps}: {@code LOBBY SERVER} or {@code SERVER: Hub1}. */
    private static boolean isHubLine(String lower) {
        if (lower.contains("lobby server")) return true;
        return lower.contains("server:") && lower.contains("hub");
    }

    private static boolean isDungeonKeyword(String lower) {
        return lower.contains("souls") || lower.contains("essence") || lower.contains("shards")
                || lower.contains("swings") || lower.contains("rebirth");
    }

    public enum Location { HUB, DUNGEONS, UNKNOWN }

    public static final class Snapshot {
        public final double souls;
        public final double essence;
        public final double shards;
        public final double credits;
        public final double swings;
        public final int rebirthLevel;

        public final boolean hasSouls;
        public final boolean hasEssence;
        public final boolean hasShards;
        public final boolean hasCredits;
        public final boolean hasSwings;
        public final boolean hasRebirth;
        public final Location location;

        private Snapshot(
                double souls, double essence, double shards, double credits, double swings,
                int rebirthLevel,
                boolean hasSouls, boolean hasEssence, boolean hasShards,
                boolean hasCredits, boolean hasSwings, boolean hasRebirth,
                Location location
        ) {
            this.souls = souls;
            this.essence = essence;
            this.shards = shards;
            this.credits = credits;
            this.swings = swings;
            this.rebirthLevel = rebirthLevel;
            this.hasSouls = hasSouls;
            this.hasEssence = hasEssence;
            this.hasShards = hasShards;
            this.hasCredits = hasCredits;
            this.hasSwings = hasSwings;
            this.hasRebirth = hasRebirth;
            this.location = location == null ? Location.UNKNOWN : location;
        }

        public boolean countsStats() {
            return location == Location.DUNGEONS;
        }

        public static Snapshot empty() {
            return new Snapshot(0, 0, 0, 0, 0, -1, false, false, false, false, false, false, Location.UNKNOWN);
        }
    }
}
