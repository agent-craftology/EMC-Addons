package com.emcaddons.gui.clickgui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public abstract class SettingRow {
    protected int x;
    protected int y;
    protected int w;

    public void setPosition(int x, int y, int w) {
        this.x = x;
        this.y = y;
        this.w = w;
        onLayout();
    }

    protected void onLayout() {}

    public abstract int getHeight();

    public abstract void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY);

    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        return false;
    }

    public boolean mouseDragged(double mx, double my, int button) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double vertical) {
        return false;
    }

    public void unfocus() {}

    public void flush() {}

    public boolean isCapturingKey() {
        return false;
    }
}
