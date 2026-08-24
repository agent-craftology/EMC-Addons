package com.emcaddons.gui.clickgui.widget;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class ButtonRow extends SettingRow {
    private final String label;
    private final java.util.function.Supplier<String> buttonText;
    private final Runnable onClick;
    private final java.util.function.BooleanSupplier enabled;
    private final java.util.function.Supplier<Chip> chip;
    private int btnX;
    private int btnY;
    private int btnW;

    public ButtonRow(String label, String buttonText, Runnable onClick) {
        this(label, () -> buttonText, onClick, () -> true, null);
    }

    public ButtonRow(String label, java.util.function.Supplier<String> buttonText, Runnable onClick,
                     java.util.function.BooleanSupplier enabled, java.util.function.Supplier<Chip> chip) {
        this.label = label;
        this.buttonText = buttonText;
        this.onClick = onClick;
        this.enabled = enabled;
        this.chip = chip;
    }

    @Override
    public int getHeight() {
        return GuiTheme.ROW_H;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        GuiDraw.fillRoundRect(ctx, x, y, w, getHeight(), 6, GuiTheme.ROW);
        GuiDraw.text(ctx, tr, label, x + 10, y + 12, GuiTheme.TITLE);

        if (chip != null) {
            Chip c = chip.get();
            if (c != null) {
                int cw = tr.getWidth(c.text) + 12;
                int cx = x + tr.getWidth(label) + 24;
                GuiDraw.fillRoundRect(ctx, cx, y + 8, cw, 16, 8, 0x33000000 | (c.color & 0x00FFFFFF));
                GuiDraw.textCentered(ctx, tr, c.text, cx + cw / 2, y + 12, c.color);
            }
        }

        String text = buttonText.get();
        btnW = Math.max(64, tr.getWidth(text) + 16);
        btnX = x + w - btnW - 10;
        btnY = y + 6;
        boolean hover = GuiDraw.hit(mouseX, mouseY, btnX, btnY, btnW, 20);
        boolean on = enabled.getAsBoolean();
        int bg = !on ? GuiTheme.TRACK : (hover ? GuiTheme.ACCENT_SOFT : GuiTheme.PILL);
        GuiDraw.fillRoundRect(ctx, btnX, btnY, btnW, 20, 5, bg);
        GuiDraw.textCentered(ctx, tr, text, btnX + btnW / 2, btnY + 6, on ? GuiTheme.TITLE : GuiTheme.MUTED);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !enabled.getAsBoolean()) return false;
        if (GuiDraw.hit(mx, my, btnX, btnY, btnW, 20)) {
            onClick.run();
            return true;
        }
        return false;
    }

    public record Chip(String text, int color) {}
}
