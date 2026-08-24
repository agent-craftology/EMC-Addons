package com.emcaddons.gui.clickgui.widget;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class ToggleRow extends SettingRow {
    private final String label;
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    public ToggleRow(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        this.label = label;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public int getHeight() {
        return GuiTheme.ROW_H;
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, Font tr, int mouseX, int mouseY) {
        boolean on = getter.getAsBoolean();
        boolean hover = GuiDraw.hit(mouseX, mouseY, x, y, w, getHeight());
        GuiDraw.fillRoundRect(ctx, x, y, w, getHeight(), 6, hover ? GuiTheme.CARD_HOVER : GuiTheme.ROW);
        GuiDraw.text(ctx, tr, label, x + 10, y + 12, GuiTheme.TITLE);

        int tw = 28;
        int th = 14;
        int tx = x + w - tw - 10;
        int ty = y + (getHeight() - th) / 2;
        GuiDraw.fillRoundRect(ctx, tx, ty, tw, th, 7, on ? GuiTheme.ACCENT : GuiTheme.TRACK);
        int knob = 10;
        int kx = on ? tx + tw - knob - 2 : tx + 2;
        GuiDraw.fillRoundRect(ctx, kx, ty + 2, knob, knob, 5, GuiTheme.KNOB);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        if (!GuiDraw.hit(mx, my, x, y, w, getHeight())) return false;
        setter.accept(!getter.getAsBoolean());
        return true;
    }
}
