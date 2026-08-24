package com.emcaddons.gui.clickgui.widget;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Supplier;

public final class LabelRow extends SettingRow {
    private final String label;
    private final Supplier<String> value;

    public LabelRow(String label, Supplier<String> value) {
        this.label = label;
        this.value = value;
    }

    @Override
    public int getHeight() {
        return GuiTheme.ROW_H;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        GuiDraw.fillRoundRect(ctx, x, y, w, getHeight(), 6, GuiTheme.ROW);
        GuiDraw.text(ctx, tr, label, x + 10, y + 12, GuiTheme.TITLE);
        String text = value.get();
        if (text == null) text = "";
        GuiDraw.text(ctx, tr, text, x + w - tr.getWidth(text) - 10, y + 12, GuiTheme.MUTED);
    }
}
