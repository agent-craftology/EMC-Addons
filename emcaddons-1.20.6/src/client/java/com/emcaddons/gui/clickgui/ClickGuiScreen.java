package com.emcaddons.gui.clickgui;

import com.emcaddons.EmcAddons;
import com.emcaddons.EmcAddonsClient;
import com.emcaddons.gui.clickgui.widget.GuiTextField;
import com.emcaddons.gui.clickgui.widget.ProfileListWidget;
import com.emcaddons.gui.clickgui.widget.SettingRow;
import com.emcaddons.gui.clickgui.widget.SliderRow;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    public enum Page {
        MODULES, SETTINGS, CONFIG, SETTINGS_ROWS, MODULE
    }

    public enum Module {
        DUNGEONS("Dungeons", false),
        GENS("Gens", true),
        FACTORIES("Factories", true),
        SKYBLOCK("Skyblock", true),
        PRISONS("Prisons", true);

        public final String title;
        public final boolean comingSoon;

        Module(String title, boolean comingSoon) {
            this.title = title;
            this.comingSoon = comingSoon;
        }
    }

    private static final Identifier LOGO = new Identifier("emcaddons", "textures/gui/logo.png");
    private static final int LOGO_SIZE = 32;
    private static final int LOGO_TEX = 64;

    private final EmcAddonsClient mod;
    private final NavHit[] navHits = {
            new NavHit("modules", "Modules", GuiDraw.Icon.BOX, Page.MODULES, true),
            new NavHit("settings", "Settings", GuiDraw.Icon.GEAR, Page.SETTINGS, false),
            new NavHit("config", "Config", GuiDraw.Icon.PATH, Page.CONFIG, false)
    };

    private Page page = Page.MODULES;
    private Module module = Module.DUNGEONS;
    private int winX, winY, winW, winH;
    private int contentX, contentY, contentW, contentH;
    private int backX, backY, backW;
    private int contentScrollTarget;
    private final Anim scrollAnim = new Anim(0f);
    private int contentHeight;
    private final List<SettingRow> rows = new ArrayList<>();
    private final GuiTextField searchField = new GuiTextField();
    private String searchQuery = "";
    private SliderRow draggingSlider;
    private boolean draggingScrollbar;
    private final Anim contentSlide = new Anim(0f);
    private int scrollbarX, scrollbarY, scrollbarH, thumbY, thumbH;
    private boolean scrollbarActive;
    private String hoverTooltip;
    private final ThemeSelector themeSelector = new ThemeSelector();
    private String configStatus = "—";
    private final String modVersion;

    public ClickGuiScreen(EmcAddonsClient mod) {
        super(Text.literal("EMC Addons"));
        this.mod = mod;
        this.modVersion = liveModVersion();
        searchField.setMaxLength(64);
        searchField.setPlaceholder("Search...");
        searchField.setChangedListener(s -> {
            searchQuery = s;
            contentScrollTarget = 0;
            scrollAnim.snapTo(0f);
        });
    }

    private static String liveModVersion() {
        try {
            return FabricLoader.getInstance()
                    .getModContainer(EmcAddons.MOD_ID)
                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                    .orElse("?");
        } catch (Exception e) {
            return "?";
        }
    }

    @Override
    protected void init() {
        layoutChrome();
        if (rows.isEmpty()) {
            rebuildRows();
        } else {
            layoutRows();
        }
    }

    private void layoutChrome() {
        winW = Math.min(960, Math.max(620, width - 32));
        winH = Math.min(540, Math.max(360, height - 32));
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        contentX = winX + GuiTheme.SIDEBAR_W + GuiTheme.PAD;
        contentY = winY + GuiTheme.HEADER_H + 6;
        contentW = winW - GuiTheme.SIDEBAR_W - GuiTheme.PAD * 2;
        contentH = winH - GuiTheme.HEADER_H - GuiTheme.PAD;
        int searchW = GuiTheme.SEARCH_W;
        searchField.setBounds(winX + winW - GuiTheme.PAD - searchW, winY + 12, searchW, GuiTheme.SEARCH_H);
        backW = 52;
        backX = contentX;
        backY = winY + 12;
    }

    private void rebuildRows() {
        flushRows();
        rows.clear();
        draggingSlider = null;
        contentScrollTarget = 0;
        scrollAnim.snapTo(0f);
        switch (page) {
            case MODULES -> rows.addAll(ClickGuiPages.modules(this));
            case SETTINGS -> rows.addAll(ClickGuiPages.settings(mod, this));
            case CONFIG -> rows.addAll(ClickGuiPages.config(mod, this, () -> searchQuery));
            case SETTINGS_ROWS -> rows.addAll(ClickGuiPages.settingsRows(mod));
            case MODULE -> rows.addAll(ClickGuiPages.module(mod, this, module));
        }
        layoutRows();
    }

    private int currentScroll() {
        return Math.round(scrollAnim.get());
    }

    private void layoutRows() {
        int pad = GuiTheme.PAD;
        int inner = contentW - pad * 2;
        int y = pad;
        int scroll = currentScroll();
        for (int i = 0; i < rows.size(); i++) {
            SettingRow row = rows.get(i);
            if (row instanceof ProfileListWidget list && i == rows.size() - 1) {
                int used = y;
                list.setFillHeight(Math.max(160, contentH - used - pad));
            }
            row.setPosition(contentX + pad, contentY + y - scroll, inner);
            y += row.getHeight() + 6;
        }
        contentHeight = y + pad;
    }

    void openPage(Page next) {
        navigate(next);
    }

    void openModule(Module next) {
        this.module = next != null ? next : Module.DUNGEONS;
        navigate(Page.MODULE);
    }

    public void refreshPage() {
        rebuildRows();
    }

    public void setConfigStatus(String status) {
        this.configStatus = (status == null || status.isBlank()) ? "—" : status;
    }

    public String getConfigStatus() {
        return configStatus == null || configStatus.isBlank() ? "—" : configStatus;
    }

    private void navigate(Page next) {
        if (page == next) return;
        page = next;
        searchField.setFocused(false);
        searchQuery = "";
        searchField.setText("");
        contentSlide.snapTo(6f);
        contentSlide.set(0f);
        rebuildRows();
    }

    private boolean isDrillIn() {
        return page == Page.SETTINGS_ROWS || page == Page.MODULE;
    }

    private Page backPage() {
        return switch (page) {
            case MODULE -> Page.MODULES;
            case SETTINGS_ROWS -> Page.MODULE;
            default -> Page.SETTINGS;
        };
    }

    private Page sidebarPage() {
        return switch (page) {
            case MODULES, MODULE, SETTINGS_ROWS -> Page.MODULES;
            case SETTINGS -> Page.SETTINGS;
            case CONFIG -> Page.CONFIG;
        };
    }

    private boolean showSearch() {
        return page == Page.CONFIG;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, GuiTheme.DIM);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        layoutChrome();
        scrollAnim.set(contentScrollTarget);
        scrollAnim.update(18f);
        contentSlide.update(12f);
        layoutRows();
        hoverTooltip = null;
        renderBackground(context, mouseX, mouseY, delta);
        GuiScale.begin(context, width, height);
        mouseX = GuiScale.mouseXi(mouseX, width);
        mouseY = GuiScale.mouseYi(mouseY, height);
        GuiDraw.dropShadow(context, winX, winY, winW, winH, GuiTheme.WINDOW_RADIUS, 5);
        GuiDraw.fillRoundRect(context, winX, winY, winW, winH, GuiTheme.WINDOW_RADIUS, GuiTheme.WINDOW);
        GuiDraw.fillRoundRect(context, winX, winY, GuiTheme.SIDEBAR_W, winH, GuiTheme.WINDOW_RADIUS, GuiTheme.SIDEBAR);
        GuiDraw.fill(context, winX + GuiTheme.SIDEBAR_W - 8, winY, 8, winH, GuiTheme.SIDEBAR);
        GuiDraw.vGradient(context, winX + GuiTheme.SIDEBAR_W, winY, winW - GuiTheme.SIDEBAR_W, GuiTheme.HEADER_H + 10,
                GuiTheme.HEADER_GRAD_TOP, GuiTheme.HEADER_GRAD_BOTTOM);
        drawSidebar(context, mouseX, mouseY);
        drawHeader(context, mouseX, mouseY);
        GuiDraw.fillRoundRect(context, contentX, contentY, contentW, contentH, GuiTheme.CARD_RADIUS, GuiTheme.CARD);
        GuiDraw.scissor(context, contentX - 2, contentY, contentW + 4, contentH);
        int slide = Math.round(contentSlide.get());
        context.getMatrices().push();
        context.getMatrices().translate(0, slide, 0);
        drawRows(context, mouseX, mouseY - slide);
        context.getMatrices().pop();
        GuiDraw.disableScissor(context);
        drawScrollbar(context, mouseX, mouseY);
        if (hoverTooltip != null) {
            GuiDraw.tooltip(context, textRenderer, hoverTooltip, mouseX, mouseY, width, height);
        }
        GuiScale.end(context);
    }

    private void drawScrollbar(DrawContext ctx, int mouseX, int mouseY) {
        int max = Math.max(0, contentHeight - contentH);
        scrollbarActive = max > 0;
        if (!scrollbarActive) return;
        scrollbarX = contentX + contentW - 2;
        scrollbarY = contentY;
        scrollbarH = contentH;
        float t = max == 0 ? 0f : currentScroll() / (float) max;
        thumbH = Math.max(24, scrollbarH * scrollbarH / (contentHeight));
        thumbY = scrollbarY + Math.round((scrollbarH - thumbH) * t);
        boolean hover = draggingScrollbar || GuiDraw.hit(mouseX, mouseY, scrollbarX - 2, thumbY, 6, thumbH);
        GuiDraw.fillRoundRect(ctx, scrollbarX, scrollbarY, 3, scrollbarH, 1, GuiTheme.SCROLLBAR_TRACK);
        GuiDraw.fillRoundRect(ctx, scrollbarX, thumbY, 3, thumbH, 1, hover ? GuiTheme.SCROLLBAR_THUMB_HOVER : GuiTheme.SCROLLBAR_THUMB);
    }

    private void drawSidebar(DrawContext ctx, int mouseX, int mouseY) {
        int logoX = winX + 12;
        int logoY = winY + 12;
        int logoS = LOGO_SIZE;
        boolean hasLogo = GuiDraw.hasTexture(LOGO);
        if (hasLogo) {
            GuiDraw.texture(ctx, LOGO, logoX, logoY, logoS, logoS, LOGO_TEX, LOGO_TEX);
        }
        int textX = hasLogo ? logoX + logoS + 8 : logoX;
        int textBlockH = 9 + 2 + 9;
        int textY = hasLogo ? logoY + (logoS - textBlockH) / 2 : logoY + 4;
        GuiDraw.text(ctx, textRenderer, GuiTheme.PRODUCT, textX, textY, GuiTheme.TITLE);
        GuiDraw.text(ctx, textRenderer, modVersion, textX, textY + 11, GuiTheme.MUTED);

        int ny = hasLogo ? logoY + logoS + 12 : textY + 26;
        ny = drawNavSection(ctx, mouseX, mouseY, ny, "HOME", true);
        drawNavSection(ctx, mouseX, mouseY, ny + 12, "CONFIGURATION", false);
    }

    private int drawNavSection(DrawContext ctx, int mouseX, int mouseY, int startY, String title, boolean main) {
        GuiDraw.text(ctx, textRenderer, title, winX + 16, startY, GuiTheme.MUTED);
        int y = startY + 14;
        Page selected = sidebarPage();
        for (NavHit nav : navHits) {
            if (nav.main != main) continue;
            nav.x = winX + 10;
            nav.y = y;
            nav.w = GuiTheme.SIDEBAR_W - 20;
            nav.h = GuiTheme.NAV_H;
            boolean on = nav.page == selected;
            boolean hover = GuiDraw.hit(mouseX, mouseY, nav.x, nav.y, nav.w, nav.h);
            nav.selectAnim.set(on ? 1f : 0f);
            float t = nav.selectAnim.update(16f);
            if (t > 0.02f || hover) {
                int bg = hover && t < 0.5f ? GuiTheme.PILL : Anim.lerpColor(GuiTheme.PILL, GuiTheme.ACCENT_SOFT, t);
                GuiDraw.fillRoundRect(ctx, nav.x, nav.y, nav.w, nav.h, 6, bg);
            }
            if (t > 0.02f) {
                int barH = Math.round((nav.h - 10) * t);
                GuiDraw.fill(ctx, nav.x, nav.y + (nav.h - barH) / 2, 2, barH, GuiTheme.ACCENT);
            }
            int labelX = nav.x + 10;
            int iconColor = Anim.lerpColor(GuiTheme.MUTED, GuiTheme.ACCENT, t);
            int textColor = Anim.lerpColor(GuiTheme.MUTED, GuiTheme.TITLE, t);
            GuiDraw.icon(ctx, nav.icon, nav.x + 10, nav.y + 9, iconColor);
            labelX = nav.x + 24;
            GuiDraw.text(ctx, textRenderer, nav.label, labelX, nav.y + 9, textColor);
            y += GuiTheme.NAV_H + 4;
        }
        return y;
    }

    private void drawHeader(DrawContext ctx, int mouseX, int mouseY) {
        String title;
        String sub;
        switch (page) {
            case MODULES -> { title = "Modules"; sub = "Manage modules"; }
            case SETTINGS -> { title = "Settings"; sub = "Appearance, keybinds, and HUD"; }
            case CONFIG -> { title = "Config"; sub = "Profiles"; }
            case SETTINGS_ROWS -> { title = "EMC Stats rows"; sub = "Show or hide card lines"; }
            case MODULE -> {
                title = module != null ? module.title : "Module";
                sub = module != null && module.comingSoon ? "Coming Soon!" : "Statistics";
            }
            default -> { title = "Modules"; sub = ""; }
        }

        int tx = contentX;
        if (isDrillIn()) {
            boolean hover = GuiDraw.hit(mouseX, mouseY, backX, backY, backW, 22);
            GuiDraw.fillRoundRect(ctx, backX, backY, backW, 22, 6, hover ? GuiTheme.ACCENT_SOFT : GuiTheme.PILL);
            GuiDraw.textCentered(ctx, textRenderer, "Back", backX + backW / 2, backY + 7, GuiTheme.TITLE);
            tx = backX + backW + 10;
        }
        GuiDraw.text(ctx, textRenderer, title, tx, winY + 12, GuiTheme.TITLE);
        int titleW = textRenderer.getWidth(title);
        GuiDraw.text(ctx, textRenderer, " | " + sub, tx + titleW, winY + 12, GuiTheme.MUTED);
        int themeRight = winX + winW - GuiTheme.PAD;
        if (showSearch()) {
            searchField.render(ctx, textRenderer, mouseX, mouseY);
            themeRight = winX + winW - GuiTheme.PAD - GuiTheme.SEARCH_W - 8;
        }
        themeSelector.layoutRight(themeRight, winY + 12, textRenderer);
        themeSelector.render(ctx, textRenderer, mouseX, mouseY);
    }

    private void drawRows(DrawContext ctx, int mouseX, int mouseY) {
        for (SettingRow row : rows) {
            row.render(ctx, textRenderer, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseX = GuiScale.mouseX(mouseX, width);
        mouseY = GuiScale.mouseY(mouseY, height);
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        searchField.setFocused(false);
        for (SettingRow row : rows) row.unfocus();

        for (NavHit nav : navHits) {
            if (nav.w > 0 && GuiDraw.hit(mouseX, mouseY, nav.x, nav.y, nav.w, nav.h)) {
                navigate(nav.page);
                return true;
            }
        }
        if (isDrillIn() && GuiDraw.hit(mouseX, mouseY, backX, backY, backW, 22)) {
            navigate(backPage());
            return true;
        }
        if (themeSelector.mouseClicked(mouseX, mouseY, button)) return true;
        if (showSearch() && searchField.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (scrollbarActive && GuiDraw.hit(mouseX, mouseY, scrollbarX - 3, scrollbarY, 9, scrollbarH)) {
            draggingScrollbar = true;
            applyScrollbarDrag(mouseY);
            return true;
        }
        for (SettingRow row : rows) {
            if (row.mouseClicked(mouseX, mouseY, button)) {
                if (row instanceof SliderRow slider) draggingSlider = slider;
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mouseX = GuiScale.mouseX(mouseX, width);
        mouseY = GuiScale.mouseY(mouseY, height);
        boolean handled = false;
        for (SettingRow row : rows) {
            if (row.mouseReleased(mouseX, mouseY, button)) handled = true;
        }
        draggingSlider = null;
        draggingScrollbar = false;
        return handled || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        mouseX = GuiScale.mouseX(mouseX, width);
        mouseY = GuiScale.mouseY(mouseY, height);
        if (draggingScrollbar) {
            applyScrollbarDrag(mouseY);
            return true;
        }
        if (draggingSlider != null) {
            return draggingSlider.mouseDragged(mouseX, mouseY, button);
        }
        for (SettingRow row : rows) {
            if (row instanceof SliderRow slider && slider.isDragging()) {
                draggingSlider = slider;
                return slider.mouseDragged(mouseX, mouseY, button);
            }
            if (row.mouseDragged(mouseX, mouseY, button)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void applyScrollbarDrag(double mouseY) {
        int max = Math.max(0, contentHeight - contentH);
        int track = Math.max(1, scrollbarH - thumbH);
        double t = (mouseY - scrollbarY - thumbH / 2.0) / track;
        t = Math.max(0, Math.min(1, t));
        contentScrollTarget = (int) Math.round(t * max);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        mouseX = GuiScale.mouseX(mouseX, width);
        mouseY = GuiScale.mouseY(mouseY, height);
        for (SettingRow row : rows) {
            if (row.mouseScrolled(mouseX, mouseY, verticalAmount)) return true;
        }
        if (GuiDraw.hit(mouseX, mouseY, contentX, contentY, contentW, contentH)
                || GuiDraw.hit(mouseX, mouseY, winX, winY, winW, winH)) {
            int max = Math.max(0, contentHeight - contentH);
            contentScrollTarget -= (int) Math.round(verticalAmount * 18);
            if (contentScrollTarget < 0) contentScrollTarget = 0;
            if (contentScrollTarget > max) contentScrollTarget = max;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (SettingRow row : rows) {
            if (row.isCapturingKey()) {
                return row.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        if (searchField.isFocused() && searchField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        for (SettingRow row : rows) {
            if (row.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (isDrillIn()) {
                navigate(backPage());
                return true;
            }
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchField.isFocused() && searchField.charTyped(chr, modifiers)) return true;
        for (SettingRow row : rows) {
            if (row.charTyped(chr, modifiers)) return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void flushRows() {
        for (SettingRow row : rows) row.flush();
    }

    @Override
    public void close() {
        flushRows();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static final class NavHit {
        final String id;
        final String label;
        final GuiDraw.Icon icon;
        final Page page;
        final boolean main;
        final Anim selectAnim = new Anim(0f);
        int x, y, w, h;

        NavHit(String id, String label, GuiDraw.Icon icon, Page page, boolean main) {
            this.id = id;
            this.label = label;
            this.icon = icon;
            this.page = page;
            this.main = main;
        }
    }
}
