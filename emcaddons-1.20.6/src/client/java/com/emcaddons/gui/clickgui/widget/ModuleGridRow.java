package com.emcaddons.gui.clickgui.widget;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Clickable module cards laid out in a wrapping grid.
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

    private final List<Card> cards;
    private final List<Hit> hits = new ArrayList<>();
    private int cols = 1;
    private int cardW = GuiTheme.CARD_W;

    public ModuleGridRow(List<Card> cards) {
        this.cards = cards == null ? List.of() : List.copyOf(cards);
    }

    @Override
    protected void onLayout() {
        int gap = GuiTheme.CARD_GAP;
        cols = Math.min(3, Math.max(1, (w + gap) / (GuiTheme.CARD_W + gap)));
        cardW = Math.max(1, (w - (cols - 1) * gap) / cols);
        hits.clear();
        for (int i = 0; i < cards.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (cardW + gap);
            int cy = y + row * (GuiTheme.CARD_H + gap);
            hits.add(new Hit(cx, cy, cardW, GuiTheme.CARD_H));
        }
    }

    @Override
    public int getHeight() {
        if (cards.isEmpty()) return 0;
        int rows = (cards.size() + cols - 1) / cols;
        return rows * GuiTheme.CARD_H + (rows - 1) * GuiTheme.CARD_GAP;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        onLayout();
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            Hit hit = hits.get(i);
            boolean hover = GuiDraw.hit(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h);
            GuiDraw.fillRoundRect(ctx, hit.x, hit.y, hit.w, hit.h, GuiTheme.CARD_RADIUS,
                    hover ? GuiTheme.CARD_HOVER : GuiTheme.ROW);
            if (hover) {
                GuiDraw.fill(ctx, hit.x, hit.y + 8, 2, hit.h - 16, GuiTheme.ACCENT);
            }
            int textX = hit.x + 12;
            int maxText = Math.max(8, hit.w - 24);
            GuiDraw.text(ctx, tr, GuiDraw.ellipsize(tr, card.title, maxText), textX, hit.y + 16, GuiTheme.TITLE);
            GuiDraw.text(ctx, tr, GuiDraw.ellipsize(tr, card.description, maxText), textX, hit.y + 32, GuiTheme.MUTED);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        onLayout();
        for (int i = 0; i < cards.size(); i++) {
            Hit hit = hits.get(i);
            if (GuiDraw.hit(mx, my, hit.x, hit.y, hit.w, hit.h)) {
                Runnable click = cards.get(i).onClick;
                if (click != null) click.run();
                return true;
            }
        }
        return false;
    }

    private record Hit(int x, int y, int w, int h) {}
}
