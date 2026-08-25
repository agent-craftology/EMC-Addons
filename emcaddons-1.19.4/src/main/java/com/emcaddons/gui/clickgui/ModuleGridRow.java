package com.emcaddons.gui.clickgui;

import java.util.function.Consumer;

/**
 * Card grid for gamemode modules. Whole card is clickable; no on/off toggle.
 */
public final class ModuleGridRow extends SettingRow {

    public enum Mode {
        DUNGEONS("Dungeons", "Track dungeon currencies, rates, and grind time"),
        GENS("Gens", "Coming Soon!"),
        FACTORIES("Factories", "Coming Soon!"),
        SKYBLOCK("Skyblock", "Coming Soon!"),
        PRISONS("Prisons", "Coming Soon!");

        public final String title;
        public final String description;

        Mode(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public boolean comingSoon() {
            return this != DUNGEONS;
        }
    }

    private static final Mode[] MODES = Mode.values();

    private final Consumer<Mode> onOpen;
    private final int[] cardX = new int[MODES.length];
    private final int[] cardY = new int[MODES.length];
    private int lastW = GuiTheme.CARD_W * 3 + GuiTheme.CARD_GAP * 2;
    private int lastCardW = GuiTheme.CARD_W;

    public ModuleGridRow(Consumer<Mode> onOpen) {
        this.onOpen = onOpen;
    }

    @Override
    public int height() {
        return gridHeight(Math.max(1, lastW));
    }

    @Override
    public void render(GuiDraw d, int x, int y, int w, int mouseX, int mouseY) {
        lastW = w;
        int h = gridHeight(w);
        bounds(x, y, w, h);
        int cols = columns(w);
        int gap = GuiTheme.CARD_GAP;
        int cardW = cardWidth(w, cols);
        lastCardW = cardW;
        for (int i = 0; i < MODES.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (cardW + gap);
            int cy = y + row * (GuiTheme.CARD_H + gap);
            cardX[i] = cx;
            cardY[i] = cy;
            Mode mode = MODES[i];
            boolean hover = GuiTheme.hit(cx, cy, cardW, GuiTheme.CARD_H, mouseX, mouseY);
            d.fillRoundRect(cx, cy, cardW, GuiTheme.CARD_H, GuiTheme.CARD_RADIUS,
                    hover ? GuiTheme.CARD_HOVER : GuiTheme.ROW);
            if (hover) {
                d.fill(cx, cy + 8, 2, GuiTheme.CARD_H - 16, GuiTheme.ACCENT);
            }
            int textX = cx + 12;
            d.text(mode.title, textX, cy + 14, GuiTheme.TITLE);
            d.text(d.ellipsize(mode.description, cardW - 24), textX, cy + 30, GuiTheme.MUTED);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        for (int i = 0; i < MODES.length; i++) {
            if (GuiTheme.hit(cardX[i], cardY[i], lastCardW, GuiTheme.CARD_H, mouseX, mouseY)) {
                if (onOpen != null) onOpen.accept(MODES[i]);
                return true;
            }
        }
        return false;
    }

    private static int columns(int w) {
        int gap = GuiTheme.CARD_GAP;
        int min = GuiTheme.CARD_W;
        if (w >= 3 * min + 2 * gap) return 3;
        if (w >= 2 * min + gap) return 2;
        return 1;
    }

    private static int cardWidth(int w, int cols) {
        return (w - (cols - 1) * GuiTheme.CARD_GAP) / cols;
    }

    private static int gridHeight(int w) {
        int cols = columns(w);
        int rows = (MODES.length + cols - 1) / cols;
        return rows * GuiTheme.CARD_H + Math.max(0, rows - 1) * GuiTheme.CARD_GAP;
    }
}
