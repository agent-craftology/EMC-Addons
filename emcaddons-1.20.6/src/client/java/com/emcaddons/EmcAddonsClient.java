package com.emcaddons;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.emcaddons.config.ConfigProfileManager;
import com.emcaddons.config.ConfigShare;
import com.emcaddons.gui.WindowIcon;
import com.emcaddons.gui.clickgui.ClickGuiScreen;
import com.emcaddons.gui.clickgui.GuiScale;
import com.emcaddons.gui.clickgui.GuiTheme;
import com.emcaddons.scoreboard.DungeonZoneScoreboard;
import com.emcaddons.scoreboard.EmcSidebar;
import com.emcaddons.scoreboard.EmcStatsScoreboard;
import com.emcaddons.scoreboard.HudLayoutManager;
import com.emcaddons.scoreboard.NameplateText;
import com.emcaddons.scoreboard.StatCard;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.function.IntConsumer;

public class EmcAddonsClient implements ClientModInitializer {
    private static EmcAddonsClient instance;

    private KeyBinding guiKeyBinding;
    private int guiOpenMenuKey = GLFW.GLFW_KEY_RIGHT_ALT;
    private KeyBinding hudToggleDungeonsBinding;
    private KeyBinding hudToggleGensBinding;
    private KeyBinding hudToggleFactoriesBinding;
    private KeyBinding hudToggleSkyblockBinding;
    private KeyBinding hudTogglePrisonsBinding;
    private int hudToggleDungeonsKey = 0;
    private int hudToggleGensKey = 0;
    private int hudToggleFactoriesKey = 0;
    private int hudToggleSkyblockKey = 0;
    private int hudTogglePrisonsKey = 0;
    private File CONFIG_DIR;
    private ConfigProfileManager configProfileManager;
    private final HudLayoutManager hudLayoutManager = new HudLayoutManager();
    private final EmcStatsScoreboard emcStatsScoreboard = new EmcStatsScoreboard();
    private final DungeonZoneScoreboard dungeonZoneScoreboard = new DungeonZoneScoreboard();
    private EmcStatsScoreboard.Currency hudCurrencyGraph = EmcStatsScoreboard.Currency.SOULS;
    private StatCard.GraphQuality hudGraphQuality = StatCard.GraphQuality.HIGH;
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
        hudLayoutManager.register(dungeonZoneScoreboard, 6, 90);
        applyHudCurrencySettings();
        if (pendingHudSettings != null) {
            hudLayoutManager.deserialize(pendingHudSettings);
            pendingHudSettings = null;
        }

        guiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.emcaddons.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT,
                "key.categories.emcaddons"
        ));
        guiKeyBinding.setBoundKey(inputKey(guiOpenMenuKey));
        hudToggleDungeonsBinding = registerToggleKey("key.emcaddons.toggle_dungeons", hudToggleDungeonsKey);
        hudToggleGensBinding = registerToggleKey("key.emcaddons.toggle_gens", hudToggleGensKey);
        hudToggleFactoriesBinding = registerToggleKey("key.emcaddons.toggle_factories", hudToggleFactoriesKey);
        hudToggleSkyblockBinding = registerToggleKey("key.emcaddons.toggle_skyblock", hudToggleSkyblockKey);
        hudTogglePrisonsBinding = registerToggleKey("key.emcaddons.toggle_prisons", hudTogglePrisonsKey);
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
            dispatcher.register(ClientCommandManager.literal("emczone")
                    .then(ClientCommandManager.literal("debug")
                            .executes(ctx -> {
                                dumpEmcZoneDebug();
                                return 1;
                            })));
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> WindowIcon.apply(client, isWindowIconEnabled()));

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.world == null || client.player == null) return;
                emcStatsScoreboard.update(client);
                dungeonZoneScoreboard.update(client);
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
                pollHudToggle(hudToggleDungeonsBinding, hudToggleDungeonsKey, "emcstats");
                pollHudToggle(hudToggleGensBinding, hudToggleGensKey, "gens");
                pollHudToggle(hudToggleFactoriesBinding, hudToggleFactoriesKey, "factories");
                pollHudToggle(hudToggleSkyblockBinding, hudToggleSkyblockKey, "skyblock");
                pollHudToggle(hudTogglePrisonsBinding, hudTogglePrisonsKey, "prisons");
                boolean rebound = syncBoundKey(guiKeyBinding, guiOpenMenuKey, v -> guiOpenMenuKey = v);
                rebound |= syncBoundKey(hudToggleDungeonsBinding, hudToggleDungeonsKey, v -> hudToggleDungeonsKey = v);
                rebound |= syncBoundKey(hudToggleGensBinding, hudToggleGensKey, v -> hudToggleGensKey = v);
                rebound |= syncBoundKey(hudToggleFactoriesBinding, hudToggleFactoriesKey, v -> hudToggleFactoriesKey = v);
                rebound |= syncBoundKey(hudToggleSkyblockBinding, hudToggleSkyblockKey, v -> hudToggleSkyblockKey = v);
                rebound |= syncBoundKey(hudTogglePrisonsBinding, hudTogglePrisonsKey, v -> hudTogglePrisonsKey = v);
                if (rebound) saveSettings();
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
            guiKeyBinding.setBoundKey(inputKey(guiOpenMenuKey));
            KeyBinding.updateKeysByCode();
        }
        saveSettings();
    }

    public int getHudToggleDungeonsKey() {
        return hudToggleDungeonsKey;
    }

    public void setHudToggleDungeonsKey(int keyCode) {
        this.hudToggleDungeonsKey = normalizeKey(keyCode);
        bindAndSave(hudToggleDungeonsBinding, hudToggleDungeonsKey);
    }

    public int getHudToggleGensKey() {
        return hudToggleGensKey;
    }

    public void setHudToggleGensKey(int keyCode) {
        this.hudToggleGensKey = normalizeKey(keyCode);
        bindAndSave(hudToggleGensBinding, hudToggleGensKey);
    }

    public int getHudToggleFactoriesKey() {
        return hudToggleFactoriesKey;
    }

    public void setHudToggleFactoriesKey(int keyCode) {
        this.hudToggleFactoriesKey = normalizeKey(keyCode);
        bindAndSave(hudToggleFactoriesBinding, hudToggleFactoriesKey);
    }

    public int getHudToggleSkyblockKey() {
        return hudToggleSkyblockKey;
    }

    public void setHudToggleSkyblockKey(int keyCode) {
        this.hudToggleSkyblockKey = normalizeKey(keyCode);
        bindAndSave(hudToggleSkyblockBinding, hudToggleSkyblockKey);
    }

    public int getHudTogglePrisonsKey() {
        return hudTogglePrisonsKey;
    }

    public void setHudTogglePrisonsKey(int keyCode) {
        this.hudTogglePrisonsKey = normalizeKey(keyCode);
        bindAndSave(hudTogglePrisonsBinding, hudTogglePrisonsKey);
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

    private void dumpEmcZoneDebug() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            sendPlayerMessage("§cNo world/player.");
            return;
        }
        EmcSidebar.Snapshot snap = EmcSidebar.read(mc);
        sendPlayerMessage("§eSidebar location: §f" + snap.location);
        var nearby = mc.world.getEntitiesByClass(
                Entity.class,
                mc.player.getBoundingBox().expand(24.0),
                entity -> entity != mc.player
        );
        sendPlayerMessage("§eNearby entities (" + nearby.size() + "):");
        for (Entity entity : nearby) {
            String typeId = String.valueOf(Registries.ENTITY_TYPE.getId(entity.getType()));
            String custom = entity.getCustomName() == null ? "-" : entity.getCustomName().getString();
            String plate = NameplateText.of(entity).orElse("-");
            String displayText = "-";
            if (entity instanceof DisplayEntity.TextDisplayEntity textDisplay) {
                DisplayEntity.TextDisplayEntity.Data data = textDisplay.getData();
                if (data != null && data.text() != null) {
                    displayText = data.text().getString();
                } else {
                    displayText = "(unset)";
                }
            }
            sendPlayerMessage("§7" + typeId + " §8custom=§f" + custom + " §8plate=§f" + plate + " §8display=§f" + displayText);
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
        applyAllKeyBindings();
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
        StatCard.GraphQuality quality = hudGraphQuality != null ? hudGraphQuality : StatCard.GraphQuality.HIGH;
        if (lastHudProperties != null) {
            String stored = lastHudProperties.getProperty("hud.graph.quality", quality.name());
            try {
                quality = StatCard.GraphQuality.valueOf(stored.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
        emcStatsScoreboard.setGraphQuality(quality);
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
        hudToggleDungeonsKey = parseKeyCode(map.getProperty("hudToggleKey.dungeons"), hudToggleDungeonsKey);
        hudToggleGensKey = parseKeyCode(map.getProperty("hudToggleKey.gens"), hudToggleGensKey);
        hudToggleFactoriesKey = parseKeyCode(map.getProperty("hudToggleKey.factories"), hudToggleFactoriesKey);
        hudToggleSkyblockKey = parseKeyCode(map.getProperty("hudToggleKey.skyblock"), hudToggleSkyblockKey);
        hudTogglePrisonsKey = parseKeyCode(map.getProperty("hudToggleKey.prisons"), hudTogglePrisonsKey);
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
        String hudQuality = map.getProperty("hud.graph.quality", "HIGH");
        try {
            hudGraphQuality = StatCard.GraphQuality.valueOf(hudQuality.trim());
        } catch (IllegalArgumentException ignored) {
            hudGraphQuality = StatCard.GraphQuality.HIGH;
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
        StatCard.GraphQuality graphQuality = emcStatsScoreboard.getGraphQuality();
        if (graphQuality == null) graphQuality = StatCard.GraphQuality.HIGH;
        p.setProperty("hud.graph.quality", graphQuality.name());
        hudLayoutManager.serialize(p);
        if (configProfileManager != null) {
            configProfileManager.saveActiveSettings(p);
        }
    }

    private KeyBinding registerToggleKey(String translationKey, int keyCode) {
        KeyBinding binding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                translationKey,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.emcaddons"
        ));
        binding.setBoundKey(inputKey(keyCode));
        return binding;
    }

    private void applyAllKeyBindings() {
        if (guiKeyBinding != null) guiKeyBinding.setBoundKey(inputKey(guiOpenMenuKey));
        if (hudToggleDungeonsBinding != null) hudToggleDungeonsBinding.setBoundKey(inputKey(hudToggleDungeonsKey));
        if (hudToggleGensBinding != null) hudToggleGensBinding.setBoundKey(inputKey(hudToggleGensKey));
        if (hudToggleFactoriesBinding != null) hudToggleFactoriesBinding.setBoundKey(inputKey(hudToggleFactoriesKey));
        if (hudToggleSkyblockBinding != null) hudToggleSkyblockBinding.setBoundKey(inputKey(hudToggleSkyblockKey));
        if (hudTogglePrisonsBinding != null) hudTogglePrisonsBinding.setBoundKey(inputKey(hudTogglePrisonsKey));
        KeyBinding.updateKeysByCode();
    }

    private void bindAndSave(KeyBinding binding, int keyCode) {
        if (binding != null) {
            binding.setBoundKey(inputKey(keyCode));
            KeyBinding.updateKeysByCode();
        }
        saveSettings();
    }

    private void pollHudToggle(KeyBinding binding, int storedKey, String cardId) {
        if (binding == null) return;
        while (binding.wasPressed()) {
            if (storedKey != 0) {
                hudLayoutManager.toggleCard(cardId);
                persistHudLayout();
            }
        }
    }

    private boolean syncBoundKey(KeyBinding binding, int stored, IntConsumer assign) {
        if (binding == null) return false;
        int controlKey = KeyBindingHelper.getBoundKeyOf(binding).getCode();
        if (controlKey == GLFW.GLFW_KEY_UNKNOWN) controlKey = 0;
        if (controlKey != stored) {
            assign.accept(controlKey);
            return true;
        }
        return false;
    }

    private static InputUtil.Key inputKey(int keyCode) {
        return InputUtil.fromKeyCode(keyCode <= 0 ? GLFW.GLFW_KEY_UNKNOWN : keyCode, 0);
    }

    private static int normalizeKey(int keyCode) {
        return keyCode <= 0 ? 0 : keyCode;
    }

    private static int parseKeyCode(String raw, int fallback) {
        if (raw == null) return fallback;
        try {
            return normalizeKey(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
