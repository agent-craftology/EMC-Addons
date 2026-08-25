package com.emcaddons;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.emcaddons.config.ConfigProfileManager;
import com.emcaddons.config.ConfigShare;
import com.emcaddons.gui.WindowIcon;
import com.emcaddons.gui.clickgui.ClickGuiScreen;
import com.emcaddons.gui.clickgui.GuiScale;
import com.emcaddons.gui.clickgui.GuiTheme;
import com.emcaddons.scoreboard.EmcStatsScoreboard;
import com.emcaddons.scoreboard.HudLayoutManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.Properties;

public class EmcAddonsClient implements ClientModInitializer {

    private static EmcAddonsClient instance;

    private KeyBinding guiKeyBinding;
    private int guiOpenMenuKey = GLFW.GLFW_KEY_RIGHT_ALT;
    private boolean windowIconEnabled = true;
    private final HudLayoutManager hudLayoutManager = new HudLayoutManager();
    private final EmcStatsScoreboard emcStats = new EmcStatsScoreboard();
    private GuiTheme.Theme guiTheme = GuiTheme.Theme.EMERALD;
    private int guiOpacity = GuiTheme.OPACITY_DEFAULT;
    private int hudOpacity = GuiTheme.OPACITY_DEFAULT;
    private float clickGuiScale = GuiScale.DEFAULT;
    private File CONFIG_DIR;
    private ConfigProfileManager configProfileManager;
    private Properties lastLoadedSettings;

    @Override
    public void onInitializeClient() {
        instance = this;
        MinecraftClient mc = MinecraftClient.getInstance();

        CONFIG_DIR = new File(mc.runDirectory, "config/emcaddons");
        if (!CONFIG_DIR.exists()) CONFIG_DIR.mkdirs();
        configProfileManager = new ConfigProfileManager(CONFIG_DIR);
        configProfileManager.initialize();
        ConfigShare.bind(configProfileManager);

        loadSettings();

        hudLayoutManager.register(emcStats, 6, 6);
        if (lastLoadedSettings != null) {
            hudLayoutManager.deserialize(lastLoadedSettings);
            applyEmcStatsHud(lastLoadedSettings);
        }

        guiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.emcaddons.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT,
                "key.categories.emcaddons"
        ));
        guiKeyBinding.setBoundKey(InputUtil.fromKeyCode(guiOpenMenuKey, 0));
        KeyBinding.updateKeysByCode();

        ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("config")
            .then(ClientCommandManager.literal("list")
                .executes(ctx -> {
                    handleOutgoingChatMessage("/config list");
                    return 1;
                }))
            .then(ClientCommandManager.literal("create")
                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        handleOutgoingChatMessage("/config create " + name);
                        return 1;
                    })))
            .then(ClientCommandManager.literal("delete")
                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                    .suggests((ctx, builder) -> CommandSource.suggestMatching(configProfileManager.listProfiles(), builder))
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        handleOutgoingChatMessage("/config delete " + name);
                        return 1;
                    })))
            .then(ClientCommandManager.literal("load")
                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                    .suggests((ctx, builder) -> CommandSource.suggestMatching(configProfileManager.listProfiles(), builder))
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        handleOutgoingChatMessage("/config load " + name);
                        return 1;
                    })))
            .then(ClientCommandManager.literal("export")
                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                    .suggests((ctx, builder) -> CommandSource.suggestMatching(configProfileManager.listProfiles(), builder))
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        handleOutgoingChatMessage("/config export " + name);
                        return 1;
                    })))
            .then(ClientCommandManager.literal("import")
                .executes(ctx -> {
                    handleOutgoingChatMessage("/config import");
                    return 1;
                }))
            .executes(ctx -> {
                sendPlayerMessage("§eUsage: /config <create|list|delete|load|export|import> [name]");
                return 1;
            })
        );

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> WindowIcon.apply(client, isWindowIconEnabled()));

        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.world == null || client.player == null) return;
                emcStats.update(client);
                hudLayoutManager.renderAll(matrixStack);
            } catch (Exception e) {
                System.err.println("EMC Addons: Error rendering HUD: " + e.getMessage());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                emcStats.update(client);
                while (guiKeyBinding.wasPressed()) {
                    if (client.world != null && client.player != null) {
                        client.setScreen(new ClickGuiScreen(this));
                    }
                }
                if (guiKeyBinding != null) {
                    int controlKey = KeyBindingHelper.getBoundKeyOf(guiKeyBinding).getCode();
                    if (controlKey != guiOpenMenuKey) {
                        guiOpenMenuKey = controlKey;
                        saveSettings();
                    }
                }
            } catch (Exception e) {
                System.err.println("EMC Addons: Error in client tick: " + e.getMessage());
            }
        });
    }

    private void sendPlayerMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.world != null && mc.player != null) {
                mc.player.sendMessage(new LiteralText(message), false);
            }
        });
    }

    public void sendPlayerMessagePublic(String message) {
        sendPlayerMessage(message);
    }

    public ConfigProfileManager getConfigProfileManager() {
        return configProfileManager;
    }

    public void flushLiveSettings() {
        saveSettings();
    }

    public void flushActiveProfileToDisk() {
        flushLiveSettings();
    }

    public void exportConfigProfile(String name) {
        if (configProfileManager == null) {
            sendPlayerMessage("§cConfig is not ready");
            return;
        }
        String active = configProfileManager.getActiveProfileName();
        if (name != null && name.equals(active)) {
            flushActiveProfileToDisk();
        }
        ConfigShare.chooseAndExport(configProfileManager, name, this::sendPlayerMessage);
    }

    public void importConfigProfile() {
        ConfigShare.chooseAndImport(configProfileManager, this::sendPlayerMessage);
    }

    public static EmcAddonsClient getInstance() {
        return instance;
    }

    public EmcStatsScoreboard getEmcStatsScoreboard() {
        return emcStats;
    }

    public GuiTheme.Theme getGuiTheme() {
        return guiTheme;
    }

    public void setGuiTheme(GuiTheme.Theme theme) {
        this.guiTheme = theme == null ? GuiTheme.Theme.EMERALD : theme;
        GuiTheme.apply(this.guiTheme, this.guiOpacity);
        saveSettings();
    }

    public int getGuiOpacity() {
        return GuiTheme.clampOpacity(guiOpacity);
    }

    public void setGuiOpacity(int percent) {
        int next = GuiTheme.clampOpacity(percent);
        if (next == this.guiOpacity) return;
        this.guiOpacity = next;
        GuiTheme.apply(this.guiTheme, this.guiOpacity);
        saveSettings();
    }

    public int getHudOpacity() {
        return GuiTheme.clampOpacity(hudOpacity);
    }

    public void setHudOpacity(int percent) {
        int next = GuiTheme.clampOpacity(percent);
        if (next == this.hudOpacity) return;
        this.hudOpacity = next;
        GuiTheme.applyHudOpacity(this.hudOpacity);
        saveSettings();
    }

    public float getClickGuiScale() {
        return GuiScale.clamp(clickGuiScale);
    }

    public int getClickGuiScalePercent() {
        return GuiScale.toPercent(clickGuiScale);
    }

    public void setClickGuiScale(float scale) {
        float next = GuiScale.clamp(scale);
        if (next == this.clickGuiScale) return;
        this.clickGuiScale = next;
        saveSettings();
    }

    public void setClickGuiScalePercent(int percent) {
        setClickGuiScale(GuiScale.fromPercent(percent));
    }

    public boolean isWindowIconEnabled() {
        return windowIconEnabled;
    }

    public void setWindowIconEnabled(boolean enabled) {
        this.windowIconEnabled = enabled;
        saveSettings();
        WindowIcon.apply(MinecraftClient.getInstance(), enabled);
    }

    public boolean isScoreboardsEnabled() {
        return hudLayoutManager.isMasterVisible();
    }

    public void setScoreboardsEnabled(boolean enabled) {
        hudLayoutManager.setMasterVisible(enabled);
        saveSettings();
    }

    public void toggleScoreboards() {
        hudLayoutManager.setMasterVisible(!hudLayoutManager.isMasterVisible());
        saveSettings();
    }

    public HudLayoutManager getHudLayoutManager() {
        return hudLayoutManager;
    }

    public void persistHudLayout() {
        saveSettings();
    }

    public int getGuiOpenMenuKey() {
        return guiOpenMenuKey;
    }

    public void setGuiOpenMenuKey(int keyCode) {
        this.guiOpenMenuKey = keyCode;
        if (guiKeyBinding != null) {
            guiKeyBinding.setBoundKey(InputUtil.fromKeyCode(guiOpenMenuKey, 0));
            KeyBinding.updateKeysByCode();
        }
        saveSettings();
    }

    public boolean handleOutgoingChatMessage(String message) {
        if (message == null) return false;
        String trimmed = message.trim();
        if (!trimmed.toLowerCase().startsWith("/config")) return false;
        String[] parts = trimmed.split("\\s+");
        if (parts.length < 2) {
            sendPlayerMessage("§eUsage: /config <create|list|delete|load|export|import> [name]");
            return true;
        }
        String action = parts[1].toLowerCase();
        try {
            switch (action) {
                case "list":
                    sendPlayerMessage("§7Profiles: " + String.join(", ", configProfileManager.listProfiles()));
                    return true;
                case "create":
                    if (parts.length < 3) {
                        sendPlayerMessage("§cUsage: /config create <name>");
                        return true;
                    }
                    flushLiveSettings();
                    configProfileManager.createProfileFromActive(parts[2]);
                    sendPlayerMessage("§aCreated profile: " + parts[2]);
                    return true;
                case "delete":
                    if (parts.length < 3) {
                        sendPlayerMessage("§cUsage: /config delete <name>");
                        return true;
                    }
                    if (configProfileManager.deleteProfile(parts[2])) {
                        sendPlayerMessage("§aDeleted profile: " + parts[2]);
                    } else {
                        sendPlayerMessage("§cCould not delete profile (missing or active): " + parts[2]);
                    }
                    return true;
                case "load":
                    if (parts.length < 3) {
                        sendPlayerMessage("§cUsage: /config load <name>");
                        return true;
                    }
                    if (loadProfile(parts[2])) {
                        sendPlayerMessage("§aLoaded profile: " + parts[2]);
                    } else {
                        sendPlayerMessage("§cFailed to load profile: " + parts[2]);
                    }
                    return true;
                case "export":
                    if (parts.length < 3) {
                        sendPlayerMessage("§cUsage: /config export <name>");
                        return true;
                    }
                    exportConfigProfile(parts[2]);
                    return true;
                case "import":
                    importConfigProfile();
                    return true;
                default:
                    sendPlayerMessage("§eUsage: /config <create|list|delete|load|export|import> [name]");
                    return true;
            }
        } catch (Exception e) {
            sendPlayerMessage("§cConfig command failed: " + e.getMessage());
            return true;
        }
    }

    private boolean loadProfile(String name) {
        if (configProfileManager.listProfiles().stream().noneMatch(p -> p.equals(name))) {
            return false;
        }
        flushLiveSettings();
        configProfileManager.setActiveProfile(name);
        loadSettings();
        if (guiKeyBinding != null) {
            guiKeyBinding.setBoundKey(InputUtil.fromKeyCode(guiOpenMenuKey, 0));
            KeyBinding.updateKeysByCode();
        }
        applyEmcStatsHud(lastLoadedSettings);
        if (lastLoadedSettings != null) hudLayoutManager.deserialize(lastLoadedSettings);
        return true;
    }

    private void applyEmcStatsHud(Properties map) {
        if (map == null) return;
        emcStats.loadHudVisibility(map);
        EmcStatsScoreboard.Currency graph = EmcStatsScoreboard.Currency.SOULS;
        String graphValue = map.getProperty("hud.currency.graph");
        if (graphValue != null && !graphValue.isEmpty()) {
            try {
                graph = EmcStatsScoreboard.Currency.valueOf(graphValue);
            } catch (IllegalArgumentException ignored) {
            }
        }
        emcStats.setGraphCurrency(graph);
    }

    private void loadSettings() {
        Properties map = new Properties();
        if (configProfileManager != null) {
            configProfileManager.migrateLegacySettingsIfNeeded(map);
            map = configProfileManager.loadActiveSettings();
        }
        lastLoadedSettings = map;

        String gok = map.getProperty("guiOpenMenuKey");
        if (gok != null) {
            try {
                guiOpenMenuKey = Integer.parseInt(gok);
            } catch (NumberFormatException ignored) {
            }
        }
        String wie = map.getProperty("windowIconEnabled");
        if (wie != null) windowIconEnabled = Boolean.parseBoolean(wie);
        String gt = map.getProperty("guiTheme");
        if (gt != null) {
            try {
                guiTheme = GuiTheme.Theme.valueOf(gt);
            } catch (IllegalArgumentException ignored) {
            }
        }
        String go = map.getProperty("guiOpacity");
        if (go != null) {
            try {
                guiOpacity = GuiTheme.clampOpacity(Integer.parseInt(go.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        String ho = map.getProperty("hudOpacity");
        if (ho != null) {
            try {
                hudOpacity = GuiTheme.clampOpacity(Integer.parseInt(ho.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        GuiTheme.apply(guiTheme, guiOpacity);
        GuiTheme.applyHudOpacity(hudOpacity);
        String cgs = map.getProperty("clickGuiScale");
        if (cgs != null) {
            try {
                clickGuiScale = GuiScale.clamp(Float.parseFloat(cgs.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        String sbe = map.getProperty("scoreboardsEnabled");
        if (sbe != null) hudLayoutManager.setMasterVisible(Boolean.parseBoolean(sbe));
        hudLayoutManager.deserialize(map);
        applyEmcStatsHud(map);
    }

    private void saveSettings() {
        Properties p = new Properties();
        p.setProperty("guiOpenMenuKey", String.valueOf(guiOpenMenuKey));
        p.setProperty("windowIconEnabled", String.valueOf(windowIconEnabled));
        p.setProperty("guiTheme", guiTheme.name());
        p.setProperty("guiOpacity", String.valueOf(getGuiOpacity()));
        p.setProperty("hudOpacity", String.valueOf(getHudOpacity()));
        p.setProperty("clickGuiScale", String.format(java.util.Locale.ROOT, "%.2f", getClickGuiScale()));
        p.setProperty("scoreboardsEnabled", String.valueOf(hudLayoutManager.isMasterVisible()));
        hudLayoutManager.serialize(p);
        emcStats.saveHudVisibility(p);
        EmcStatsScoreboard.Currency graph = emcStats.getGraphCurrency();
        p.setProperty("hud.currency.graph", graph != null ? graph.name() : "SOULS");
        if (configProfileManager != null) {
            configProfileManager.saveActiveSettings(p);
        }
    }
}
