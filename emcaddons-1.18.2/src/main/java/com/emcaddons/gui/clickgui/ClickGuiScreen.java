package com.emcaddons.gui.clickgui;

import com.emcaddons.EmcAddons;
import com.emcaddons.EmcAddonsClient;
import com.emcaddons.config.ConfigProfileManager;
import com.emcaddons.config.ConfigShare;
import com.emcaddons.scoreboard.EmcStatsScoreboard;
import com.emcaddons.scoreboard.HudLayoutManager;
import com.emcaddons.scoreboard.StatCard;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClickGuiScreen extends Screen {

    public enum Page {
        MODULES, SETTINGS, CONFIG, SETTINGS_ROWS, MODULE
    }

    private static final Identifier LOGO = new Identifier("emcaddons", "textures/gui/logo.png");
    private static final int LOGO_SIZE = 32;
    private static final int LOGO_TEX = 64;
    private static final String MOD_VERSION = readModVersion();

    private final EmcAddonsClient mod;
    private final List<SettingRow> rows = new ArrayList<>();
    private final List<GuiTextField> fields = new ArrayList<>();

    private Page page = Page.MODULES;
    private int contentScrollTarget;
    private final Anim scrollAnim = new Anim(0f);
    private ConfigListPanel configList;
    private String configNameDraft = "";
    private ModuleGridRow.Mode selectedMode = ModuleGridRow.Mode.DUNGEONS;

    private int winX, winY, winW, winH;
    private int contentX, contentY, contentW, contentH;
    private int navModulesY, navSettingsY, navConfigY;
    private final java.util.Map<Page, Anim> navAnims = new java.util.EnumMap<>(Page.class);

    private boolean draggingScrollbar;
    private int scrollbarX, scrollbarY, scrollbarH, thumbY, thumbH;
    private boolean scrollbarActive;
    private String hoverTooltip;
    private final ThemeSelector themeSelector = new ThemeSelector();
    private Boolean logoPresent;

    public ClickGuiScreen(EmcAddonsClient mod) {
        super(new LiteralText("EMC Addons"));
        this.mod = mod;
        this.configList = new ConfigListPanel(mod);
    }

    private void layout() {
        winW = Math.min(960, Math.max(620, width - 32));
        winH = Math.min(540, Math.max(360, height - 32));
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        contentX = winX + GuiTheme.SIDEBAR_W + GuiTheme.PAD;
        contentY = winY + GuiTheme.HEADER_H + 6;
        contentW = winW - GuiTheme.SIDEBAR_W - GuiTheme.PAD * 2;
        contentH = winH - GuiTheme.HEADER_H - GuiTheme.PAD;
    }

    @Override
    protected void init() {
        layout();
        rebuildPage();
    }

    private void setPage(Page next) {
        leavePage();
        page = next;
        contentScrollTarget = 0;
        scrollAnim.snapTo(0f);
        if (next == Page.CONFIG) {
            configList.resetScroll();
        }
        init();
    }

    private void leavePage() {
        for (SettingRow row : rows) row.onLeave();
    }

    private void rebuildPage() {
        rows.clear();
        fields.clear();

        switch (page) {
            case MODULES -> buildModules();
            case SETTINGS -> buildSettings();
            case CONFIG -> buildConfig();
            case SETTINGS_ROWS -> buildSettingsRows();
            case MODULE -> buildModule();
        }
        for (SettingRow row : rows) {
            row.collectFields(fields::add);
        }
    }

    private void buildSettings() {
        rows.add(new SettingRow.Section("APPEARANCE"));
        rows.add(new SettingRow.Cycle("Theme",
                () -> mod.getGuiTheme().displayName,
                () -> {
                    GuiTheme.Theme[] all = GuiTheme.Theme.values();
                    mod.setGuiTheme(all[(mod.getGuiTheme().ordinal() + 1) % all.length]);
                }));
        rows.add(new SettingRow.Slider("GUI Opacity", GuiTheme.OPACITY_MIN, GuiTheme.OPACITY_MAX, 1, "%",
                mod::getGuiOpacity, mod::setGuiOpacity));
        rows.add(new SettingRow.Slider("GUI scale", GuiScale.MIN_PCT, GuiScale.MAX_PCT, GuiScale.STEP_PCT, "%",
                mod::getClickGuiScalePercent, mod::setClickGuiScalePercent));
        rows.add(new SettingRow.Toggle("Custom window icon",
                mod::isWindowIconEnabled,
                () -> mod.setWindowIconEnabled(!mod.isWindowIconEnabled())));
        rows.add(new SettingRow.Section("CONTROLS"));
        rows.add(new SettingRow.Keybind("Open menu", mod::getGuiOpenMenuKey, mod::setGuiOpenMenuKey, false));
        rows.add(new SettingRow.Keybind("Toggle Dungeons HUD", mod::getHudToggleDungeonsKey, mod::setHudToggleDungeonsKey, true));
        rows.add(new SettingRow.Keybind("Toggle Gens HUD", mod::getHudToggleGensKey, mod::setHudToggleGensKey, true));
        rows.add(new SettingRow.Keybind("Toggle Factories HUD", mod::getHudToggleFactoriesKey, mod::setHudToggleFactoriesKey, true));
        rows.add(new SettingRow.Keybind("Toggle Skyblock HUD", mod::getHudToggleSkyblockKey, mod::setHudToggleSkyblockKey, true));
        rows.add(new SettingRow.Keybind("Toggle Prisons HUD", mod::getHudTogglePrisonsKey, mod::setHudTogglePrisonsKey, true));
        rows.add(new SettingRow.Keybind("Toggle Advanced stats", mod::getHudToggleAdvancedKey, mod::setHudToggleAdvancedKey, true));
        rows.add(new SettingRow.Section("HUD"));
        rows.add(new SettingRow.Button("Edit HUD layout", () -> {
            if (client != null) client.setScreen(new com.emcaddons.gui.HudEditScreen(mod.getHudLayoutManager(), mod::persistHudLayout));
        }));
        rows.add(new SettingRow.Button("Reset positions", () -> mod.getHudLayoutManager().resetPositions()));
        rows.add(new SettingRow.Slider("HUD opacity", GuiTheme.OPACITY_MIN, GuiTheme.OPACITY_MAX, 1, "%",
                mod::getHudOpacity, mod::setHudOpacity));
        rows.add(new SettingRow.Slider("HUD scale", HudLayoutManager.SCALE_MIN, HudLayoutManager.SCALE_MAX,
                HudLayoutManager.SCALE_STEP, "%",
                () -> mod.getHudLayoutManager().getScalePercent(),
                pct -> mod.getHudLayoutManager().setScalePercent(pct)));
    }

    private void buildConfig() {
        ConfigProfileManager manager = mod.getConfigProfileManager();
        String active = manager != null ? manager.getActiveProfileName() : null;
        rows.add(new SettingRow.Section("PROFILES"));
        rows.add(new SettingRow.Label(
                () -> {
                    ConfigProfileManager m = mod.getConfigProfileManager();
                    String name = m != null ? m.getActiveProfileName() : null;
                    return "Active: " + (name == null || name.isEmpty() ? "none" : name);
                },
                () -> GuiTheme.ACCENT));
        rows.add(new SettingRow.Text(textRenderer, "Name", 160, 32, configNameDraft, "profile name",
                s -> configNameDraft = s));
        rows.add(new SettingRow.Button("Create", this::onConfigCreate));
        rows.add(new SettingRow.Button("Load", this::onConfigLoad));
        rows.add(new SettingRow.Button("Delete", this::onConfigDelete, true));
        rows.add(new SettingRow.Button("Export", this::onConfigExport));
        rows.add(new SettingRow.Button("Import", this::onConfigImport));
        configList.rebuild();
        if (configList.getSelected() == null && active != null) {
            configList.setSelected(active);
        }
    }

    private void onConfigCreate() {
        String name = configNameDraft == null ? "" : configNameDraft.trim();
        if (name.isEmpty()) {
            mod.sendPlayerMessagePublic("\u00a7cUsage: enter a profile name");
            return;
        }
        mod.handleOutgoingChatMessage("/config create " + name);
        configList.setSelected(name);
        rebuildPage();
    }

    private void onConfigLoad() {
        String name = configList.getSelected();
        if (name == null || name.isEmpty()) {
            mod.sendPlayerMessagePublic("\u00a7cSelect a profile to load");
            return;
        }
        mod.handleOutgoingChatMessage("/config load " + name);
        rebuildPage();
    }

    private void onConfigDelete() {
        String name = configList.getSelected();
        if (name == null || name.isEmpty()) {
            mod.sendPlayerMessagePublic("\u00a7cSelect a profile to delete");
            return;
        }
        mod.handleOutgoingChatMessage("/config delete " + name);
        rebuildPage();
    }

    private void onConfigExport() {
        String name = configList.getSelected();
        if (name == null || name.isEmpty()) {
            mod.sendPlayerMessagePublic("\u00a7cSelect a profile to export");
            return;
        }
        ConfigProfileManager manager = mod.getConfigProfileManager();
        if (manager != null && name.equals(manager.getActiveProfileName())) {
            mod.flushLiveSettings();
        }
        ConfigShare.chooseAndExport(manager, name, this::onConfigShareMessage);
    }

    private void onConfigImport() {
        ConfigShare.chooseAndImport(mod.getConfigProfileManager(), this::onConfigShareMessage);
    }

    private void onConfigShareMessage(String message) {
        mod.sendPlayerMessagePublic(message);
        if (page == Page.CONFIG) {
            rebuildPage();
        }
    }

    private void buildModules() {
        rows.add(new ModuleGridRow(this::openModule));
    }

    private void openModule(ModuleGridRow.Mode mode) {
        selectedMode = mode == null ? ModuleGridRow.Mode.DUNGEONS : mode;
        setPage(Page.MODULE);
    }

    private void buildModule() {
        ModuleGridRow.Mode mode = selectedMode == null ? ModuleGridRow.Mode.DUNGEONS : selectedMode;
        rows.add(new SettingRow.Section(mode.title.toUpperCase(Locale.ROOT)));
        if (mode.comingSoon()) {
            rows.add(new SettingRow.Label(() -> "Coming Soon!", () -> GuiTheme.MUTED));
        } else {
            rows.add(new SettingRow.Toggle("Show HUD",
                    () -> mod.getHudLayoutManager().isMasterVisible(),
                    () -> {
                        mod.getHudLayoutManager().setMasterVisible(!mod.getHudLayoutManager().isMasterVisible());
                        mod.persistHudLayout();
                    }));
            rows.add(hudVisibleToggle("EMC Stats card visible", "emcstats"));
            rows.add(hudAdvancedToggle("Advanced stats"));
            rows.add(new SettingRow.Button("EMC Stats rows", () -> setPage(Page.SETTINGS_ROWS)));
            rows.add(new SettingRow.Section("ZONE"));
            rows.add(hudVisibleToggle("Zone card visible", "dungeonzone"));
            rows.add(new SettingRow.Toggle("Zone / Stage",
                    () -> mod.getDungeonZoneScoreboard().isShowZoneStage(),
                    () -> {
                        var zone = mod.getDungeonZoneScoreboard();
                        zone.setShowZoneStage(!zone.isShowZoneStage());
                        mod.persistHudLayout();
                    }));
            rows.add(new SettingRow.Toggle("Respawn time",
                    () -> mod.getDungeonZoneScoreboard().isShowRespawn(),
                    () -> {
                        var zone = mod.getDungeonZoneScoreboard();
                        zone.setShowRespawn(!zone.isShowRespawn());
                        mod.persistHudLayout();
                    }));
        }
        rows.add(new SettingRow.Button("Reset statistics", () -> {
            EmcStatsScoreboard sb = mod.getEmcStatsScoreboard();
            if (sb != null) sb.resetSession(EmcStatsScoreboard.GameMode.valueOf(mode.name()));
        }));
    }

    private void buildSettingsRows() {
        EmcStatsScoreboard sb = mod.getEmcStatsScoreboard();
        for (EmcStatsScoreboard.HudStat stat : EmcStatsScoreboard.HudStat.values()) {
            EmcStatsScoreboard.HudStat hudStat = stat;
            rows.add(new SettingRow.Toggle(hudStat.label,
                    () -> sb == null || sb.isHudStatVisible(hudStat),
                    () -> {
                        if (sb == null) return;
                        sb.setHudStatVisible(hudStat, !sb.isHudStatVisible(hudStat));
                        mod.persistHudLayout();
                    }));
        }
        rows.add(new SettingRow.Cycle("Graph currency",
                () -> graphCurrencyLabel(sb),
                () -> {
                    if (sb == null) return;
                    EmcStatsScoreboard.Currency[] all = EmcStatsScoreboard.Currency.values();
                    EmcStatsScoreboard.Currency current = sb.getGraphCurrency();
                    int idx = current == null ? 0 : current.ordinal();
                    sb.setGraphCurrency(all[(idx + 1) % all.length]);
                    mod.persistHudLayout();
                }));
        rows.add(new SettingRow.Cycle("Graph quality",
                () -> graphQualityLabel(sb),
                () -> {
                    if (sb == null) return;
                    StatCard.GraphQuality[] all = StatCard.GraphQuality.values();
                    StatCard.GraphQuality current = sb.getGraphQuality();
                    int idx = current == null ? 0 : current.ordinal();
                    sb.setGraphQuality(all[(idx + 1) % all.length]);
                    mod.persistHudLayout();
                }));
    }

    private SettingRow.Toggle hudVisibleToggle(String label, String id) {
        return new SettingRow.Toggle(label,
                () -> {
                    var c = mod.getHudLayoutManager().get(id);
                    return c != null && c.isVisible();
                },
                () -> {
                    var c = mod.getHudLayoutManager().get(id);
                    if (c != null) {
                        c.setVisible(!c.isVisible());
                        mod.persistHudLayout();
                    }
                });
    }

    private SettingRow.Toggle hudAdvancedToggle(String label) {
        return new SettingRow.Toggle(label,
                () -> mod.getHudLayoutManager().isAdvanced(),
                () -> {
                    mod.getHudLayoutManager().toggleAdvanced();
                    mod.persistHudLayout();
                });
    }

    private static String graphCurrencyLabel(EmcStatsScoreboard sb) {
        EmcStatsScoreboard.Currency currency = sb != null ? sb.getGraphCurrency() : null;
        if (currency == null) currency = EmcStatsScoreboard.Currency.values()[0];
        String name = currency.name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String graphQualityLabel(EmcStatsScoreboard sb) {
        StatCard.GraphQuality quality = sb != null ? sb.getGraphQuality() : null;
        return quality != null ? quality.displayName : StatCard.GraphQuality.HIGH.displayName;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        layout();
        scrollAnim.set(contentScrollTarget);
        scrollAnim.update(18f);
        hoverTooltip = null;
        GuiDraw d = new GuiDraw(matrices, textRenderer);
        d.fill(0, 0, width, height, GuiTheme.DIM);
        GuiScale.begin(matrices, width, height);
        mouseX = GuiScale.mouseXi(mouseX, width);
        mouseY = GuiScale.mouseYi(mouseY, height);
        d.dropShadow(winX, winY, winW, winH, GuiTheme.WINDOW_RADIUS, 5);
        d.fillRoundRect(winX, winY, winW, winH, GuiTheme.WINDOW_RADIUS, GuiTheme.WINDOW);
        d.fillRoundRect(winX, winY, GuiTheme.SIDEBAR_W, winH, GuiTheme.WINDOW_RADIUS, GuiTheme.SIDEBAR);
        d.fill(winX + GuiTheme.SIDEBAR_W - 8, winY, 8, winH, GuiTheme.SIDEBAR);
        d.vGradient(winX + GuiTheme.SIDEBAR_W, winY, winW - GuiTheme.SIDEBAR_W, GuiTheme.HEADER_H + 10,
                GuiTheme.HEADER_GRAD_TOP, GuiTheme.HEADER_GRAD_BOTTOM);
        d.fillRoundRect(contentX, contentY, contentW, contentH, GuiTheme.CARD_RADIUS, GuiTheme.CARD);

        renderSidebar(d, mouseX, mouseY);
        renderHeader(d, mouseX, mouseY, delta);
        d.enableScissor(contentX, contentY, contentW, contentH);
        if (page == Page.CONFIG) {
            renderConfig(d, mouseX, mouseY);
        } else {
            renderRows(d, mouseX, mouseY);
        }
        d.disableScissor();
        drawScrollbar(d, mouseX, mouseY);
        if (hoverTooltip != null) {
            d.tooltip(hoverTooltip, mouseX, mouseY, width, height);
        }
        GuiScale.end(matrices);
    }

    private void drawScrollbar(GuiDraw d, int mouseX, int mouseY) {
        int inner = rowsHeight();
        int max = Math.max(0, inner - contentH);
        scrollbarActive = max > 0 && page != Page.CONFIG;
        if (!scrollbarActive) return;
        scrollbarX = contentX + contentW - 2;
        scrollbarY = contentY;
        scrollbarH = contentH;
        float t = max == 0 ? 0f : Math.round(scrollAnim.get()) / (float) max;
        thumbH = Math.max(24, scrollbarH * scrollbarH / inner);
        thumbY = scrollbarY + Math.round((scrollbarH - thumbH) * t);
        boolean hover = draggingScrollbar || GuiTheme.hit(scrollbarX - 2, thumbY, 6, thumbH, mouseX, mouseY);
        d.fillRoundRect(scrollbarX, scrollbarY, 3, scrollbarH, 1, GuiTheme.SCROLLBAR_TRACK);
        d.fillRoundRect(scrollbarX, thumbY, 3, thumbH, 1, hover ? GuiTheme.SCROLLBAR_THUMB_HOVER : GuiTheme.SCROLLBAR_THUMB);
    }

    private void applyScrollbarDrag(double mouseY) {
        int max = maxScroll();
        int track = Math.max(1, scrollbarH - thumbH);
        double t = (mouseY - scrollbarY - thumbH / 2.0) / track;
        t = Math.max(0, Math.min(1, t));
        contentScrollTarget = (int) Math.round(t * max);
    }

    private boolean hasLogo() {
        if (logoPresent != null) return logoPresent;
        MinecraftClient mc = MinecraftClient.getInstance();
        ResourceManager manager = mc != null ? mc.getResourceManager() : null;
        logoPresent = manager != null && manager.containsResource(LOGO);
        return logoPresent;
    }

    private void renderSidebar(GuiDraw d, int mouseX, int mouseY) {
        int logoX = winX + 12;
        int logoY = winY + 12;
        boolean logo = hasLogo();
        if (logo) {
            d.texture(LOGO, logoX, logoY, LOGO_SIZE, LOGO_SIZE, LOGO_TEX, LOGO_TEX);
        }

        int textX = logo ? logoX + LOGO_SIZE + 8 : logoX;
        int stackH = 20;
        int textY = logoY + (LOGO_SIZE - stackH) / 2;
        d.text(GuiTheme.PRODUCT, textX, textY, GuiTheme.TITLE);
        d.text(MOD_VERSION, textX, textY + 11, GuiTheme.MUTED);

        int y = logoY + LOGO_SIZE + 16;
        d.text("HOME", winX + 16, y, GuiTheme.MUTED);
        y += 14;
        navModulesY = y;
        y = navItem(d, y, GuiDraw.Icon.BOX, "Modules", Page.MODULES, mouseX, mouseY);
        y += 8;
        d.text("CONFIGURATION", winX + 16, y, GuiTheme.MUTED);
        y += 14;
        navSettingsY = y;
        y = navItem(d, y, GuiDraw.Icon.GEAR, "Settings", Page.SETTINGS, mouseX, mouseY);
        navConfigY = y;
        navItem(d, y, GuiDraw.Icon.PATH, "Config", Page.CONFIG, mouseX, mouseY);
    }

    private boolean sidebarSelected(Page target) {
        if (target == Page.MODULES) {
            return page == Page.MODULES || page == Page.MODULE || page == Page.SETTINGS_ROWS;
        }
        if (target == Page.SETTINGS) {
            return page == Page.SETTINGS;
        }
        return page == target;
    }

    private Anim navAnim(Page p) {
        return navAnims.computeIfAbsent(p, k -> new Anim(0f));
    }

    private int navItem(GuiDraw d, int y, GuiDraw.Icon icon, String label, Page target, int mouseX, int mouseY) {
        int x = winX + 10;
        int w = GuiTheme.SIDEBAR_W - 20;
        int h = GuiTheme.NAV_H;
        boolean selected = sidebarSelected(target);
        boolean hover = GuiTheme.hit(x, y, w, h, mouseX, mouseY);
        Anim a = navAnim(target);
        a.set(selected ? 1f : 0f);
        float t = a.update(16f);
        if (t > 0.02f || hover) {
            int bg = hover && t < 0.5f ? GuiTheme.PILL : Anim.lerpColor(GuiTheme.PILL, GuiTheme.ACCENT_SOFT, t);
            d.fillRoundRect(x, y, w, h, 6, bg);
        }
        if (t > 0.02f) {
            int barH = Math.round((h - 10) * t);
            d.fill(x, y + (h - barH) / 2, 2, barH, GuiTheme.ACCENT);
        }
        int labelX = x + 10;
        int iconColor = Anim.lerpColor(GuiTheme.MUTED, GuiTheme.ACCENT, t);
        int textColor = Anim.lerpColor(GuiTheme.MUTED, GuiTheme.TITLE, t);
        if (icon != null) {
            d.icon(icon, x + 10, y + 9, iconColor);
            labelX = x + 24;
        }
        d.text(label, labelX, y + 9, textColor);
        return y + h + 4;
    }

    private void renderHeader(GuiDraw d, int mouseX, int mouseY, float delta) {
        int tx = contentX;
        int backW = 52;
        int backX = contentX;
        int backY = winY + 12;
        if (isDrillIn()) {
            boolean backHover = GuiTheme.hit(backX, backY, backW, 22, mouseX, mouseY);
            d.fillRoundRect(backX, backY, backW, 22, 6, backHover ? GuiTheme.ACCENT_SOFT : GuiTheme.PILL);
            d.textCenter("Back", backX + backW / 2, backY + 7, GuiTheme.TITLE);
            tx = backX + backW + 10;
        }
        String title = headerTitle();
        d.text(title, tx, winY + 12, GuiTheme.TITLE);
        d.text(" | " + headerSubtitle(), tx + d.width(title), winY + 12, GuiTheme.MUTED);
        int themeRight = winX + winW - GuiTheme.PAD;
        themeSelector.layoutRight(themeRight, winY + 12, d);
        themeSelector.render(d, mouseX, mouseY);
    }

    private boolean isDrillIn() {
        return page == Page.SETTINGS_ROWS || page == Page.MODULE;
    }

    private Page backPage() {
        if (page == Page.MODULE) return Page.MODULES;
        if (page == Page.SETTINGS_ROWS) return Page.MODULE;
        return Page.SETTINGS;
    }

    private String headerTitle() {
        return switch (page) {
            case MODULES -> "Modules";
            case SETTINGS -> "Settings";
            case CONFIG -> "Config";
            case SETTINGS_ROWS -> "EMC Stats rows";
            case MODULE -> selectedMode == null ? "Modules" : selectedMode.title;
        };
    }

    private String headerSubtitle() {
        return switch (page) {
            case MODULES -> "Manage modules";
            case SETTINGS -> "Appearance, keybind, and HUD";
            case CONFIG -> "Profiles and sharing";
            case SETTINGS_ROWS -> "Show or hide card lines";
            case MODULE -> selectedMode != null && selectedMode.comingSoon() ? "Coming Soon!" : "Settings";
        };
    }

    private void renderConfig(GuiDraw d, int mouseX, int mouseY) {
        int x = contentX + GuiTheme.PAD;
        int y = contentY + 10;
        int w = contentW - GuiTheme.PAD * 2;
        for (SettingRow row : rows) {
            row.render(d, x, y, w, mouseX, mouseY);
            y += row.height() + 4;
        }
        int listH = Math.max(40, contentY + contentH - y - 8);
        configList.setBounds(x, y, w, listH);
        configList.render(d, mouseX, mouseY);
    }

    private void renderRows(GuiDraw d, int mouseX, int mouseY) {
        int x = contentX + GuiTheme.PAD;
        int y = contentY + 10 - Math.round(scrollAnim.get());
        int w = contentW - GuiTheme.PAD * 2;
        for (SettingRow row : rows) {
            row.render(d, x, y, w, mouseX, mouseY);
            y += row.height() + 4;
        }
    }

    private int rowsHeight() {
        int h = 10;
        for (SettingRow row : rows) h += row.height() + 4;
        return h + 8;
    }

    private int maxScroll() {
        return Math.max(0, rowsHeight() - contentH);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseX = GuiScale.mouseX(mouseX, width);
        mouseY = GuiScale.mouseY(mouseY, height);
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (clickNav(mouseX, mouseY, navModulesY, Page.MODULES)) return true;
        if (clickNav(mouseX, mouseY, navSettingsY, Page.SETTINGS)) return true;
        if (clickNav(mouseX, mouseY, navConfigY, Page.CONFIG)) return true;

        if (isDrillIn()) {
            int backX = contentX;
            int backY = winY + 12;
            if (GuiTheme.hit(backX, backY, 52, 22, mouseX, mouseY)) {
                setPage(backPage());
                return true;
            }
        }

        if (themeSelector.mouseClicked(mouseX, mouseY, button)) return true;

        if (scrollbarActive && GuiTheme.hit(scrollbarX - 3, scrollbarY, 9, scrollbarH, mouseX, mouseY)) {
            draggingScrollbar = true;
            applyScrollbarDrag(mouseY);
            return true;
        }

        unfocusFields();
        for (GuiTextField field : fields) {
            if (field.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (page == Page.CONFIG) {
            for (SettingRow row : rows) {
                if (row.mouseClicked(mouseX, mouseY, button)) return true;
            }
            if (configList.mouseClicked(mouseX, mouseY)) return true;
            return true;
        }

        for (SettingRow row : rows) {
            if (row.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return true;
    }

    private boolean clickNav(double mouseX, double mouseY, int y, Page target) {
        int x = winX + 10;
        int w = GuiTheme.SIDEBAR_W - 20;
        if (GuiTheme.hit(x, y, w, GuiTheme.NAV_H, mouseX, mouseY)) {
            setPage(target);
            return true;
        }
        return false;
    }

    private void unfocusFields() {
        for (GuiTextField field : fields) field.setFocused(false);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        mouseX = GuiScale.mouseX(mouseX, width);
        mouseY = GuiScale.mouseY(mouseY, height);
        if (draggingScrollbar) {
            applyScrollbarDrag(mouseY);
            return true;
        }
        for (SettingRow row : rows) {
            if (row.mouseDragged(mouseX, mouseY)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mouseX = GuiScale.mouseX(mouseX, width);
        mouseY = GuiScale.mouseY(mouseY, height);
        draggingScrollbar = false;
        for (SettingRow row : rows) row.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        mouseX = GuiScale.mouseX(mouseX, width);
        mouseY = GuiScale.mouseY(mouseY, height);
        if (page == Page.CONFIG && configList.mouseScrolled(mouseX, mouseY, amount)) {
            return true;
        }
        if (GuiTheme.hit(contentX, contentY, contentW, contentH, mouseX, mouseY)) {
            int max = maxScroll();
            if (max > 0) {
                contentScrollTarget = (int) Math.max(0, Math.min(max, contentScrollTarget - amount * 18));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (SettingRow row : rows) {
            if (row instanceof SettingRow.Keybind keybind && keybind.captureKey(keyCode)) {
                return true;
            }
        }
        for (SettingRow row : rows) {
            if (row.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        for (GuiTextField field : fields) {
            if (field.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (SettingRow row : rows) {
            if (row.charTyped(chr, modifiers)) return true;
        }
        for (GuiTextField field : fields) {
            if (field.charTyped(chr, modifiers)) return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        for (SettingRow row : rows) row.tick();
        for (GuiTextField field : fields) field.tick();
    }

    @Override
    public void close() {
        leavePage();
        super.close();
    }

    @Override
    public void renderBackground(MatrixStack matrices) {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static String readModVersion() {
        try {
            return FabricLoader.getInstance()
                    .getModContainer(EmcAddons.MOD_ID)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("");
        } catch (Exception e) {
            return "";
        }
    }
}
