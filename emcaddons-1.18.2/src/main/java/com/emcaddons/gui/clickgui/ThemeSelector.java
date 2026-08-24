package com.emcaddons.gui.clickgui;

import com.emcaddons.EmcAddonsClient;

/**
 * Compact cycle control matching the Settings "Theme" row, for use on every custom screen.
 */
public final class ThemeSelector {
    public static final int H = 22;

    private int x;
    private int y;
    private int w = 80;

    public int width() {
        return w;
    }

    public void layoutRight(int right, int y, GuiDraw d) {
        String label = label();
        this.w = d.width(label) + 20;
        this.x = right - this.w;
        this.y = y;
    }

    public void render(GuiDraw d, int mouseX, int mouseY) {
        boolean hover = GuiTheme.hit(x, y, w, H, mouseX, mouseY);
        d.fillRoundRect(x, y, w, H, 6, hover ? GuiTheme.ACCENT_SOFT : GuiTheme.PILL);
        d.textCenter(label(), x + w / 2, y + 7, GuiTheme.ACCENT);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !GuiTheme.hit(x, y, w, H, mx, my)) return false;
        cycle();
        return true;
    }

    public static String label() {
        return "Theme  " + currentName();
    }

    public static String currentName() {
        EmcAddonsClient mod = EmcAddonsClient.getInstance();
        GuiTheme.Theme theme = mod != null ? mod.getGuiTheme() : GuiTheme.current();
        return theme == null ? GuiTheme.Theme.EMERALD.displayName : theme.displayName;
    }

    public static void cycle() {
        EmcAddonsClient mod = EmcAddonsClient.getInstance();
        if (mod == null) return;
        GuiTheme.Theme[] all = GuiTheme.Theme.values();
        GuiTheme.Theme current = mod.getGuiTheme();
        int idx = current == null ? 0 : current.ordinal();
        mod.setGuiTheme(all[(idx + 1) % all.length]);
    }
}
