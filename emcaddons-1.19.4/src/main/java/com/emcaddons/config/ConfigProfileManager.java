package com.emcaddons.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public final class ConfigProfileManager {
    private static final String PROFILES_DIR = "profiles";
    private static final String ACTIVE_PROFILE_FILE = "active_profile.cbcfg";
    private static final String ACTIVE_PROFILE_KEY = "activeProfile";
    private static final String DEFAULT_PROFILE = "default";
    private static final String SETTINGS_FILE = "settings.cbcfg";
    private static final Set<String> SKIP_COPY_NAMES = Set.of(
            "path_system.cbcfg", "path_keybinds.cbcfg"
    );

    private final File baseConfigDir;
    private final File profilesDir;
    private final File activeProfileFile;

    public ConfigProfileManager(File baseConfigDir) {
        this.baseConfigDir = baseConfigDir;
        this.profilesDir = new File(baseConfigDir, PROFILES_DIR);
        this.activeProfileFile = new File(baseConfigDir, ACTIVE_PROFILE_FILE);
    }

    public void initialize() {
        ensureDirs();
        String active = getActiveProfileName();
        if (active == null || active.trim().isEmpty()) {
            setActiveProfile(DEFAULT_PROFILE);
            active = DEFAULT_PROFILE;
        }
        ensureProfileDir(active);
    }

    public String getActiveProfileName() {
        if (!activeProfileFile.exists()) return null;
        try {
            Properties p = BinaryKeyValueFile.load(activeProfileFile);
            return p.getProperty(ACTIVE_PROFILE_KEY);
        } catch (IOException ignored) {
            return null;
        }
    }

    public void setActiveProfile(String name) {
        validateName(name);
        ensureDirs();
        ensureProfileDir(name);
        Properties p = new Properties();
        p.setProperty(ACTIVE_PROFILE_KEY, name);
        try {
            BinaryKeyValueFile.save(p, activeProfileFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save active profile", e);
        }
    }

    public File getBaseConfigDir() {
        return baseConfigDir;
    }

    public File getProfilesDir() {
        ensureDirs();
        return profilesDir;
    }

    public File getProfileDir(String name) {
        validateName(name);
        return new File(profilesDir, name);
    }

    public boolean profileExists(String name) {
        if (name == null || name.isEmpty()) return false;
        return new File(profilesDir, name).isDirectory();
    }

    public static boolean isValidName(String name) {
        return name != null && name.matches("[A-Za-z0-9_-]{1,32}");
    }

    public static String sanitizeName(String raw) {
        if (raw == null) return "imported";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length() && sb.length() < 32; i++) {
            char c = raw.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? "imported" : sb.toString();
    }

    public String allocateUniqueName(String desired) {
        ensureDirs();
        String base = sanitizeName(desired);
        if (!new File(profilesDir, base).exists()) return base;
        for (int n = 2; n < 1000; n++) {
            String suffix = "_" + n;
            int max = 32 - suffix.length();
            String stem = base.length() <= max ? base : base.substring(0, max);
            String candidate = stem + suffix;
            if (!new File(profilesDir, candidate).exists()) return candidate;
        }
        throw new IllegalStateException("Could not allocate a unique profile name");
    }

    public File getActiveProfileDir() {
        String active = getActiveProfileName();
        if (active == null || active.trim().isEmpty()) active = DEFAULT_PROFILE;
        File dir = new File(profilesDir, active);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public Properties loadActiveSettings() {
        File settings = new File(getActiveProfileDir(), SETTINGS_FILE);
        if (!settings.exists()) return new Properties();
        try {
            return BinaryKeyValueFile.load(settings);
        } catch (IOException e) {
            return new Properties();
        }
    }

    public void saveActiveSettings(Properties properties) {
        File settings = new File(getActiveProfileDir(), SETTINGS_FILE);
        try {
            BinaryKeyValueFile.save(properties, settings);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save active settings", e);
        }
    }

    public void createProfileFromActive(String name) {
        validateName(name);
        File target = new File(profilesDir, name);
        if (target.exists()) {
            throw new IllegalArgumentException("Profile already exists: " + name);
        }
        File source = getActiveProfileDir();
        copyDirectory(source, target);
    }

    public List<String> listProfiles() {
        ensureDirs();
        File[] dirs = profilesDir.listFiles(File::isDirectory);
        List<String> names = new ArrayList<>();
        if (dirs != null) {
            for (File dir : dirs) {
                if (dir.getName().startsWith(".")) continue;
                names.add(dir.getName());
            }
        }
        Collections.sort(names);
        return names;
    }

    public boolean deleteProfile(String name) {
        validateName(name);
        String active = getActiveProfileName();
        if (name.equals(active)) return false;
        File dir = new File(profilesDir, name);
        if (!dir.exists()) return false;
        deleteRecursively(dir);
        return true;
    }

    public void migrateLegacySettingsIfNeeded(Properties legacyProperties) {
        File settings = new File(getActiveProfileDir(), SETTINGS_FILE);
        if (settings.exists()) return;
        if (legacyProperties == null || legacyProperties.isEmpty()) return;
        saveActiveSettings(legacyProperties);
    }

    private void ensureDirs() {
        if (!baseConfigDir.exists()) baseConfigDir.mkdirs();
        if (!profilesDir.exists()) profilesDir.mkdirs();
    }

    private void ensureProfileDir(String name) {
        File dir = new File(profilesDir, name);
        if (!dir.exists()) dir.mkdirs();
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Profile name is required");
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Profile name must match [A-Za-z0-9_-]{1,32}");
        }
    }

    private static void copyDirectory(File source, File target) {
        if (!target.exists()) target.mkdirs();
        File[] files = source.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (SKIP_COPY_NAMES.contains(file.getName().toLowerCase(Locale.ROOT))) continue;
            File out = new File(target, file.getName());
            if (file.isDirectory()) {
                copyDirectory(file, out);
            } else {
                try {
                    Files.copy(file.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy profile file: " + file.getName(), e);
                }
            }
        }
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        file.delete();
    }
}
