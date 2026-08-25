package com.emcaddons.gui.clickgui.widget;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Clickable module cards in a wrapping grid. Title plus optional muted
 * description; no on/off toggle.
 */
public final class ModuleGridRow extends SettingRow {
    public static final class Card {
        public final String title;
        public final String description;
        public final Runnable onClick;

        public Card(String title, String description, Runnable onClick) {
            this.title = title == null ? "" : title;
            this.description = description == null ? "" : description;
            this.onClick = onClick;
        }
    }

    private final Card[] cards;
    private int cols = 1;
    private int cardW = GuiTheme.CARD_W;
    private int gridH = GuiTheme.CARD_H;

    public ModuleGridRow(Card... cards) {
        this.cards = cards == null ? new Card[0] : cards;
    }

    @Override
    protected void onLayout() {
        int gap = GuiTheme.CARD_GAP;
        int n = cards.length;
        cols = Math.min(3, Math.max(1, n));
        while (cols > 1 && (w - (cols - 1) * gap) / cols < 80) {
            cols--;
        }
        cardW = Math.max(1, (w - (cols - 1) * gap) / cols);
        int rows = n == 0 ? 1 : (n + cols - 1) / cols;
        gridH = rows * GuiTheme.CARD_H + Math.max(0, rows - 1) * gap;
    }

    @Override
    public int getHeight() {
        return Math.max(GuiTheme.CARD_H, gridH);
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        int gap = GuiTheme.CARD_GAP;
        int h = GuiTheme.CARD_H;
        for (int i = 0; i < cards.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (cardW + gap);
            int cy = y + row * (h + gap);
            Card card = cards[i];
            boolean hover = GuiDraw.hit(mouseX, mouseY, cx, cy, cardW, h);
            GuiDraw.fillRoundRect(ctx, cx, cy, cardW, h, GuiTheme.CARD_RADIUS,
                    hover ? GuiTheme.CARD_HOVER : GuiTheme.ROW);
            if (hover) {
                GuiDraw.fill(ctx, cx, cy + 8, 2, h - 16, GuiTheme.ACCENT);
            }
            int textX = cx + 12;
            int maxText = Math.max(8, cardW - 24);
            boolean hasDesc = !card.description.isBlank();
            if (hasDesc) {
                GuiDraw.text(ctx, tr, GuiDraw.ellipsize(tr, card.title, maxText), textX, cy + 16, GuiTheme.TITLE);
                GuiDraw.text(ctx, tr, GuiDraw.ellipsize(tr, card.description, maxText), textX, cy + 32, GuiTheme.MUTED);
            } else {
                GuiDraw.text(ctx, tr, GuiDraw.ellipsize(tr, card.title, maxText),
                        textX, cy + (h - 9) / 2, GuiTheme.TITLE);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        int gap = GuiTheme.CARD_GAP;
        int h = GuiTheme.CARD_H;
        for (int i = 0; i < cards.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (cardW + gap);
            int cy = y + row * (h + gap);
            if (GuiDraw.hit(mx, my, cx, cy, cardW, h)) {
                if (cards[i].onClick != null) cards[i].onClick.run();
                return true;
            }
        }
        return false;
    }
}
