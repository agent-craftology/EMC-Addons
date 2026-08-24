package com.emcaddons.gui.clickgui.widget;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Section title inside a ClickGUI settings list (Cadia/Complex path screens mapped as groups).
 */
public final class HeadingRow extends SettingRow {
    private final String label;

    public HeadingRow(String label) {
        this.label = label;
    }

    @Override
    public int getHeight() {
        return 18;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY) {
        GuiDraw.text(ctx, tr, label, x + 2, y + 5, GuiTheme.MUTED);
    }
}
