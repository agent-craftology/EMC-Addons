package com.emcaddons.gui.clickgui;

import com.emcaddons.EmcAddonsClient;
import com.emcaddons.config.ConfigProfileManager;
import com.emcaddons.config.ConfigShare;
import com.emcaddons.gui.HudEditScreen;
import com.emcaddons.gui.clickgui.widget.ButtonRow;
import com.emcaddons.gui.clickgui.widget.HeadingRow;
import com.emcaddons.gui.clickgui.widget.KeybindRow;
import com.emcaddons.gui.clickgui.widget.LabelRow;
import com.emcaddons.gui.clickgui.widget.ModeRow;
import com.emcaddons.gui.clickgui.widget.ProfileListWidget;
import com.emcaddons.gui.clickgui.widget.SettingRow;
import com.emcaddons.gui.clickgui.widget.SliderRow;
import com.emcaddons.gui.clickgui.widget.TextRow;
import com.emcaddons.gui.clickgui.widget.ToggleRow;
import com.emcaddons.scoreboard.EmcStatsScoreboard;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

final class ClickGuiPages {
    private ClickGuiPages() {}

    static List<SettingRow> settings(EmcAddonsClient mod) {
        List<SettingRow> rows = new ArrayList<>();
        rows.add(new HeadingRow("APPEARANCE"));
        rows.add(new ModeRow("Theme", GuiTheme.Theme.displayNames(),
                () -> mod.getGuiTheme().ordinal(),
                i -> mod.setGuiTheme(GuiTheme.Theme.fromIndex(i))));
        rows.add(new SliderRow("GUI Opacity", GuiTheme.OPACITY_MIN, GuiTheme.OPACITY_MAX, 1, "%",
                mod::getGuiOpacity, mod::setGuiOpacity));
        rows.add(new SliderRow("GUI scale", GuiScale.MIN_PCT, GuiScale.MAX_PCT, GuiScale.STEP_PCT, "%",
                mod::getClickGuiScalePercent, mod::setClickGuiScalePercent));
        rows.add(new ToggleRow("Window icon", mod::isWindowIconEnabled, mod::setWindowIconEnabled));
        rows.add(new HeadingRow("KEYBINDS"));
        rows.add(new KeybindRow("Open menu", mod::getGuiOpenMenuKey, mod::setGuiOpenMenuKey, false));
        return rows;
    }

    static List<SettingRow> config(EmcAddonsClient mod, ClickGuiScreen gui, Supplier<String> search) {
        List<SettingRow> rows = new ArrayList<>();
        ConfigProfileManager manager = mod.getConfigProfileManager();
        rows.add(new HeadingRow("PROFILES"));
        rows.add(new LabelRow("Active", () -> {
            if (manager == null) return "—";
            String active = manager.getActiveProfileName();
            return active == null || active.isBlank() ? "default" : active;
        }));
        TextRow nameRow = new TextRow("New profile", "", 32, false, "A-Z 0-9 _ -", null);
        nameRow.field().setFilter(c -> Character.isLetterOrDigit(c) || c == '_' || c == '-');
        rows.add(nameRow);
        rows.add(new ButtonRow("Create", "Create", () -> {
            String name = nameRow.field().getText().trim();
            if (name.isEmpty()) {
                gui.setConfigStatus("Profile name is required");
                return;
            }
            try {
                mod.createConfigProfile(name);
                gui.setConfigStatus("Created " + name);
                Minecraft.getInstance().execute(gui::refreshPage);
            } catch (Exception e) {
                gui.setConfigStatus(e.getMessage() != null ? e.getMessage() : "Create failed");
            }
        }));
        rows.add(new ButtonRow("Import", "Import .cbshare...", () -> {
            if (manager == null) {
                gui.setConfigStatus("Config manager is missing");
                return;
            }
            File startDir = ConfigShare.defaultExportDir(manager.getBaseConfigDir());
            ConfigShare.chooseImportFileAsync(startDir, file -> {
                String result = ConfigShare.importPreset(manager, file);
                Minecraft.getInstance().execute(() -> {
                    if (ConfigShare.isError(result)) {
                        gui.setConfigStatus(ConfigShare.errorMessage(result));
                    } else {
                        gui.setConfigStatus("Imported " + result);
                        gui.refreshPage();
                    }
                });
            }, () -> Minecraft.getInstance().execute(() -> gui.setConfigStatus("Import cancelled")));
        }));
        rows.add(new LabelRow("Status", gui::getConfigStatus));
        rows.add(new ProfileListWidget(mod, gui, search));
        return rows;
    }

    static List<SettingRow> hud(EmcAddonsClient mod, ClickGuiScreen gui) {
        List<SettingRow> rows = new ArrayList<>();
        rows.add(new ToggleRow("Show HUD", () -> mod.getHudLayoutManager().isMasterVisible(), v -> {
            mod.getHudLayoutManager().setMasterVisible(v);
            mod.persistHudLayout();
        }));
        rows.add(new ToggleRow("EMC Stats card visible", () -> {
            var c = mod.getHudLayoutManager().get("emcstats");
            return c != null && c.isVisible();
        }, v -> {
            var c = mod.getHudLayoutManager().get("emcstats");
            if (c != null) c.setVisible(v);
            mod.persistHudLayout();
        }));
        rows.add(new ToggleRow("Advanced stats", () -> {
            var c = mod.getHudLayoutManager().get("emcstats");
            return c != null && c.isAdvanced();
        }, v -> {
            var c = mod.getHudLayoutManager().get("emcstats");
            if (c != null) c.setAdvanced(v);
            mod.persistHudLayout();
        }));
        rows.add(new ButtonRow("EMC Stats rows", "Open...", () -> gui.openPage(ClickGuiScreen.Page.HUD_ROWS)));
        rows.add(new ButtonRow("HUD layout", () -> "Edit layout...",
                () -> Minecraft.getInstance().gui.setScreen(
                        new HudEditScreen(mod.getHudLayoutManager(), mod::persistHudLayout)),
                () -> true, null));
        rows.add(new ButtonRow("Reset", () -> "Reset positions",
                () -> {
                    mod.getHudLayoutManager().resetPositions();
                    mod.persistHudLayout();
                }, () -> true, null));
        return rows;
    }

    static List<SettingRow> hudRows(EmcAddonsClient mod) {
        List<SettingRow> rows = new ArrayList<>();
        EmcStatsScoreboard sb = mod.getEmcStatsScoreboard();
        for (EmcStatsScoreboard.HudStat stat : EmcStatsScoreboard.HudStat.values()) {
            EmcStatsScoreboard.HudStat hudStat = stat;
            rows.add(new ToggleRow(hudStat.label, () -> sb.isHudStatVisible(hudStat), v -> {
                sb.setHudStatVisible(hudStat, v);
                mod.persistHudLayout();
            }));
        }
        EmcStatsScoreboard.Currency[] currencies = EmcStatsScoreboard.Currency.values();
        String[] labels = new String[currencies.length];
        for (int i = 0; i < currencies.length; i++) {
            String name = currencies[i].name();
            labels[i] = name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
        }
        rows.add(new ModeRow("Graph currency", labels,
                () -> {
                    EmcStatsScoreboard.Currency c = sb.getGraphCurrency();
                    for (int i = 0; i < currencies.length; i++) if (currencies[i] == c) return i;
                    return 0;
                },
                i -> {
                    if (i >= 0 && i < currencies.length) sb.setGraphCurrency(currencies[i]);
                    mod.persistHudLayout();
                }));
        return rows;
    }
}
