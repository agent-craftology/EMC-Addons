package com.emcaddons.gui.clickgui;

import com.emcaddons.EmcAddonsClient;
import com.emcaddons.config.ConfigProfileManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Scrollable profile list matching Click GUI row chrome.
 */
public final class ConfigListPanel {

    private static final int ROW_H = 26;

    private final EmcAddonsClient mod;
    private final List<String> names = new ArrayList<>();
    private String selected;
    private int scroll;
    private int x;
    private int y;
    private int w;
    private int h;

    public ConfigListPanel(EmcAddonsClient mod) {
        this.mod = mod;
    }

    public void rebuild() {
        names.clear();
        ConfigProfileManager manager = mod.getConfigProfileManager();
        if (manager == null) {
            return;
        }
        names.addAll(manager.listProfiles());
        if (selected == null || !names.contains(selected)) {
            selected = manager.getActiveProfileName();
            if (selected != null && !names.contains(selected) && !names.isEmpty()) {
                selected = names.get(0);
            }
        }
        int visible = Math.max(1, h / ROW_H);
        if (scroll > Math.max(0, names.size() - visible)) {
            scroll = Math.max(0, names.size() - visible);
        }
    }

    public void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public String getSelected() {
        return selected;
    }

    public void setSelected(String name) {
        this.selected = name;
    }

    public void render(GuiDraw d, int mouseX, int mouseY) {
        ConfigProfileManager manager = mod.getConfigProfileManager();
        String active = manager != null ? manager.getActiveProfileName() : null;
        d.enableScissor(x, y, w, h);
        if (names.isEmpty()) {
            d.text("No configs yet", x + 8, y + 8, GuiTheme.MUTED);
            d.disableScissor();
            return;
        }
        int visible = Math.max(0, h / ROW_H);
        for (int i = 0; i < visible && i + scroll < names.size(); i++) {
            String name = names.get(i + scroll);
            int rowY = y + i * ROW_H;
            boolean isSelected = selected != null && selected.equals(name);
            boolean isActive = active != null && active.equals(name);
            boolean hover = GuiTheme.hit(x, rowY, w, ROW_H - 2, mouseX, mouseY);
            d.fillRoundRect(x, rowY, w, ROW_H - 2, 5, hover ? GuiTheme.ROW_HOVER : GuiTheme.ROW);
            if (isSelected) {
                d.fill(x, rowY + 5, 2, ROW_H - 12, GuiTheme.ACCENT);
            }
            String label = (isActive ? "(active) " : "") + name;
            int color = isActive || isSelected ? GuiTheme.ACCENT : GuiTheme.TITLE;
            d.text(d.ellipsize(label, w - 16), x + 8, rowY + 8, color);
        }
        d.disableScissor();
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!GuiTheme.hit(x, y, w, h, mouseX, mouseY)) {
            return false;
        }
        int visible = Math.max(0, h / ROW_H);
        for (int i = 0; i < visible && i + scroll < names.size(); i++) {
            String name = names.get(i + scroll);
            int rowY = y + i * ROW_H;
            if (GuiTheme.hit(x, rowY, w, ROW_H - 2, mouseX, mouseY)) {
                selected = name;
                return true;
            }
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!GuiTheme.hit(x, y, w, h, mouseX, mouseY)) {
            return false;
        }
        int visible = Math.max(1, h / ROW_H);
        if (amount < 0) {
            if (scroll < names.size() - visible) {
                scroll++;
            }
        } else if (amount > 0 && scroll > 0) {
            scroll--;
        }
        return true;
    }

    public void resetScroll() {
        scroll = 0;
    }
}
