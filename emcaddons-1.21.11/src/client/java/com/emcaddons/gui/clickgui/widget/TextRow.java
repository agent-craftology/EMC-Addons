package com.emcaddons.gui.clickgui.widget;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TextRow extends SettingRow {
    private final String label;
    private final GuiTextField field;
    private final Consumer<String> onFlush;

    public TextRow(String label, String initial, int maxLen, boolean numeric, String placeholder, Consumer<String> onChange) {
        this(label, initial, maxLen, numeric, placeholder, onChange, null);
    }

    public TextRow(String label, String initial, int maxLen, boolean numeric, String placeholder,
                   Consumer<String> onChange, Consumer<String> onFlush) {
        this.label = label;
        this.onFlush = onFlush;
        this.field = new GuiTextField();
        this.field.setMaxLength(maxLen);
        this.field.setNumeric(numeric);
        this.field.setPlaceholder(placeholder);
        this.field.setText(initial == null ? "" : initial);
        if (onChange != null) this.field.setChangedListener(onChange);
    }

    public GuiTextField field() {
        return field;
    }

    @Override
    public int getHeight() {
        return GuiTheme.ROW_H;
    }

    @Override
    protected void onLayout() {
        int fieldW = Math.min(220, Math.max(80, w / 2));
        field.setBounds(x + w - fieldW - 10, y + 5, fieldW, 22);
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        GuiDraw.fillRoundRect(ctx, x, y, w, getHeight(), 6, GuiTheme.ROW);
        GuiDraw.text(ctx, tr, label, x + 10, y + 12, GuiTheme.TITLE);
        field.render(ctx, tr, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return field.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return field.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return field.charTyped(chr, modifiers);
    }

    @Override
    public void unfocus() {
        field.setFocused(false);
    }

    @Override
    public void flush() {
        if (onFlush != null) onFlush.accept(field.getText());
    }
}
