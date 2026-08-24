package com.emcaddons.gui.clickgui.widget;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class ModeRow extends SettingRow {
    private final String label;
    private final String[] options;
    private final IntSupplier getter;
    private final IntConsumer setter;

    private int pillW = 80;
    private static final int GAP = 4;

    public ModeRow(String label, String[] options, IntSupplier getter, IntConsumer setter) {
        this.label = label;
        this.options = options;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public int getHeight() {
        return GuiTheme.ROW_H + 4;
    }

    private int startX() {
        int total = options.length * pillW + (options.length - 1) * GAP;
        return x + w - 10 - total;
    }

    private void layoutPills(TextRenderer tr) {
        int labelW = tr.getWidth(label) + 24;
        int available = Math.max(40, w - labelW - 10);
        int maxText = 0;
        for (String option : options) maxText = Math.max(maxText, tr.getWidth(option));
        int desired = maxText + 12;
        int fit = (available - (options.length - 1) * GAP) / Math.max(1, options.length);
        pillW = Math.max(36, Math.min(desired, fit));
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        layoutPills(tr);
        GuiDraw.fillRoundRect(ctx, x, y, w, getHeight(), 6, GuiTheme.ROW);
        GuiDraw.text(ctx, tr, label, x + 10, y + 14, GuiTheme.TITLE);

        int selected = getter.getAsInt();
        int start = startX();
        int py = y + 8;
        for (int i = 0; i < options.length; i++) {
            int px = start + i * (pillW + GAP);
            boolean on = i == selected;
            boolean hover = GuiDraw.hit(mouseX, mouseY, px, py, pillW, 20);
            int bg = on ? GuiTheme.ACCENT_SOFT : (hover ? GuiTheme.CARD_HOVER : GuiTheme.PILL);
            GuiDraw.fillRoundRect(ctx, px, py, pillW, 20, 6, bg);
            if (on) {
                GuiDraw.fill(ctx, px, py + 4, 2, 12, GuiTheme.ACCENT);
            }
            GuiDraw.textCentered(ctx, tr, options[i], px + pillW / 2, py + 6, on ? GuiTheme.ACCENT : GuiTheme.TITLE);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        int start = startX();
        int py = y + 8;
        for (int i = 0; i < options.length; i++) {
            int px = start + i * (pillW + GAP);
            if (GuiDraw.hit(mx, my, px, py, pillW, 20)) {
                setter.accept(i);
                return true;
            }
        }
        return false;
    }
}
