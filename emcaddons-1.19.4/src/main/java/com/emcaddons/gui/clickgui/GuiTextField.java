package com.emcaddons.gui.clickgui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Styled text field: vanilla TextFieldWidget for input, custom rounded chrome for look.
 */
public final class GuiTextField {

    private final TextFieldWidget inner;
    private final String placeholder;
    private int x;
    private int y;
    private int w;
    private int h;

    public GuiTextField(TextRenderer tr, int width, int height, int maxLength, String text, String placeholder) {
        this.w = Math.max(20, width);
        this.h = Math.max(16, height);
        this.placeholder = placeholder;
        this.inner = new TextFieldWidget(tr, 0, 0, this.w - 12, 12, Text.literal(""));
        this.inner.setMaxLength(maxLength);
        this.inner.setDrawsBackground(false);
        this.inner.setEditableColor(GuiTheme.TITLE);
        this.inner.setText(text == null ? "" : text);
        refreshSuggestion();
    }

    public void setChangedListener(Consumer<String> listener) {
        inner.setChangedListener(value -> {
            refreshSuggestion();
            if (listener != null) listener.accept(value);
        });
    }

    private void refreshSuggestion() {
        if (placeholder != null && inner.getText().isEmpty()) {
            inner.setSuggestion(placeholder);
        } else {
            inner.setSuggestion(null);
        }
    }

    public void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        if (w > 12) {
            this.w = w;
            inner.setWidth(w - 12);
        }
        if (h > 8) this.h = h;
        inner.setX(x + 6);
        inner.setY(y + Math.max(0, (this.h - 9) / 2));
    }

    public void render(GuiDraw d, int mouseX, int mouseY, float delta) {
        boolean hover = GuiTheme.hit(x, y, this.w, this.h, mouseX, mouseY) || inner.isFocused();
        d.fillRoundRect(x, y, this.w, this.h, 5, hover ? GuiTheme.PILL : GuiTheme.FIELD);
        if (inner.isFocused()) {
            d.fill(x + 2, y + 2, 2, this.h - 4, GuiTheme.ACCENT);
        }
        inner.render(d.matrices(), mouseX, mouseY, delta);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return inner.mouseClicked(mouseX, mouseY, button);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return inner.isFocused() && inner.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        return inner.isFocused() && inner.charTyped(chr, modifiers);
    }

    public void tick() {
        inner.tick();
    }

    public String getText() {
        return inner.getText();
    }

    public void setText(String text) {
        inner.setText(text == null ? "" : text);
        refreshSuggestion();
    }

    public boolean isFocused() {
        return inner.isFocused();
    }

    public void setFocused(boolean focused) {
        inner.setFocused(focused);
    }

    public boolean hit(double mx, double my) {
        return GuiTheme.hit(x, y, w, h, mx, my);
    }
}
