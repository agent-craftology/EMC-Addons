package com.emcaddons.gui.clickgui;

public final class GuiTheme {
    private GuiTheme() {}

    public static int DIM = 0x66000000;
    public static int WINDOW = 0xFF10151A;
    public static int SIDEBAR = 0xFF141A20;
    public static int CARD = 0xFF161C22;
    public static int CARD_HOVER = 0xFF1C242C;
    public static int ACCENT = 0xFF3DDC64;
    public static int ACCENT_SOFT = 0x333DDC64;
    public static int TITLE = 0xFFE8E8F0;
    public static int MUTED = 0xFF8A8A9A;
    public static int ON = 0xFF55FF88;
    public static int OFF = 0xFFFF5555;
    public static int FIELD = 0xFF101118;
    public static int FIELD_BORDER = 0xFF2A2B36;
    public static int PILL = 0xFF222330;
    public static int ROW = 0xFF14151D;
    public static int ROW_HOVER = 0xFF1C242C;
    public static int TRACK = 0xFF0E0F16;
    public static int KNOB = 0xFFE8E8F0;
    public static int DANGER = 0xFFFF6B6B;
    public static int PAUSED = 0xFFFFCC66;

    public static int SHADOW = 0x60000000;
    public static int SHADOW_SOFT = 0x28000000;
    public static int HEADER_GRAD_TOP = 0xFF171E27;
    public static int HEADER_GRAD_BOTTOM = 0x00171E27;
    public static int SCROLLBAR_TRACK = 0x33000000;
    public static int SCROLLBAR_THUMB = 0x662A2B36;
    public static int SCROLLBAR_THUMB_HOVER = 0x993DDC64;
    public static int TOOLTIP_BG = 0xF0121820;
    public static int TOOLTIP_BORDER = 0xFF2A2B36;
    public static int TAB_BG = 0xFF10151A;
    public static int HUD_BG = 0xE0121820;
    public static int HUD_TITLE = 0xFFE8E8F0;
    public static int HUD_MUTED = 0xFF8A8A9A;

    public static int ACCENT_MINER = 0xFF3DDC64;
    public static int ACCENT_FARMER = 0xFFB8E24D;
    public static int ACCENT_DUNGEON = 0xFFB865F2;

    public static final int WINDOW_RADIUS = 10;
    public static final int WIN_RADIUS = WINDOW_RADIUS;
    public static final int CARD_RADIUS = 8;
    public static final int PILL_RADIUS = 6;
    public static final int ROW_RADIUS = 6;
    public static final int SIDEBAR_W = 168;
    public static final int HEADER_H = 46;
    public static final int CARD_W = 186;
    public static final int CARD_H = 70;
    public static final int CARD_GAP = 10;
    public static final int PAD = 14;
    public static final int NAV_H = 26;
    public static final int ROW_H = 32;
    public static final int SLIDER_H = 42;
    public static final int COORD_H = 36;
    public static final int SEARCH_W = 176;
    public static final int SEARCH_H = 22;
    public static final int ICON = 8;
    public static final String PRODUCT = "EMC Addons";

    public static final int OPACITY_MIN = 0;
    public static final int OPACITY_MAX = 100;
    public static final int OPACITY_DEFAULT = 100;
    /**
     * Window / sidebar / card / row alpha never drops below this percent of the
     * theme's original alpha. The 0–100 slider is mapped onto
     * {@code [WINDOW_ALPHA_FLOOR_PCT, 100]} so 0% still leaves a usable panel
     * (every stop looks different). Dim overlay and shadows scale 0–100 with
     * the slider. Text, accents, knobs, and HUD cards stay fully opaque.
     */
    public static final int WINDOW_ALPHA_FLOOR_PCT = 40;

    private static Theme current = Theme.EMERALD;
    private static int opacityPct = OPACITY_DEFAULT;

    static {
        apply(Theme.EMERALD);
    }

    public static boolean hit(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    public static Theme current() {
        return current;
    }

    public static int getOpacityPercent() {
        return opacityPct;
    }

    public static int clampOpacity(int percent) {
        if (percent < OPACITY_MIN) return OPACITY_MIN;
        if (percent > OPACITY_MAX) return OPACITY_MAX;
        return percent;
    }

    public static int soft(int accent) {
        return (accent & 0x00FFFFFF) | 0x33000000;
    }

    public static void apply(Theme theme) {
        apply(theme, opacityPct);
    }

    public static void apply(Theme theme, int opacityPercent) {
        opacityPct = clampOpacity(opacityPercent);
        if (theme == null) theme = Theme.EMERALD;
        current = theme;
        theme.paint();
        if (theme != Theme.LIGHT_BLUE) {
            HUD_TITLE = TITLE;
            HUD_MUTED = MUTED;
        }
        ACCENT_SOFT = soft(ACCENT);
        SCROLLBAR_THUMB_HOVER = (ACCENT & 0x00FFFFFF) | 0x99000000;
        HEADER_GRAD_BOTTOM = HEADER_GRAD_TOP & 0x00FFFFFF;
        ROW_HOVER = CARD_HOVER;
        applyOpacity();
    }

    private static void applyOpacity() {
        if (opacityPct >= OPACITY_MAX) return;
        float chrome = chromeScale();
        float veil = opacityPct / 100f;
        DIM = scaleAlpha(DIM, veil);
        SHADOW = scaleAlpha(SHADOW, veil);
        SHADOW_SOFT = scaleAlpha(SHADOW_SOFT, veil);
        SCROLLBAR_TRACK = scaleAlpha(SCROLLBAR_TRACK, veil);

        WINDOW = scaleAlpha(WINDOW, chrome);
        SIDEBAR = scaleAlpha(SIDEBAR, chrome);
        CARD = scaleAlpha(CARD, chrome);
        CARD_HOVER = scaleAlpha(CARD_HOVER, chrome);
        ACCENT_SOFT = scaleAlpha(ACCENT_SOFT, chrome);
        FIELD = scaleAlpha(FIELD, chrome);
        FIELD_BORDER = scaleAlpha(FIELD_BORDER, chrome);
        PILL = scaleAlpha(PILL, chrome);
        ROW = scaleAlpha(ROW, chrome);
        ROW_HOVER = scaleAlpha(ROW_HOVER, chrome);
        TRACK = scaleAlpha(TRACK, chrome);
        HEADER_GRAD_TOP = scaleAlpha(HEADER_GRAD_TOP, chrome);
        HEADER_GRAD_BOTTOM = scaleAlpha(HEADER_GRAD_BOTTOM, chrome);
        SCROLLBAR_THUMB = scaleAlpha(SCROLLBAR_THUMB, chrome);
        SCROLLBAR_THUMB_HOVER = scaleAlpha(SCROLLBAR_THUMB_HOVER, chrome);
        TOOLTIP_BG = scaleAlpha(TOOLTIP_BG, chrome);
        TOOLTIP_BORDER = scaleAlpha(TOOLTIP_BORDER, chrome);
        TAB_BG = scaleAlpha(TAB_BG, chrome);
    }

    /** Slider 0–100 → chrome alpha 40–100% of the theme original. */
    private static float chromeScale() {
        return WINDOW_ALPHA_FLOOR_PCT / 100f
                + (opacityPct / 100f) * (1f - WINDOW_ALPHA_FLOOR_PCT / 100f);
    }

    private static int scaleAlpha(int argb, float t) {
        if (t >= 1f) return argb;
        int a = (argb >>> 24) & 0xFF;
        int scaled = Math.round(a * Math.max(0f, t));
        return (argb & 0x00FFFFFF) | (scaled << 24);
    }

    public enum Theme {
        EMERALD("Emerald") {
            @Override
            void paint() {
                DIM = 0x66000000;
                WINDOW = 0xFF10151A;
                SIDEBAR = 0xFF141A20;
                CARD = 0xFF161C22;
                CARD_HOVER = 0xFF1C242C;
                ACCENT = 0xFF3DDC64;
                TITLE = 0xFFE8E8F0;
                MUTED = 0xFF8A8A9A;
                ON = 0xFF55FF88;
                OFF = 0xFFFF5555;
                FIELD = 0xFF101118;
                FIELD_BORDER = 0xFF2A2B36;
                PILL = 0xFF222330;
                ROW = 0xFF14151D;
                TRACK = 0xFF0E0F16;
                KNOB = 0xFFE8E8F0;
                DANGER = 0xFFFF6B6B;
                PAUSED = 0xFFFFCC66;
                SHADOW = 0x60000000;
                SHADOW_SOFT = 0x28000000;
                HEADER_GRAD_TOP = 0xFF171E27;
                SCROLLBAR_TRACK = 0x33000000;
                SCROLLBAR_THUMB = 0x662A2B36;
                TOOLTIP_BG = 0xF0121820;
                TOOLTIP_BORDER = 0xFF2A2B36;
                TAB_BG = 0xFF10151A;
                HUD_BG = 0xE0121820;
                ACCENT_MINER = 0xFF3DDC64;
                ACCENT_FARMER = 0xFFB8E24D;
                ACCENT_DUNGEON = 0xFFB865F2;
            }
        },
        MIDNIGHT("Midnight") {
            @Override
            void paint() {
                DIM = 0x66000000;
                WINDOW = 0xFF0C1018;
                SIDEBAR = 0xFF10161F;
                CARD = 0xFF141A24;
                CARD_HOVER = 0xFF1A2230;
                ACCENT = 0xFF5B8CFF;
                TITLE = 0xFFE6ECF8;
                MUTED = 0xFF8892A8;
                ON = 0xFF7AA2FF;
                OFF = 0xFFFF6B7A;
                FIELD = 0xFF0A0E16;
                FIELD_BORDER = 0xFF2A3348;
                PILL = 0xFF1C2433;
                ROW = 0xFF121826;
                TRACK = 0xFF0A0E16;
                KNOB = 0xFFE6ECF8;
                DANGER = 0xFFFF6B7A;
                PAUSED = 0xFFFFCC66;
                SHADOW = 0x60000000;
                SHADOW_SOFT = 0x28000000;
                HEADER_GRAD_TOP = 0xFF151C28;
                SCROLLBAR_TRACK = 0x33000000;
                SCROLLBAR_THUMB = 0x66344A72;
                TOOLTIP_BG = 0xF0101620;
                TOOLTIP_BORDER = 0xFF2A3348;
                TAB_BG = 0xFF0C1018;
                HUD_BG = 0xE0101620;
                ACCENT_MINER = 0xFF5B8CFF;
                ACCENT_FARMER = 0xFF4EC9B0;
                ACCENT_DUNGEON = 0xFF9B7DFF;
            }
        },
        EMBER("Ember") {
            @Override
            void paint() {
                DIM = 0x66000000;
                WINDOW = 0xFF161210;
                SIDEBAR = 0xFF1C1614;
                CARD = 0xFF221A16;
                CARD_HOVER = 0xFF2A211C;
                ACCENT = 0xFFFF8A3D;
                TITLE = 0xFFF4EBE3;
                MUTED = 0xFFA89888;
                ON = 0xFFFFB06B;
                OFF = 0xFFFF6B6B;
                FIELD = 0xFF120E0C;
                FIELD_BORDER = 0xFF3A2C24;
                PILL = 0xFF2A221C;
                ROW = 0xFF1A1412;
                TRACK = 0xFF120E0C;
                KNOB = 0xFFF4EBE3;
                DANGER = 0xFFFF6B6B;
                PAUSED = 0xFFFFCC66;
                SHADOW = 0x60000000;
                SHADOW_SOFT = 0x28000000;
                HEADER_GRAD_TOP = 0xFF221A16;
                SCROLLBAR_TRACK = 0x33000000;
                SCROLLBAR_THUMB = 0x665C3A28;
                TOOLTIP_BG = 0xF0161210;
                TOOLTIP_BORDER = 0xFF3A2C24;
                TAB_BG = 0xFF161210;
                HUD_BG = 0xE0161210;
                ACCENT_MINER = 0xFFFF8A3D;
                ACCENT_FARMER = 0xFFE8C547;
                ACCENT_DUNGEON = 0xFFE05A4F;
            }
        },
        OCEAN("Ocean") {
            @Override
            void paint() {
                DIM = 0x66000000;
                WINDOW = 0xFF0E1618;
                SIDEBAR = 0xFF121C1E;
                CARD = 0xFF152022;
                CARD_HOVER = 0xFF1C2A2C;
                ACCENT = 0xFF2ECFC4;
                TITLE = 0xFFE4F4F2;
                MUTED = 0xFF7A9A96;
                ON = 0xFF5EE0D6;
                OFF = 0xFFFF6B7A;
                FIELD = 0xFF0A1214;
                FIELD_BORDER = 0xFF244044;
                PILL = 0xFF1A2A2C;
                ROW = 0xFF121A1C;
                TRACK = 0xFF0A1214;
                KNOB = 0xFFE4F4F2;
                DANGER = 0xFFFF6B7A;
                PAUSED = 0xFFFFCC66;
                SHADOW = 0x60000000;
                SHADOW_SOFT = 0x28000000;
                HEADER_GRAD_TOP = 0xFF152022;
                SCROLLBAR_TRACK = 0x33000000;
                SCROLLBAR_THUMB = 0x662A5C58;
                TOOLTIP_BG = 0xF0121A1C;
                TOOLTIP_BORDER = 0xFF244044;
                TAB_BG = 0xFF0E1618;
                HUD_BG = 0xE0121A1C;
                ACCENT_MINER = 0xFF2ECFC4;
                ACCENT_FARMER = 0xFF7ED957;
                ACCENT_DUNGEON = 0xFF5BA8FF;
            }
        },
        AMETHYST("Amethyst") {
            @Override
            void paint() {
                DIM = 0x66000000;
                WINDOW = 0xFF121018;
                SIDEBAR = 0xFF18141F;
                CARD = 0xFF1C1826;
                CARD_HOVER = 0xFF252032;
                ACCENT = 0xFFB07CFF;
                TITLE = 0xFFF0E8F8;
                MUTED = 0xFF9788AA;
                ON = 0xFFC9A0FF;
                OFF = 0xFFFF6B8A;
                FIELD = 0xFF100E16;
                FIELD_BORDER = 0xFF3A2E4A;
                PILL = 0xFF241E30;
                ROW = 0xFF16121E;
                TRACK = 0xFF100E16;
                KNOB = 0xFFF0E8F8;
                DANGER = 0xFFFF6B8A;
                PAUSED = 0xFFFFCC66;
                SHADOW = 0x60000000;
                SHADOW_SOFT = 0x28000000;
                HEADER_GRAD_TOP = 0xFF1C1826;
                SCROLLBAR_TRACK = 0x33000000;
                SCROLLBAR_THUMB = 0x664A3A68;
                TOOLTIP_BG = 0xF014101A;
                TOOLTIP_BORDER = 0xFF3A2E4A;
                TAB_BG = 0xFF121018;
                HUD_BG = 0xE014101A;
                ACCENT_MINER = 0xFFB07CFF;
                ACCENT_FARMER = 0xFFE07AD6;
                ACCENT_DUNGEON = 0xFF7C8CFF;
            }
        },
        LIGHT_BLUE("Light Blue") {
            @Override
            void paint() {
                DIM = 0x4A3A5060;
                WINDOW = 0xFFF4FAFE;
                SIDEBAR = 0xFFE6F2FB;
                CARD = 0xFFFFFFFF;
                CARD_HOVER = 0xFFD4EAF8;
                ACCENT = 0xFF3BA4E6;
                TITLE = 0xFF0D1B2A;
                MUTED = 0xFF3D5568;
                ON = 0xFF1B8A4A;
                OFF = 0xFFC62828;
                FIELD = 0xFFFFFFFF;
                FIELD_BORDER = 0xFFB4D4E8;
                PILL = 0xFFD4E8F5;
                ROW = 0xFFEEF6FC;
                TRACK = 0xFFC8DCE8;
                KNOB = 0xFF3BA4E6;
                DANGER = 0xFFD64545;
                PAUSED = 0xFFB8860B;
                SHADOW = 0x28000000;
                SHADOW_SOFT = 0x14000000;
                HEADER_GRAD_TOP = 0xFFE3F1FA;
                SCROLLBAR_TRACK = 0x22000000;
                SCROLLBAR_THUMB = 0x6688B8D0;
                TOOLTIP_BG = 0xF0F4FAFE;
                TOOLTIP_BORDER = 0xFFB4D4E8;
                TAB_BG = 0xFFF4FAFE;
                HUD_BG = 0xE8F4FAFE;
                HUD_TITLE = 0xFF4A6B82;
                HUD_MUTED = 0xFF7A96A8;
                ACCENT_MINER = 0xFF2E9A4A;
                ACCENT_FARMER = 0xFF6B9A20;
                ACCENT_DUNGEON = 0xFF7A4EC8;
            }
        };

        public final String displayName;

        Theme(String displayName) {
            this.displayName = displayName;
        }

        abstract void paint();

        public static String[] displayNames() {
            Theme[] values = values();
            String[] names = new String[values.length];
            for (int i = 0; i < values.length; i++) names[i] = values[i].displayName;
            return names;
        }

        public static Theme fromIndex(int index) {
            Theme[] values = values();
            if (index < 0 || index >= values.length) return EMERALD;
            return values[index];
        }
    }
}
