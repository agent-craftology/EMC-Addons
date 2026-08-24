package com.emcaddons.gui.clickgui.widget;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class KeybindRow extends SettingRow {
    private final String label;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final boolean escClears;
    private boolean capturing;

    public KeybindRow(String label, IntSupplier getter, IntConsumer setter, boolean escClears) {
        this.label = label;
        this.getter = getter;
        this.setter = setter;
        this.escClears = escClears;
    }

    @Override
    public int getHeight() {
        return GuiTheme.ROW_H;
    }

    @Override
    public boolean isCapturingKey() {
        return capturing;
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, Font tr, int mouseX, int mouseY) {
        boolean hover = GuiDraw.hit(mouseX, mouseY, x, y, w, getHeight());
        GuiDraw.fillRoundRect(ctx, x, y, w, getHeight(), 6, hover || capturing ? GuiTheme.CARD_HOVER : GuiTheme.ROW);
        GuiDraw.text(ctx, tr, label, x + 10, y + 12, GuiTheme.TITLE);

        String text = capturing ? "Press key..." : keyName(getter.getAsInt());
        int bw = Math.max(70, tr.width(text) + 16);
        int bx = x + w - bw - 10;
        int by = y + 6;
        GuiDraw.fillRoundRect(ctx, bx, by, bw, 20, 5, capturing ? GuiTheme.ACCENT_SOFT : GuiTheme.PILL);
        GuiDraw.textCentered(ctx, tr, text, bx + bw / 2, by + 6, capturing ? GuiTheme.ACCENT : GuiTheme.TITLE);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        if (!GuiDraw.hit(mx, my, x, y, w, getHeight())) {
            capturing = false;
            return false;
        }
        capturing = true;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!capturing) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (escClears) setter.accept(0);
            capturing = false;
            return true;
        }
        setter.accept(keyCode);
        capturing = false;
        return true;
    }

    public static String keyName(int key) {
        if (key == 0) return "None";
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name == null) return "Key " + key;
        if (name.length() == 1) return name.toUpperCase();
        return name;
    }
}
