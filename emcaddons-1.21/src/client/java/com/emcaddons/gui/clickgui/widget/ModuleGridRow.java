package com.emcaddons.gui.clickgui.widget;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import com.emcaddons.scoreboard.GameMode;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

public final class ModuleGridRow extends SettingRow {
    private static final GameMode[] MODES = GameMode.values();
    private static final int MAX_COLS = 3;
    private static final int MIN_CARD_W = 80;

    private final Consumer<GameMode> onSelect;
    private final int[] cardX = new int[MODES.length];
    private final int[] cardY = new int[MODES.length];
    private int cols = 1;
    private int gridRows = 2;
    private int cardW = GuiTheme.CARD_W;

    public ModuleGridRow(Consumer<GameMode> onSelect) {
        this.onSelect = onSelect;
    }

    @Override
    protected void onLayout() {
        int gap = GuiTheme.CARD_GAP;
        int n = MODES.length;
        cols = Math.min(MAX_COLS, n);
        while (cols > 1 && (w - (cols - 1) * gap) / cols < MIN_CARD_W) {
            cols--;
        }
        cols = Math.max(1, cols);
        cardW = Math.max(1, (w - (cols - 1) * gap) / cols);
        gridRows = Math.max(1, (n + cols - 1) / cols);
        for (int i = 0; i < n; i++) {
            int col = i % cols;
            int row = i / cols;
            cardX[i] = x + col * (cardW + gap);
            cardY[i] = y + row * (GuiTheme.CARD_H + gap);
        }
    }

    @Override
    public int getHeight() {
        int rows = Math.max(1, gridRows);
        return rows * GuiTheme.CARD_H + (rows - 1) * GuiTheme.CARD_GAP;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        for (int i = 0; i < MODES.length; i++) {
            drawCard(ctx, tr, mouseX, mouseY, MODES[i], cardX[i], cardY[i]);
        }
    }

    private void drawCard(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY,
                          GameMode mode, int cx, int cy) {
        boolean hover = GuiDraw.hit(mouseX, mouseY, cx, cy, cardW, GuiTheme.CARD_H);
        GuiDraw.fillRoundRect(ctx, cx, cy, cardW, GuiTheme.CARD_H, GuiTheme.CARD_RADIUS,
                hover ? GuiTheme.CARD_HOVER : GuiTheme.ROW);
        if (hover) {
            GuiDraw.fill(ctx, cx, cy + 8, 2, GuiTheme.CARD_H - 16, GuiTheme.ACCENT);
        }
        int textX = cx + 12;
        int maxText = Math.max(8, cardW - 24);
        String desc = mode.isComingSoon() ? "Coming Soon!" : "Track dungeon currencies, rates, and grind time";
        GuiDraw.text(ctx, tr, GuiDraw.ellipsize(tr, mode.displayName, maxText), textX, cy + 16, GuiTheme.TITLE);
        GuiDraw.text(ctx, tr, GuiDraw.ellipsize(tr, desc, maxText), textX, cy + 32, GuiTheme.MUTED);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || onSelect == null) return false;
        for (int i = 0; i < MODES.length; i++) {
            if (GuiDraw.hit(mx, my, cardX[i], cardY[i], cardW, GuiTheme.CARD_H)) {
                onSelect.accept(MODES[i]);
                return true;
            }
        }
        return false;
    }
}
