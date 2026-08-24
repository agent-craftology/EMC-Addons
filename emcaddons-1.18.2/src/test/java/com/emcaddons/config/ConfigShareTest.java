package com.emcaddons.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigShareTest {

    @TempDir
    Path temp;

    @Test
    void exportImportRoundTripKeepsProfileFiles() throws Exception {
        File profile = profileDir("farm");
        Files.writeString(profile.toPath().resolve("settings.cbcfg"), "settings");
        Files.createDirectories(profile.toPath().resolve("paths"));
        Files.writeString(profile.toPath().resolve("paths/route.cbcfg"), "path");

        File zip = temp.resolve("farm.cbshare").toFile();
        assertNull(ConfigShare.exportPreset(profile, "farm", zip));

        try (ZipFile zf = new ZipFile(zip)) {
            Set<String> names = entryNames(zf);
            assertTrue(names.contains("manifest.txt"));
            assertTrue(names.contains("settings.cbcfg"));
            assertTrue(names.contains("paths/route.cbcfg"));
            String manifest = new String(zf.getInputStream(zf.getEntry("manifest.txt")).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(manifest.contains("mod=emcaddons"));
            assertTrue(manifest.contains("mc=1.18.2"));
            assertTrue(manifest.contains("name=farm"));
        }

        File destProfiles = temp.resolve("import-profiles").toFile();
        String imported = ConfigShare.importPreset(destProfiles, zip);
        assertFalse(ConfigShare.isError(imported), imported);
        assertEquals("farm", imported);
        assertEquals("settings", Files.readString(new File(destProfiles, "farm/settings.cbcfg").toPath()));
        assertEquals("path", Files.readString(new File(destProfiles, "farm/paths/route.cbcfg").toPath()));
        assertFalse(new File(destProfiles, "farm/manifest.txt").exists());
    }

    @Test
    void exportOmitsLicenseAndToken() throws Exception {
        File profile = profileDir("secret");
        Files.writeString(profile.toPath().resolve("settings.cbcfg"), "ok");
        Files.writeString(profile.toPath().resolve("license.txt"), "LICENSE-KEY");
        Files.writeString(profile.toPath().resolve("token.dat"), "TOKEN");
        Files.createDirectories(profile.toPath().resolve("paths"));
        Files.writeString(profile.toPath().resolve("paths").resolve("token.dat"), "nested-token");

        File zip = temp.resolve("secret.cbshare").toFile();
        assertNull(ConfigShare.exportPreset(profile, "secret", zip));

        try (ZipFile zf = new ZipFile(zip)) {
            Set<String> names = entryNames(zf);
            assertTrue(names.contains("settings.cbcfg"));
            assertFalse(names.contains("license.txt"));
            assertFalse(names.contains("token.dat"));
            assertFalse(names.contains("paths/token.dat"));
        }
    }

    @Test
    void importRejectsOtherMods() throws Exception {
        File zip = temp.resolve("talon.cbshare").toFile();
        writeShare(zip, "mod=talonmc\nmc=1.18.2\nformat=1\nname=pvp\n", "settings.cbcfg", "x");

        String result = ConfigShare.importPreset(temp.resolve("profiles").toFile(), zip);
        assertTrue(ConfigShare.isError(result), result);
        assertTrue(ConfigShare.errorMessage(result).contains("talonmc"), result);
        assertFalse(new File(temp.resolve("profiles").toFile(), "pvp").exists());
    }

    /** Rejects other mod ids. */
    @Test
    void importRejectsForeignModShares() throws Exception {
        File zip = temp.resolve("paid.cbshare").toFile();
        writeShare(zip, "mod=emcsolver\nmc=1.18.2\nformat=1\nname=farm\n", "settings.cbcfg", "x");

        String result = ConfigShare.importPreset(temp.resolve("profiles").toFile(), zip);
        assertTrue(ConfigShare.isError(result), result);
        assertTrue(ConfigShare.errorMessage(result).contains("not emcaddons"), result);
        assertFalse(new File(temp.resolve("profiles").toFile(), "farm").exists());
    }

    @Test
    void importRejectsWrongMinecraftVersion() throws Exception {
        File zip = temp.resolve("old.cbshare").toFile();
        writeShare(zip, "mod=emcaddons\nmc=1.20.6\nformat=1\nname=old\n", "settings.cbcfg", "x");

        String result = ConfigShare.importPreset(temp.resolve("profiles").toFile(), zip);
        assertTrue(ConfigShare.isError(result), result);
        assertTrue(ConfigShare.errorMessage(result).contains("1.20.6"), result);
    }

    @Test
    void importRejectsZipSlipAndDoesNotWriteOutside() throws Exception {
        File zip = temp.resolve("slip.cbshare").toFile();
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            put(out, "manifest.txt", "mod=emcaddons\nmc=1.18.2\nformat=1\nname=evil\n");
            put(out, "../outside.txt", "pwned");
            put(out, "settings.cbcfg", "x");
        }

        File destProfiles = temp.resolve("profiles").toFile();
        Files.createDirectories(destProfiles.toPath());
        String result = ConfigShare.importPreset(destProfiles, zip);
        assertTrue(ConfigShare.isError(result), result);
        assertTrue(ConfigShare.errorMessage(result).toLowerCase().contains("unsafe"), result);
        assertFalse(Files.exists(temp.resolve("outside.txt")));
        assertFalse(new File(destProfiles, "evil").exists());
    }

    @Test
    void importUniquifiesExistingName() throws Exception {
        File destProfiles = temp.resolve("profiles").toFile();
        Files.createDirectories(new File(destProfiles, "farm").toPath());
        Files.writeString(new File(destProfiles, "farm/keep.txt").toPath(), "original");

        File zip = temp.resolve("farm.cbshare").toFile();
        writeShare(zip, "mod=emcaddons\nmc=1.18.2\nformat=1\nname=farm\n", "settings.cbcfg", "imported");

        String imported = ConfigShare.importPreset(destProfiles, zip);
        assertEquals("farm_2", imported);
        assertEquals("original", Files.readString(new File(destProfiles, "farm/keep.txt").toPath()));
        assertEquals("imported", Files.readString(new File(destProfiles, "farm_2/settings.cbcfg").toPath()));
    }

    @Test
    void importAcceptsSingleRootFolder() throws Exception {
        File zip = temp.resolve("nested.cbshare").toFile();
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            put(out, "raid/manifest.txt", "mod=emcaddons\nmc=1.18.2\nformat=1\nname=raid\n");
            put(out, "raid/settings.cbcfg", "nested");
            put(out, "raid/paths/a.cbcfg", "p");
        }

        File destProfiles = temp.resolve("profiles").toFile();
        String imported = ConfigShare.importPreset(destProfiles, zip);
        assertEquals("raid", imported);
        assertEquals("nested", Files.readString(new File(destProfiles, "raid/settings.cbcfg").toPath()));
        assertEquals("p", Files.readString(new File(destProfiles, "raid/paths/a.cbcfg").toPath()));
    }

    @Test
    void normalizeZipNameRejectsTraversal() {
        assertNull(ConfigShare.normalizeZipName("../evil.txt"));
        assertNull(ConfigShare.normalizeZipName("a/../../b"));
        assertNull(ConfigShare.normalizeZipName("/abs.txt"));
        assertNull(ConfigShare.normalizeZipName("C:/windows.txt"));
        assertEquals("paths/x.cbcfg", ConfigShare.normalizeZipName("paths\\x.cbcfg"));
        assertEquals("settings.cbcfg", ConfigShare.normalizeZipName("./settings.cbcfg"));
    }

    @Test
    void guiResultWrappersMatchTabContract() throws Exception {
        File base = temp.resolve("emcaddons").toFile();
        ConfigProfileManager manager = new ConfigProfileManager(base);
        manager.initialize();
        File profile = manager.getProfileDir(manager.getActiveProfileName() == null ? "default" : manager.getActiveProfileName());
        Files.writeString(profile.toPath().resolve("settings.cbcfg"), "live");

        File zip = temp.resolve("gui.cbshare").toFile();
        ConfigShare.Result exported = ConfigShare.exportProfile(manager, profile.getName(), zip);
        assertTrue(exported.ok, exported.message);

        ConfigShare.Result imported = ConfigShare.importFile(manager, zip);
        assertTrue(imported.ok, imported.message);
        assertNotNull(imported.name);
        assertTrue(imported.name.startsWith(profile.getName()) || imported.name.contains("_"));
    }

    private File profileDir(String name) throws Exception {
        File dir = temp.resolve("profiles/" + name).toFile();
        Files.createDirectories(dir.toPath());
        return dir;
    }

    private static Set<String> entryNames(ZipFile zf) {
        Set<String> names = new HashSet<>();
        zf.stream().forEach(e -> names.add(e.getName().replace('\\', '/')));
        return names;
    }

    private static void writeShare(File zip, String manifest, String fileName, String body) throws Exception {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            put(out, "manifest.txt", manifest);
            put(out, fileName, body);
        }
    }

    private static void put(ZipOutputStream out, String name, String body) throws Exception {
        out.putNextEntry(new ZipEntry(name));
        out.write(body.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }
}
