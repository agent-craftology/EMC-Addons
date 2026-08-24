package com.emcaddons.gui.clickgui.widget;

import com.emcaddons.EmcAddonsClient;
import com.emcaddons.config.ConfigProfileManager;
import com.emcaddons.config.ConfigShare;
import com.emcaddons.gui.clickgui.ClickGuiScreen;
import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ProfileListWidget extends SettingRow {
    private static final int ROW_H = 24;
    private static final int LOAD_W = 40;
    private static final int EXPORT_W = 48;
    private static final int DELETE_W = 48;
    private static final int BTN_H = 18;
    private static final int BTN_GAP = 4;

    private final EmcAddonsClient mod;
    private final ClickGuiScreen gui;
    private final Supplier<String> search;
    private int fillHeight = 160;
    private int scroll;
    private final List<String> filtered = new ArrayList<>();

    public ProfileListWidget(EmcAddonsClient mod, ClickGuiScreen gui, Supplier<String> search) {
        this.mod = mod;
        this.gui = gui;
        this.search = search;
    }

    public void setFillHeight(int fillHeight) {
        this.fillHeight = Math.max(80, fillHeight);
    }

    @Override
    public int getHeight() {
        return fillHeight;
    }

    private void rebuild() {
        filtered.clear();
        ConfigProfileManager manager = mod.getConfigProfileManager();
        if (manager == null) return;
        String q = search.get() == null ? "" : search.get().toLowerCase().trim();
        for (String name : manager.listProfiles()) {
            if (q.isEmpty() || name.toLowerCase().contains(q)) {
                filtered.add(name);
            }
        }
        int visible = Math.max(1, (fillHeight - 8) / ROW_H);
        int maxScroll = Math.max(0, filtered.size() - visible);
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;
    }

    private String activeName() {
        ConfigProfileManager manager = mod.getConfigProfileManager();
        if (manager == null) return "";
        String active = manager.getActiveProfileName();
        return active == null ? "" : active;
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, Font tr, int mouseX, int mouseY) {
        rebuild();
        GuiDraw.fillRoundRect(ctx, x, y, w, fillHeight, 6, GuiTheme.ROW);
        String active = activeName();

        GuiDraw.scissor(ctx, x, y, w, fillHeight);
        int visible = Math.max(1, (fillHeight - 8) / ROW_H);
        int rowY = y + 4;
        if (filtered.isEmpty()) {
            GuiDraw.text(ctx, tr, "No profiles", x + 10, y + 14, GuiTheme.MUTED);
            GuiDraw.disableScissor(ctx);
            return;
        }
        for (int i = 0; i < visible && (i + scroll) < filtered.size(); i++) {
            String name = filtered.get(i + scroll);
            int ry = rowY + i * ROW_H;
            boolean hover = GuiDraw.hit(mouseX, mouseY, x + 4, ry, w - 8, ROW_H - 2);
            boolean activeRow = name.equals(active);
            if (activeRow || hover) {
                GuiDraw.fillRoundRect(ctx, x + 4, ry, w - 8, ROW_H - 2, 4,
                        activeRow ? GuiTheme.ACCENT_SOFT : GuiTheme.CARD_HOVER);
            }
            if (activeRow) {
                GuiDraw.fill(ctx, x + 4, ry + 4, 2, ROW_H - 10, GuiTheme.ACCENT);
            }

            int buttonsW = LOAD_W + EXPORT_W + DELETE_W + BTN_GAP * 2;
            String label = activeRow ? "(active) " + name : name;
            String shown = GuiDraw.ellipsize(tr, label, w - buttonsW - 28);
            GuiDraw.text(ctx, tr, shown, x + 12, ry + 8, activeRow ? GuiTheme.ACCENT : GuiTheme.TITLE);

            int delX = x + w - 8 - DELETE_W;
            int expX = delX - BTN_GAP - EXPORT_W;
            int loadX = expX - BTN_GAP - LOAD_W;
            int by = ry + 2;

            boolean loadH = GuiDraw.hit(mouseX, mouseY, loadX, by, LOAD_W, BTN_H);
            boolean expH = GuiDraw.hit(mouseX, mouseY, expX, by, EXPORT_W, BTN_H);
            boolean delH = GuiDraw.hit(mouseX, mouseY, delX, by, DELETE_W, BTN_H);

            GuiDraw.fillRoundRect(ctx, loadX, by, LOAD_W, BTN_H, 4,
                    activeRow ? GuiTheme.TRACK : (loadH ? GuiTheme.ACCENT_SOFT : GuiTheme.PILL));
            GuiDraw.textCentered(ctx, tr, "Load", loadX + LOAD_W / 2, by + 5,
                    activeRow ? GuiTheme.MUTED : GuiTheme.TITLE);

            GuiDraw.fillRoundRect(ctx, expX, by, EXPORT_W, BTN_H, 4, expH ? GuiTheme.ACCENT_SOFT : GuiTheme.PILL);
            GuiDraw.textCentered(ctx, tr, "Export", expX + EXPORT_W / 2, by + 5, GuiTheme.TITLE);

            GuiDraw.fillRoundRect(ctx, delX, by, DELETE_W, BTN_H, 4,
                    activeRow ? GuiTheme.TRACK : (delH ? 0x33FF5555 : GuiTheme.PILL));
            GuiDraw.textCentered(ctx, tr, "Delete", delX + DELETE_W / 2, by + 5,
                    activeRow ? GuiTheme.MUTED : GuiTheme.DANGER);
        }
        GuiDraw.disableScissor(ctx);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        if (!GuiDraw.hit(mx, my, x, y, w, fillHeight)) return false;
        rebuild();
        int visible = Math.max(1, (fillHeight - 8) / ROW_H);
        int rowY = y + 4;
        for (int i = 0; i < visible && (i + scroll) < filtered.size(); i++) {
            String name = filtered.get(i + scroll);
            int ry = rowY + i * ROW_H;
            int delX = x + w - 8 - DELETE_W;
            int expX = delX - BTN_GAP - EXPORT_W;
            int loadX = expX - BTN_GAP - LOAD_W;
            int by = ry + 2;
            boolean activeRow = name.equals(activeName());
            if (GuiDraw.hit(mx, my, loadX, by, LOAD_W, BTN_H)) {
                if (!activeRow) loadProfile(name);
                return true;
            }
            if (GuiDraw.hit(mx, my, expX, by, EXPORT_W, BTN_H)) {
                exportProfile(name, activeRow);
                return true;
            }
            if (GuiDraw.hit(mx, my, delX, by, DELETE_W, BTN_H)) {
                if (!activeRow) deleteProfile(name);
                return true;
            }
        }
        return true;
    }

    private void loadProfile(String name) {
        try {
            if (mod.loadConfigProfile(name)) {
                gui.setConfigStatus("Loaded " + name);
            } else {
                gui.setConfigStatus("Failed to load " + name);
            }
        } catch (Exception e) {
            gui.setConfigStatus(e.getMessage() != null ? e.getMessage() : "Load failed");
        }
        Minecraft.getInstance().execute(gui::refreshPage);
    }

    private void deleteProfile(String name) {
        try {
            if (mod.deleteConfigProfile(name)) {
                gui.setConfigStatus("Deleted " + name);
            } else {
                gui.setConfigStatus("Could not delete (missing or active): " + name);
            }
        } catch (Exception e) {
            gui.setConfigStatus(e.getMessage() != null ? e.getMessage() : "Delete failed");
        }
        Minecraft.getInstance().execute(gui::refreshPage);
    }

    private void exportProfile(String name, boolean active) {
        ConfigProfileManager manager = mod.getConfigProfileManager();
        if (manager == null) {
            gui.setConfigStatus("Config manager is missing");
            return;
        }
        if (active) {
            try {
                mod.persistActiveProfile();
            } catch (Exception e) {
                gui.setConfigStatus(e.getMessage() != null ? e.getMessage() : "Could not save profile");
                return;
            }
        }
        File startDir = ConfigShare.defaultExportDir(manager.getBaseConfigDir());
        ConfigShare.chooseExportFileAsync(name, startDir, dest -> {
            String err = ConfigShare.exportPreset(manager, name, dest);
            Minecraft.getInstance().execute(() -> {
                if (err == null) {
                    gui.setConfigStatus("Exported " + name + " → " + dest.getName());
                } else {
                    gui.setConfigStatus(err);
                }
            });
        }, () -> Minecraft.getInstance().execute(() -> gui.setConfigStatus("Export cancelled")));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double vertical) {
        if (!GuiDraw.hit(mx, my, x, y, w, fillHeight)) return false;
        rebuild();
        scroll -= (int) Math.round(vertical);
        int visible = Math.max(1, (fillHeight - 8) / ROW_H);
        int maxScroll = Math.max(0, filtered.size() - visible);
        if (scroll < 0) scroll = 0;
        if (scroll > maxScroll) scroll = maxScroll;
        return true;
    }
}
