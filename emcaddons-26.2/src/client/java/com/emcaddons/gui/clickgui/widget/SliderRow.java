package com.emcaddons.gui.clickgui.widget;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class SliderRow extends SettingRow {
    private final String label;
    private final int min;
    private final int max;
    private final int step;
    private final String suffix;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private boolean dragging;

    public SliderRow(String label, int min, int max, int step, String suffix, IntSupplier getter, IntConsumer setter) {
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.suffix = suffix == null ? "" : suffix;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public int getHeight() {
        return GuiTheme.SLIDER_H;
    }

    private int trackX() {
        return x + 10;
    }

    private int trackY() {
        return y + 24;
    }

    private int trackW() {
        return w - 20;
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, Font tr, int mouseX, int mouseY) {
        boolean hover = GuiDraw.hit(mouseX, mouseY, x, y, w, getHeight());
        GuiDraw.fillRoundRect(ctx, x, y, w, getHeight(), 6, hover || dragging ? GuiTheme.CARD_HOVER : GuiTheme.ROW);
        int value = getter.getAsInt();
        GuiDraw.text(ctx, tr, label, x + 10, y + 6, GuiTheme.TITLE);
        String valText = value + suffix;
        GuiDraw.text(ctx, tr, valText, x + w - 10 - tr.width(valText), y + 6, GuiTheme.ACCENT);

        int tx = trackX();
        int ty = trackY();
        int tw = trackW();
        GuiDraw.fillRoundRect(ctx, tx, ty, tw, 6, 3, GuiTheme.TRACK);
        double t = max == min ? 0 : (value - min) / (double) (max - min);
        t = Math.max(0, Math.min(1, t));
        int fillW = (int) Math.round(t * tw);
        if (fillW > 0) {
            GuiDraw.fillRoundRect(ctx, tx, ty, Math.max(6, fillW), 6, 3, GuiTheme.ACCENT);
        }
        int kx = tx + (int) Math.round(t * (tw - 8));
        GuiDraw.fillRoundRect(ctx, kx, ty - 3, 8, 12, 4, GuiTheme.KNOB);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        if (!GuiDraw.hit(mx, my, x, y, w, getHeight())) return false;
        dragging = true;
        applyMouse(mx);
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button) {
        if (!dragging) return false;
        applyMouse(mx);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    public boolean isDragging() {
        return dragging;
    }

    private void applyMouse(double mx) {
        double t = (mx - trackX()) / (double) trackW();
        t = Math.max(0, Math.min(1, t));
        int raw = min + (int) Math.round(t * (max - min));
        int snapped = min + (int) Math.round((raw - min) / (double) step) * step;
        snapped = Math.max(min, Math.min(max, snapped));
        setter.accept(snapped);
    }
}
