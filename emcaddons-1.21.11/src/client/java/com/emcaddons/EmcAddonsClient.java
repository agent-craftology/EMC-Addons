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
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class EmcAddonsClient implements ClientModInitializer {
    private static EmcAddonsClient instance;
    private static final KeyBinding.Category KEY_CATEGORY = KeyBinding.Category.create(Identifier.of("emcaddons", "main"));

    private KeyBinding guiKeyBinding;
    private int guiOpenMenuKey = GLFW.GLFW_KEY_RIGHT_ALT;
    private File CONFIG_DIR;
    private ConfigProfileManager configProfileManager;
    private final HudLayoutManager hudLayoutManager = new HudLayoutManager();
    private final EmcStatsScoreboard emcStatsScoreboard = new EmcStatsScoreboard();
    private EmcStatsScoreboard.Currency hudCurrencyGraph = EmcStatsScoreboard.Currency.SOULS;
    private Properties lastHudProperties;
    private Properties pendingHudSettings;
    private GuiTheme.Theme guiTheme = GuiTheme.Theme.EMERALD;
    private int guiOpacity = GuiTheme.OPACITY_DEFAULT;
    private float clickGuiScale = GuiScale.DEFAULT;
    private boolean windowIconEnabled = true;

    public static EmcAddonsClient getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        MinecraftClient mc = MinecraftClient.getInstance();

        CONFIG_DIR = new File(mc.runDirectory, "config/emcaddons");
        if (!CONFIG_DIR.exists()) CONFIG_DIR.mkdirs();
        configProfileManager = new ConfigProfileManager(CONFIG_DIR);
        configProfileManager.initialize();
        loadSettings();

        hudLayoutManager.register(emcStatsScoreboard, 6, 6);
        applyHudCurrencySettings();
        if (pendingHudSettings != null) {
            hudLayoutManager.deserialize(pendingHudSettings);
            pendingHudSettings = null;
        }

        guiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.emcaddons.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT,
                KEY_CATEGORY
        ));
        guiKeyBinding.setBoundKey(InputUtil.fromKeyCode(new KeyInput(guiOpenMenuKey, 0, 0)));
        KeyBinding.updateKeysByCode();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("config")
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
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> WindowIcon.apply(client, isWindowIconEnabled()));

        HudElementRegistry.addLast(Identifier.of("emcaddons", "stats"), (drawContext, tickCounter) -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.world == null || client.player == null) return;
                emcStatsScoreboard.update(client);
                if (!hudLayoutManager.isMasterVisible()) return;
                hudLayoutManager.renderAll(drawContext);
            } catch (Exception e) {
                System.err.println("EMC Addons: Error rendering HUD: " + e.getMessage());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                while (guiKeyBinding.wasPressed()) {
                    client.setScreen(new ClickGuiScreen(this));
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

    public ConfigProfileManager getConfigProfileManager() {
        return configProfileManager;
    }

    public void persistActiveProfile() {
        saveSettings();
    }

    public void flushLiveProfile() {
        persistActiveProfile();
    }

    public void createConfigProfile(String name) {
        persistActiveProfile();
        configProfileManager.createProfileFromActive(name);
    }

    public boolean deleteConfigProfile(String name) {
        return configProfileManager.deleteProfile(name);
    }

    public boolean loadConfigProfile(String name) {
        return loadProfile(name);
    }

    public void exportConfigShare(String name) {
        if (configProfileManager == null) {
            sendPlayerMessage("§cConfig profiles are not initialized.");
            return;
        }
        if (!configProfileManager.profileExists(name)) {
            sendPlayerMessage("§cUnknown profile: " + name);
            return;
        }
        if (name.equals(configProfileManager.getActiveProfileName())) {
            persistActiveProfile();
        }
        File profileDir = configProfileManager.getProfileDir(name);
        File startDir = ConfigShare.defaultExportDir(configProfileManager.getBaseConfigDir());
        ConfigShare.chooseExportFileAsync(name, startDir, dest -> {
            String err = ConfigShare.exportPreset(profileDir, name, dest);
            if (err == null) {
                sendPlayerMessage("§aExported profile §f" + name + " §ato §f" + dest.getAbsolutePath());
            } else {
                sendPlayerMessage("§cExport failed: " + err);
            }
        }, () -> sendPlayerMessage("§7Export cancelled."));
    }

    public void importConfigShare() {
        if (configProfileManager == null) {
            sendPlayerMessage("§cConfig profiles are not initialized.");
            return;
        }
        File startDir = ConfigShare.defaultExportDir(configProfileManager.getBaseConfigDir());
        ConfigShare.chooseImportFileAsync(startDir, src -> {
            String result = ConfigShare.importPreset(configProfileManager, src);
            if (ConfigShare.isError(result)) {
                sendPlayerMessage("§cImport failed: " + ConfigShare.errorMessage(result));
            } else {
                sendPlayerMessage("§aImported profile §f" + result + "§a. Use §f/config load " + result + " §ato switch.");
            }
        }, () -> sendPlayerMessage("§7Import cancelled."));
    }

    public int getGuiOpenMenuKey() {
        return guiOpenMenuKey;
    }

    public void setGuiOpenMenuKey(int keyCode) {
        this.guiOpenMenuKey = keyCode;
        if (guiKeyBinding != null) {
            guiKeyBinding.setBoundKey(InputUtil.fromKeyCode(new KeyInput(guiOpenMenuKey, 0, 0)));
            KeyBinding.updateKeysByCode();
        }
        saveSettings();
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            WindowIcon.apply(client, enabled);
        }
    }

    public boolean isScoreboardsEnabled() {
        return hudLayoutManager.isMasterVisible();
    }

    public void setScoreboardsEnabled(boolean enabled) {
        hudLayoutManager.setMasterVisible(enabled);
        saveSettings();
    }

    public HudLayoutManager getHudLayoutManager() {
        return hudLayoutManager;
    }

    public EmcStatsScoreboard getEmcStatsScoreboard() {
        return emcStatsScoreboard;
    }

    public void persistHudLayout() {
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
                    createConfigProfile(parts[2]);
                    sendPlayerMessage("§aCreated profile: " + parts[2]);
                    return true;
                case "delete":
                    if (parts.length < 3) {
                        sendPlayerMessage("§cUsage: /config delete <name>");
                        return true;
                    }
                    if (deleteConfigProfile(parts[2])) {
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
                    if (loadConfigProfile(parts[2])) {
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
                    exportConfigShare(parts[2]);
                    return true;
                case "import":
                    importConfigShare();
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

    private void sendPlayerMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.world != null && mc.player != null) {
                mc.player.sendMessage(Text.literal(message), false);
            }
        });
    }

    private boolean loadProfile(String name) {
        if (configProfileManager.listProfiles().stream().noneMatch(p -> p.equals(name))) {
            return false;
        }
        persistActiveProfile();
        configProfileManager.setActiveProfile(name);
        loadSettings();
        if (guiKeyBinding != null) {
            guiKeyBinding.setBoundKey(InputUtil.fromKeyCode(new KeyInput(guiOpenMenuKey, 0, 0)));
            KeyBinding.updateKeysByCode();
        }
        return true;
    }

    private Properties loadLegacySettingsProperties() {
        Properties props = new Properties();
        Path path = Paths.get(CONFIG_DIR.getAbsolutePath(), "settings.cfg");
        if (!Files.isRegularFile(path)) return props;
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                int eq = line.indexOf('=');
                if (eq > 0) props.setProperty(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        } catch (IOException ignored) {
        }
        return props;
    }

    private void applyHudCurrencySettings() {
        emcStatsScoreboard.loadHudVisibility(lastHudProperties);
        EmcStatsScoreboard.Currency graph = hudCurrencyGraph != null ? hudCurrencyGraph : EmcStatsScoreboard.Currency.SOULS;
        if (lastHudProperties != null) {
            String hudGraph = lastHudProperties.getProperty("hud.currency.graph", "SOULS");
            try {
                graph = EmcStatsScoreboard.Currency.valueOf(hudGraph.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
        emcStatsScoreboard.setGraphCurrency(graph);
    }

    private void loadSettings() {
        Properties map = loadLegacySettingsProperties();
        if (configProfileManager != null) {
            configProfileManager.migrateLegacySettingsIfNeeded(map);
            map = configProfileManager.loadActiveSettings();
        }

        String gok = map.getProperty("guiOpenMenuKey");
        if (gok != null) {
            try {
                guiOpenMenuKey = Integer.parseInt(gok);
            } catch (NumberFormatException ignored) {
            }
        }
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
        GuiTheme.apply(guiTheme, guiOpacity);
        String cgs = map.getProperty("clickGuiScale");
        if (cgs != null) {
            try {
                clickGuiScale = GuiScale.clamp(Float.parseFloat(cgs.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        String wie = map.getProperty("windowIconEnabled");
        if (wie != null) windowIconEnabled = Boolean.parseBoolean(wie);
        String sbe = map.getProperty("scoreboardsEnabled");
        if (sbe != null) hudLayoutManager.setMasterVisible(Boolean.parseBoolean(sbe));
        lastHudProperties = map;
        String hudGraph = map.getProperty("hud.currency.graph", "SOULS");
        try {
            hudCurrencyGraph = EmcStatsScoreboard.Currency.valueOf(hudGraph.trim());
        } catch (IllegalArgumentException ignored) {
            hudCurrencyGraph = EmcStatsScoreboard.Currency.SOULS;
        }
        applyHudCurrencySettings();
        if (hudLayoutManager.getCards().isEmpty()) {
            pendingHudSettings = map;
        } else {
            hudLayoutManager.deserialize(map);
        }
    }

    private void saveSettings() {
        Properties p = new Properties();
        p.setProperty("guiOpenMenuKey", String.valueOf(guiOpenMenuKey));
        p.setProperty("guiTheme", guiTheme.name());
        p.setProperty("guiOpacity", String.valueOf(getGuiOpacity()));
        p.setProperty("clickGuiScale", String.format(java.util.Locale.ROOT, "%.2f", getClickGuiScale()));
        p.setProperty("windowIconEnabled", String.valueOf(windowIconEnabled));
        p.setProperty("scoreboardsEnabled", String.valueOf(hudLayoutManager.isMasterVisible()));
        emcStatsScoreboard.saveHudVisibility(p);
        EmcStatsScoreboard.Currency graphCurrency = emcStatsScoreboard.getGraphCurrency();
        if (graphCurrency == null) graphCurrency = EmcStatsScoreboard.Currency.SOULS;
        p.setProperty("hud.currency.graph", graphCurrency.name());
        hudLayoutManager.serialize(p);
        if (configProfileManager != null) {
            configProfileManager.saveActiveSettings(p);
        }
    }
}
