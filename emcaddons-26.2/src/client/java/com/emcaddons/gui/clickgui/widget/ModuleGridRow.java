package com.emcaddons.gui.clickgui.widget;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public final class ModuleGridRow extends SettingRow {
    public static final class Card {
        public final String title;
        public final String description;
        public final Runnable onClick;

        public Card(String title, String description, Runnable onClick) {
            this.title = title;
            this.description = description == null ? "" : description;
            this.onClick = onClick;
        }
    }

    private final List<Card> cards;
    private int cols = 1;
    private int cardW = GuiTheme.CARD_W;
    private int gridH = GuiTheme.CARD_H;

    public ModuleGridRow(List<Card> cards) {
        this.cards = cards;
    }

    @Override
    protected void onLayout() {
        int gap = GuiTheme.CARD_GAP;
        cols = 3;
        cardW = Math.max(1, (w - (cols - 1) * gap) / cols);
        int rows = cards.isEmpty() ? 0 : (cards.size() + cols - 1) / cols;
        gridH = rows <= 0 ? 0 : rows * GuiTheme.CARD_H + (rows - 1) * gap;
    }

    @Override
    public int getHeight() {
        return gridH;
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, Font tr, int mouseX, int mouseY) {
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            int cx = cardX(i);
            int cy = cardY(i);
            boolean hover = GuiDraw.hit(mouseX, mouseY, cx, cy, cardW, GuiTheme.CARD_H);
            GuiDraw.fillRoundRect(ctx, cx, cy, cardW, GuiTheme.CARD_H, GuiTheme.CARD_RADIUS,
                    hover ? GuiTheme.CARD_HOVER : GuiTheme.ROW);
            if (hover) {
                GuiDraw.fill(ctx, cx, cy + 10, 2, GuiTheme.CARD_H - 20, GuiTheme.ACCENT);
            }
            int textX = cx + 12;
            int titleMax = Math.max(8, cardW - 24);
            GuiDraw.text(ctx, tr, GuiDraw.ellipsize(tr, card.title, titleMax), textX, cy + 18, GuiTheme.TITLE);
            GuiDraw.text(ctx, tr, GuiDraw.ellipsize(tr, card.description, titleMax),
                    textX, cy + 32, GuiTheme.MUTED);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        for (int i = 0; i < cards.size(); i++) {
            if (GuiDraw.hit(mx, my, cardX(i), cardY(i), cardW, GuiTheme.CARD_H)) {
                Runnable click = cards.get(i).onClick;
                if (click != null) click.run();
                return true;
            }
        }
        return false;
    }

    private int cardX(int index) {
        return x + (index % cols) * (cardW + GuiTheme.CARD_GAP);
    }

    private int cardY(int index) {
        return y + (index / cols) * (GuiTheme.CARD_H + GuiTheme.CARD_GAP);
    }
}
