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
import com.emcaddons.gui.clickgui.widget.ModuleGridRow;
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
        rows.add(new KeybindRow("Toggle Dungeons HUD", mod::getHudToggleDungeonsKey, mod::setHudToggleDungeonsKey, true));
        rows.add(new KeybindRow("Toggle Gens HUD", mod::getHudToggleGensKey, mod::setHudToggleGensKey, true));
        rows.add(new KeybindRow("Toggle Factories HUD", mod::getHudToggleFactoriesKey, mod::setHudToggleFactoriesKey, true));
        rows.add(new KeybindRow("Toggle Skyblock HUD", mod::getHudToggleSkyblockKey, mod::setHudToggleSkyblockKey, true));
        rows.add(new KeybindRow("Toggle Prisons HUD", mod::getHudTogglePrisonsKey, mod::setHudTogglePrisonsKey, true));
        rows.add(new HeadingRow("HUD"));
        rows.addAll(hudControls(mod));
        return rows;
    }

    static List<SettingRow> modules(ClickGuiScreen gui) {
        List<SettingRow> rows = new ArrayList<>();
        rows.add(new ModuleGridRow(List.of(
                new ModuleGridRow.Card("Dungeons", "Track dungeon currencies, rates, and grind time",
                        () -> gui.openModule(EmcStatsScoreboard.Gamemode.DUNGEONS)),
                new ModuleGridRow.Card("Gens", "Coming Soon!",
                        () -> gui.openModule(EmcStatsScoreboard.Gamemode.GENS)),
                new ModuleGridRow.Card("Factories", "Coming Soon!",
                        () -> gui.openModule(EmcStatsScoreboard.Gamemode.FACTORIES)),
                new ModuleGridRow.Card("Skyblock", "Coming Soon!",
                        () -> gui.openModule(EmcStatsScoreboard.Gamemode.SKYBLOCK)),
                new ModuleGridRow.Card("Prisons", "Coming Soon!",
                        () -> gui.openModule(EmcStatsScoreboard.Gamemode.PRISONS))
        )));
        return rows;
    }

    static List<SettingRow> module(EmcAddonsClient mod, ClickGuiScreen gui, EmcStatsScoreboard.Gamemode mode) {
        List<SettingRow> rows = new ArrayList<>();
        EmcStatsScoreboard sb = mod.getEmcStatsScoreboard();
        EmcStatsScoreboard.Gamemode gamemode = mode == null ? EmcStatsScoreboard.Gamemode.DUNGEONS : mode;
        rows.add(new HeadingRow(gamemode.displayName.toUpperCase(Locale.ROOT)));
        if (gamemode != EmcStatsScoreboard.Gamemode.DUNGEONS) {
            rows.add(new LabelRow("Coming Soon!", () -> ""));
        } else {
            rows.addAll(trackerControls(mod, gui));
        }
        rows.add(new ButtonRow("Reset statistics", "Reset", () -> sb.resetSession(gamemode)));
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

    private static List<SettingRow> hudControls(EmcAddonsClient mod) {
        List<SettingRow> rows = new ArrayList<>();
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

    private static List<SettingRow> trackerControls(EmcAddonsClient mod, ClickGuiScreen gui) {
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
        rows.add(new ButtonRow("EMC Stats rows", "Open...", () -> gui.openPage(ClickGuiScreen.Page.SETTINGS_ROWS)));
        return rows;
    }

    static List<SettingRow> settingsRows(EmcAddonsClient mod) {
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
