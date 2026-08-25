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
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class EmcAddonsClient implements ClientModInitializer {
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(EmcAddons.MOD_ID, "emcaddons")
    );

    private static EmcAddonsClient instance;

    private KeyMapping guiKeyMapping;
    private KeyMapping dungeonsHudKeyMapping;
    private KeyMapping gensHudKeyMapping;
    private KeyMapping factoriesHudKeyMapping;
    private KeyMapping skyblockHudKeyMapping;
    private KeyMapping prisonsHudKeyMapping;
    private int guiOpenMenuKey = GLFW.GLFW_KEY_RIGHT_ALT;
    private int hudToggleDungeonsKey = 0;
    private int hudToggleGensKey = 0;
    private int hudToggleFactoriesKey = 0;
    private int hudToggleSkyblockKey = 0;
    private int hudTogglePrisonsKey = 0;
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
        Minecraft mc = Minecraft.getInstance();

        CONFIG_DIR = new File(mc.gameDirectory, "config/emcaddons");
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

        guiKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.emcaddons.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT,
                KEY_CATEGORY
        ));
        guiKeyMapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(guiOpenMenuKey));
        dungeonsHudKeyMapping = registerHudToggleMapping("key.emcaddons.toggle_dungeons", hudToggleDungeonsKey);
        gensHudKeyMapping = registerHudToggleMapping("key.emcaddons.toggle_gens", hudToggleGensKey);
        factoriesHudKeyMapping = registerHudToggleMapping("key.emcaddons.toggle_factories", hudToggleFactoriesKey);
        skyblockHudKeyMapping = registerHudToggleMapping("key.emcaddons.toggle_skyblock", hudToggleSkyblockKey);
        prisonsHudKeyMapping = registerHudToggleMapping("key.emcaddons.toggle_prisons", hudTogglePrisonsKey);
        KeyMapping.resetMapping();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("config")
                    .then(ClientCommands.literal("list")
                            .executes(ctx -> {
                                handleOutgoingChatMessage("/config list");
                                return 1;
                            }))
                    .then(ClientCommands.literal("create")
                            .then(ClientCommands.argument("name", StringArgumentType.word())
                                    .executes(ctx -> {
                                        String name = StringArgumentType.getString(ctx, "name");
                                        handleOutgoingChatMessage("/config create " + name);
                                        return 1;
                                    })))
                    .then(ClientCommands.literal("delete")
                            .then(ClientCommands.argument("name", StringArgumentType.word())
                                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(configProfileManager.listProfiles(), builder))
                                    .executes(ctx -> {
                                        String name = StringArgumentType.getString(ctx, "name");
                                        handleOutgoingChatMessage("/config delete " + name);
                                        return 1;
                                    })))
                    .then(ClientCommands.literal("load")
                            .then(ClientCommands.argument("name", StringArgumentType.word())
                                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(configProfileManager.listProfiles(), builder))
                                    .executes(ctx -> {
                                        String name = StringArgumentType.getString(ctx, "name");
                                        handleOutgoingChatMessage("/config load " + name);
                                        return 1;
                                    })))
                    .then(ClientCommands.literal("export")
                            .then(ClientCommands.argument("name", StringArgumentType.word())
                                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(configProfileManager.listProfiles(), builder))
                                    .executes(ctx -> {
                                        String name = StringArgumentType.getString(ctx, "name");
                                        handleOutgoingChatMessage("/config export " + name);
                                        return 1;
                                    })))
                    .then(ClientCommands.literal("import")
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

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(EmcAddons.MOD_ID, "stats_hud"), (graphics, tickCounter) -> {
            try {
                Minecraft client = Minecraft.getInstance();
                if (client.level == null || client.player == null) return;
                emcStatsScoreboard.update(client);
                if (!hudLayoutManager.isMasterVisible()) return;
                hudLayoutManager.renderAll(graphics);
            } catch (Exception e) {
                System.err.println("EMC Addons: Error rendering HUD: " + e.getMessage());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                while (guiKeyMapping.consumeClick()) {
                    client.gui.setScreen(new ClickGuiScreen(this));
                }
                if (guiKeyMapping != null) {
                    int controlKey = KeyMappingHelper.getBoundKeyOf(guiKeyMapping).getValue();
                    if (controlKey != guiOpenMenuKey) {
                        guiOpenMenuKey = controlKey;
                        saveSettings();
                    }
                }
                pollHudToggle(dungeonsHudKeyMapping, hudToggleDungeonsKey, "emcstats");
                pollHudToggle(gensHudKeyMapping, hudToggleGensKey, "gens");
                pollHudToggle(factoriesHudKeyMapping, hudToggleFactoriesKey, "factories");
                pollHudToggle(skyblockHudKeyMapping, hudToggleSkyblockKey, "skyblock");
                pollHudToggle(prisonsHudKeyMapping, hudTogglePrisonsKey, "prisons");
                int nextDungeons = syncHudToggleKey(dungeonsHudKeyMapping, hudToggleDungeonsKey);
                int nextGens = syncHudToggleKey(gensHudKeyMapping, hudToggleGensKey);
                int nextFactories = syncHudToggleKey(factoriesHudKeyMapping, hudToggleFactoriesKey);
                int nextSkyblock = syncHudToggleKey(skyblockHudKeyMapping, hudToggleSkyblockKey);
                int nextPrisons = syncHudToggleKey(prisonsHudKeyMapping, hudTogglePrisonsKey);
                if (nextDungeons != hudToggleDungeonsKey
                        || nextGens != hudToggleGensKey
                        || nextFactories != hudToggleFactoriesKey
                        || nextSkyblock != hudToggleSkyblockKey
                        || nextPrisons != hudTogglePrisonsKey) {
                    hudToggleDungeonsKey = nextDungeons;
                    hudToggleGensKey = nextGens;
                    hudToggleFactoriesKey = nextFactories;
                    hudToggleSkyblockKey = nextSkyblock;
                    hudTogglePrisonsKey = nextPrisons;
                    saveSettings();
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
        if (guiKeyMapping != null) {
            guiKeyMapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(guiOpenMenuKey));
            KeyMapping.resetMapping();
        }
        saveSettings();
    }

    public int getHudToggleDungeonsKey() {
        return hudToggleDungeonsKey;
    }

    public void setHudToggleDungeonsKey(int keyCode) {
        this.hudToggleDungeonsKey = normalizeHudToggleKey(keyCode);
        bindHudToggleMapping(dungeonsHudKeyMapping, hudToggleDungeonsKey);
        saveSettings();
    }

    public int getHudToggleGensKey() {
        return hudToggleGensKey;
    }

    public void setHudToggleGensKey(int keyCode) {
        this.hudToggleGensKey = normalizeHudToggleKey(keyCode);
        bindHudToggleMapping(gensHudKeyMapping, hudToggleGensKey);
        saveSettings();
    }

    public int getHudToggleFactoriesKey() {
        return hudToggleFactoriesKey;
    }

    public void setHudToggleFactoriesKey(int keyCode) {
        this.hudToggleFactoriesKey = normalizeHudToggleKey(keyCode);
        bindHudToggleMapping(factoriesHudKeyMapping, hudToggleFactoriesKey);
        saveSettings();
    }

    public int getHudToggleSkyblockKey() {
        return hudToggleSkyblockKey;
    }

    public void setHudToggleSkyblockKey(int keyCode) {
        this.hudToggleSkyblockKey = normalizeHudToggleKey(keyCode);
        bindHudToggleMapping(skyblockHudKeyMapping, hudToggleSkyblockKey);
        saveSettings();
    }

    public int getHudTogglePrisonsKey() {
        return hudTogglePrisonsKey;
    }

    public void setHudTogglePrisonsKey(int keyCode) {
        this.hudTogglePrisonsKey = normalizeHudToggleKey(keyCode);
        bindHudToggleMapping(prisonsHudKeyMapping, hudTogglePrisonsKey);
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
        Minecraft client = Minecraft.getInstance();
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
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.level != null && mc.player != null) {
                mc.player.sendSystemMessage(Component.literal(message));
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
        if (guiKeyMapping != null) {
            guiKeyMapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(guiOpenMenuKey));
            KeyMapping.resetMapping();
        }
        bindHudToggleMapping(dungeonsHudKeyMapping, hudToggleDungeonsKey);
        bindHudToggleMapping(gensHudKeyMapping, hudToggleGensKey);
        bindHudToggleMapping(factoriesHudKeyMapping, hudToggleFactoriesKey);
        bindHudToggleMapping(skyblockHudKeyMapping, hudToggleSkyblockKey);
        bindHudToggleMapping(prisonsHudKeyMapping, hudTogglePrisonsKey);
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
        hudToggleDungeonsKey = parseHudToggleKey(map.getProperty("hudToggleKey.dungeons"));
        hudToggleGensKey = parseHudToggleKey(map.getProperty("hudToggleKey.gens"));
        hudToggleFactoriesKey = parseHudToggleKey(map.getProperty("hudToggleKey.factories"));
        hudToggleSkyblockKey = parseHudToggleKey(map.getProperty("hudToggleKey.skyblock"));
        hudTogglePrisonsKey = parseHudToggleKey(map.getProperty("hudToggleKey.prisons"));
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
        p.setProperty("hudToggleKey.dungeons", String.valueOf(hudToggleDungeonsKey));
        p.setProperty("hudToggleKey.gens", String.valueOf(hudToggleGensKey));
        p.setProperty("hudToggleKey.factories", String.valueOf(hudToggleFactoriesKey));
        p.setProperty("hudToggleKey.skyblock", String.valueOf(hudToggleSkyblockKey));
        p.setProperty("hudToggleKey.prisons", String.valueOf(hudTogglePrisonsKey));
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

    private KeyMapping registerHudToggleMapping(String translationKey, int keyCode) {
        KeyMapping mapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                translationKey,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY
        ));
        mapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCodeToInput(keyCode)));
        return mapping;
    }

    private static void bindHudToggleMapping(KeyMapping mapping, int keyCode) {
        if (mapping == null) return;
        mapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCodeToInput(keyCode)));
        KeyMapping.resetMapping();
    }

    private void pollHudToggle(KeyMapping mapping, int storedKey, String cardId) {
        if (mapping == null) return;
        while (mapping.consumeClick()) {
            if (storedKey == 0) continue;
            hudLayoutManager.toggleCard(cardId);
            persistHudLayout();
        }
    }

    private int syncHudToggleKey(KeyMapping mapping, int current) {
        if (mapping == null) return current;
        return normalizeHudToggleKey(KeyMappingHelper.getBoundKeyOf(mapping).getValue());
    }

    private static int parseHudToggleKey(String raw) {
        if (raw == null) return 0;
        try {
            return normalizeHudToggleKey(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int normalizeHudToggleKey(int keyCode) {
        return keyCode <= 0 ? 0 : keyCode;
    }

    private static int keyCodeToInput(int keyCode) {
        return keyCode <= 0 ? GLFW.GLFW_KEY_UNKNOWN : keyCode;
    }
}
