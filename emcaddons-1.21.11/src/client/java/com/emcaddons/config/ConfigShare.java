package com.emcaddons.config;

import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Shareable {@code .cbshare} export/import for EMC Addons 1.21.11 profiles.
 *
 * <p>A {@code .cbshare} file is a ZIP of one profile directory plus {@code manifest.txt}:
 * <pre>
 * mod=emcaddons
 * mc=1.21.11
 * format=1
 * name=&lt;profile&gt;
 * </pre>
 *
 * <p>API used by the Config tab and {@code /config export|import}:
 * <ul>
 *   <li>{@link #exportPreset(File, String, File)} — {@code null} on success, otherwise an error message</li>
 *   <li>{@link #importPreset(File, File)} — imported profile name, or a string starting with {@link #ERROR_PREFIX}</li>
 * </ul>
 *
 * <p>License and token files are never packed. Import rejects other mod ids and zip-slip paths.
 */
public final class ConfigShare {
    public static final String MOD_ID = "emcaddons";
    public static final String MC_VERSION = "1.21.11";
    public static final int FORMAT = 1;
    public static final String EXTENSION = ".cbshare";
    public static final String MANIFEST_NAME = "manifest.txt";
    public static final String ERROR_PREFIX = "ERROR: ";
    public static final String EXPORTS_DIR_NAME = "exports";

    private static final int MAX_ENTRIES = 2000;
    private static final long MAX_ENTRY_BYTES = 20L * 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 50L * 1024 * 1024;
    private static final Set<String> SECRET_FILE_NAMES = Set.of(
            "license.txt", "license.dat", "token.dat", "token.txt"
    );

    /** GUI-facing result. {@code ok} and {@code message} are read by the Config tab. */
    public static final class Result {
        public final boolean ok;
        public final String message;
        /** Imported profile name on success; otherwise {@code null}. */
        public final String name;

        public Result(boolean ok, String message) {
            this(ok, message, null);
        }

        public Result(boolean ok, String message, String name) {
            this.ok = ok;
            this.message = message;
            this.name = name;
        }

        public static Result ok(String message) {
            return new Result(true, message, null);
        }

        public static Result ok(String message, String name) {
            return new Result(true, message, name);
        }

        public static Result fail(String message) {
            return new Result(false, message, null);
        }
    }

    private ConfigShare() {}

    public static boolean isError(String result) {
        return result == null || result.startsWith(ERROR_PREFIX);
    }

    public static String errorMessage(String result) {
        if (result == null) return "Unknown error";
        if (result.startsWith(ERROR_PREFIX)) return result.substring(ERROR_PREFIX.length());
        return result;
    }

    /**
     * Pack {@code presetName} into {@code destZip}.
     *
     * @param presetDirOrConfigDir the profile folder itself, the {@code profiles/} folder, or
     *        {@code config/emcaddons}
     * @return {@code null} on success, otherwise a player-facing error
     */
    public static String exportPreset(File presetDirOrConfigDir, String presetName, File destZip) {
        if (presetName == null || presetName.isBlank()) return "Profile name is required";
        if (!ConfigProfileManager.isValidName(presetName)) {
            return "Profile name must match [A-Za-z0-9_-]{1,32}";
        }
        if (destZip == null) return "Export path is required";
        File profileDir = resolveProfileDir(presetDirOrConfigDir, presetName);
        if (profileDir == null || !profileDir.isDirectory()) {
            return "Profile not found: " + presetName;
        }
        File dest = ensureCbshareExtension(destZip);
        try {
            File parent = dest.getParentFile();
            if (parent != null) Files.createDirectories(parent.toPath());
            Path root = profileDir.toPath().toAbsolutePath().normalize();
            File tmp = new File(dest.getParentFile() != null ? dest.getParentFile() : dest.getAbsoluteFile().getParentFile(),
                    dest.getName() + ".tmp");
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(tmp.toPath(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                writeManifest(zip, presetName);
                Files.walkFileTree(root, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                            return FileVisitResult.CONTINUE;
                        }
                        String relative = toZipPath(root.relativize(file));
                        if (relative.isEmpty() || relative.equals(MANIFEST_NAME) || isSecretFileName(relative)) {
                            return FileVisitResult.CONTINUE;
                        }
                        ZipEntry entry = new ZipEntry(relative);
                        zip.putNextEntry(entry);
                        Files.copy(file, zip);
                        zip.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (Files.isSymbolicLink(dir) && !dir.equals(root)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            Files.move(tmp.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return null;
        } catch (Exception e) {
            return "Could not write share file: " + e.getMessage();
        }
    }

    public static String exportPreset(ConfigProfileManager manager, String presetName, File destZip) {
        if (manager == null) return "Config profiles are not initialized";
        try {
            return exportPreset(manager.getProfileDir(presetName), presetName, destZip);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /**
     * Extract a {@code .cbshare} into {@code destProfilesDir} (the {@code profiles/} folder).
     * Existing names get a {@code _2}, {@code _3}, … suffix. Never overwrites.
     *
     * @return the imported profile name, or {@link #ERROR_PREFIX} plus a reason
     */
    public static String importPreset(File destProfilesDir, File srcZip) {
        if (destProfilesDir == null) return ERROR_PREFIX + "Profiles folder is missing";
        if (srcZip == null || !srcZip.isFile()) return ERROR_PREFIX + "Share file not found";
        try {
            Files.createDirectories(destProfilesDir.toPath());
            ParsedZip parsed = parseAndValidate(srcZip);
            String name = uniqueName(destProfilesDir, parsed.name);
            File tmp = new File(destProfilesDir, ".import-" + System.nanoTime());
            Files.createDirectories(tmp.toPath());
            boolean moved = false;
            try {
                extractValidated(parsed, srcZip, tmp);
                File dest = new File(destProfilesDir, name);
                if (dest.exists()) {
                    name = uniqueName(destProfilesDir, name);
                    dest = new File(destProfilesDir, name);
                }
                Files.move(tmp.toPath(), dest.toPath());
                moved = true;
                return name;
            } finally {
                if (!moved) deleteRecursively(tmp);
            }
        } catch (ShareException e) {
            return ERROR_PREFIX + e.getMessage();
        } catch (Exception e) {
            return ERROR_PREFIX + "Could not import share: " + e.getMessage();
        }
    }

    public static String importPreset(ConfigProfileManager manager, File srcZip) {
        if (manager == null) return ERROR_PREFIX + "Config profiles are not initialized";
        return importPreset(manager.getProfilesDir(), srcZip);
    }

    public static File defaultExportDir(File baseConfigDir) {
        File downloads = downloadsDir();
        if (baseConfigDir != null) {
            File exports = new File(baseConfigDir, EXPORTS_DIR_NAME);
            exports.mkdirs();
            if (exports.isDirectory()) return exports;
        }
        return downloads;
    }

    /** Config tab: save dialog defaulting to {@code config/emcaddons/exports}. */
    public static File chooseExportFile(ConfigProfileManager manager, String name) {
        File start = manager == null ? downloadsDir() : defaultExportDir(manager.getBaseConfigDir());
        return chooseExportFile(name, start);
    }

    /** Config tab: open dialog defaulting to {@code config/emcaddons/exports} or Downloads. */
    public static File chooseImportFile(ConfigProfileManager manager) {
        File start = manager == null ? downloadsDir() : defaultExportDir(manager.getBaseConfigDir());
        return chooseImportFile(start);
    }

    /** Config tab: pack an existing profile. {@link Result#ok} is true on success. */
    public static Result exportProfile(ConfigProfileManager manager, String name, File dest) {
        String err = exportPreset(manager, name, dest);
        if (err == null) {
            return Result.ok("Exported " + name, name);
        }
        return Result.fail(err);
    }

    /** Config tab: extract a {@code .cbshare} next to other profiles (unique name on collision). */
    public static Result importFile(ConfigProfileManager manager, File file) {
        String result = importPreset(manager, file);
        if (isError(result)) return Result.fail(errorMessage(result));
        return Result.ok("Imported " + result, result);
    }

    public static File chooseExportFile(String suggestedName, File startDir) {
        String fileName = ConfigProfileManager.sanitizeName(suggestedName) + EXTENSION;
        File start = startDir != null && startDir.isDirectory() ? startDir : downloadsDir();
        File picked = showDialog(true, fileName, start);
        return picked == null ? null : ensureCbshareExtension(picked);
    }

    public static File chooseImportFile(File startDir) {
        File start = startDir != null && startDir.isDirectory() ? startDir : downloadsDir();
        return showDialog(false, "*" + EXTENSION, start);
    }

    /** Opens a save dialog off the Minecraft thread, then invokes {@code onChosen} (or {@code onCancel}). */
    public static void chooseExportFileAsync(String suggestedName, File startDir,
                                             Consumer<File> onChosen, Runnable onCancel) {
        runChooser(() -> chooseExportFile(suggestedName, startDir), onChosen, onCancel);
    }

    /** Opens an open dialog off the Minecraft thread, then invokes {@code onChosen} (or {@code onCancel}). */
    public static void chooseImportFileAsync(File startDir, Consumer<File> onChosen, Runnable onCancel) {
        runChooser(() -> chooseImportFile(startDir), onChosen, onCancel);
    }

    public static File resolveProfileDir(File hint, String name) {
        if (hint == null || name == null || name.isBlank()) return null;
        File hintFile = hint.getAbsoluteFile();
        if (hintFile.isDirectory() && name.equals(hintFile.getName())) {
            File parent = hintFile.getParentFile();
            if (parent != null && "profiles".equals(parent.getName())) return hintFile;
            if (new File(hintFile, "settings.cbcfg").isFile() || new File(hintFile, "paths").isDirectory()) {
                return hintFile;
            }
        }
        File direct = new File(hintFile, name);
        if (direct.isDirectory()) return direct;
        File viaProfiles = new File(new File(hintFile, "profiles"), name);
        if (viaProfiles.isDirectory()) return viaProfiles;
        return null;
    }

    private static void runChooser(java.util.function.Supplier<File> picker,
                                   Consumer<File> onChosen, Runnable onCancel) {
        Thread thread = new Thread(() -> {
            File picked;
            try {
                picked = picker.get();
            } catch (Exception e) {
                picked = null;
            }
            File dest = picked;
            if (dest != null) {
                if (onChosen != null) onChosen.accept(dest);
            } else if (onCancel != null) {
                onCancel.run();
            }
        }, "emcaddons-cbshare-chooser");
        thread.setDaemon(true);
        thread.start();
    }

    private static File showDialog(boolean save, String defaultFileName, File startDir) {
        File fromAwt = showFileDialog(save, defaultFileName, startDir);
        if (fromAwt != null) return fromAwt;
        return showSwingChooser(save, defaultFileName, startDir);
    }

    private static File showFileDialog(boolean save, String defaultFileName, File startDir) {
        final File[] result = new File[1];
        try {
            Runnable show = () -> {
                FileDialog dialog = new FileDialog((Frame) null,
                        save ? "Export EMC Addons config" : "Import EMC Addons config",
                        save ? FileDialog.SAVE : FileDialog.LOAD);
                dialog.setDirectory(startDir.getAbsolutePath());
                dialog.setFile(defaultFileName);
                dialog.setFilenameFilter((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(EXTENSION));
                dialog.setVisible(true);
                if (dialog.getFile() == null) return;
                result[0] = new File(dialog.getDirectory(), dialog.getFile());
            };
            if (EventQueue.isDispatchThread()) {
                show.run();
            } else {
                EventQueue.invokeAndWait(show);
            }
        } catch (Exception ignored) {
            return null;
        }
        return result[0];
    }

    private static File showSwingChooser(boolean save, String defaultFileName, File startDir) {
        final File[] result = new File[1];
        try {
            Runnable show = () -> {
                JFileChooser chooser = new JFileChooser(startDir);
                chooser.setDialogTitle(save ? "Export EMC Addons config" : "Import EMC Addons config");
                chooser.setMultiSelectionEnabled(false);
                chooser.setFileFilter(new FileNameExtensionFilter("EMC Addons share (*" + EXTENSION + ")", "cbshare"));
                if (save) {
                    chooser.setSelectedFile(new File(startDir, defaultFileName));
                    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                        result[0] = chooser.getSelectedFile();
                    }
                } else if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    result[0] = chooser.getSelectedFile();
                }
            };
            if (EventQueue.isDispatchThread()) {
                show.run();
            } else {
                EventQueue.invokeAndWait(show);
            }
        } catch (Exception ignored) {
            return null;
        }
        return result[0];
    }

    private static File downloadsDir() {
        String home = System.getProperty("user.home", ".");
        File downloads = new File(home, "Downloads");
        if (downloads.isDirectory()) return downloads;
        return new File(home);
    }

    private static File ensureCbshareExtension(File file) {
        String name = file.getName();
        if (name.toLowerCase(Locale.ROOT).endsWith(EXTENSION)) return file;
        return new File(file.getParentFile(), name + EXTENSION);
    }

    private static void writeManifest(ZipOutputStream zip, String name) throws IOException {
        String body = "mod=" + MOD_ID + "\n"
                + "mc=" + MC_VERSION + "\n"
                + "format=" + FORMAT + "\n"
                + "name=" + name + "\n";
        zip.putNextEntry(new ZipEntry(MANIFEST_NAME));
        zip.write(body.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String toZipPath(Path relative) {
        String path = relative.toString().replace('\\', '/');
        while (path.startsWith("./")) path = path.substring(2);
        return path;
    }

    private static boolean isSecretFileName(String zipPath) {
        int slash = zipPath.lastIndexOf('/');
        String base = (slash >= 0 ? zipPath.substring(slash + 1) : zipPath).toLowerCase(Locale.ROOT);
        return SECRET_FILE_NAMES.contains(base);
    }

    private static ParsedZip parseAndValidate(File srcZip) throws IOException, ShareException {
            Set<String> files = new HashSet<>();
        byte[] manifestBytes = null;
        String manifestEntry = null;
        int entries = 0;
        long total = 0;
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(srcZip.toPath()), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                entries++;
                if (entries > MAX_ENTRIES) throw new ShareException("Share file has too many entries");
                String name = normalizeZipName(entry.getName());
                if (name == null) throw new ShareException("Share file contains an unsafe path");
                if (entry.isDirectory() || name.endsWith("/")) {
                    continue;
                }
                java.io.ByteArrayOutputStream captured = isManifestEntry(name) ? new java.io.ByteArrayOutputStream() : null;
                long size = copyLimited(in, captured, name);
                total += size;
                if (total > MAX_TOTAL_BYTES) throw new ShareException("Share file is too large");
                files.add(name);
                if (captured != null) {
                    manifestEntry = name;
                    manifestBytes = captured.toByteArray();
                }
            }
        }
        if (manifestBytes == null) throw new ShareException("Not a valid .cbshare (missing manifest.txt)");
        Map<String, String> manifest = parseManifest(new String(manifestBytes, StandardCharsets.UTF_8));
        String mod = manifest.get("mod");
        if (mod == null || !MOD_ID.equals(mod)) {
            throw new ShareException("This share is for '" + (mod == null ? "unknown" : mod) + "', not " + MOD_ID);
        }
        String mc = manifest.get("mc");
        if (mc == null || !MC_VERSION.equals(mc)) {
            throw new ShareException("This share is for Minecraft " + (mc == null ? "unknown" : mc) + ", not " + MC_VERSION);
        }
        String format = manifest.getOrDefault("format", "1");
        int formatValue;
        try {
            formatValue = Integer.parseInt(format);
        } catch (NumberFormatException e) {
            throw new ShareException("Unsupported share format");
        }
        if (formatValue > FORMAT) throw new ShareException("Unsupported share format " + formatValue);
        String profileName = ConfigProfileManager.sanitizeName(manifest.get("name"));
        String contentRoot = contentRootOf(manifestEntry);
        if (files.isEmpty()) throw new ShareException("Share file has no profile files");
        return new ParsedZip(profileName, contentRoot);
    }

    private static void extractValidated(ParsedZip parsed, File srcZip, File destDir) throws IOException, ShareException {
        Path destRoot = destDir.toPath().toAbsolutePath().normalize();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(srcZip.toPath()), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String name = normalizeZipName(entry.getName());
                if (name == null) throw new ShareException("Share file contains an unsafe path");
                if (entry.isDirectory() || name.endsWith("/")) continue;
                if (isManifestEntry(name) || isSecretFileName(name)) continue;
                String relative = stripRoot(name, parsed.contentRoot);
                if (relative == null || relative.isEmpty()) continue;
                Path out = safeResolve(destRoot, relative);
                Files.createDirectories(out.getParent());
                try (OutputStream outStream = Files.newOutputStream(out)) {
                    copyLimited(in, outStream, name);
                }
            }
        }
    }

    private static long copyLimited(InputStream in, OutputStream out, String name) throws IOException, ShareException {
        byte[] buf = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(buf)) >= 0) {
            total += read;
            if (total > MAX_ENTRY_BYTES) throw new ShareException("Share entry is too large: " + name);
            if (out != null && read > 0) out.write(buf, 0, read);
        }
        return total;
    }

    private static boolean isManifestEntry(String name) {
        return MANIFEST_NAME.equals(name) || name.endsWith("/" + MANIFEST_NAME);
    }

    private static String contentRootOf(String manifestEntry) {
        int slash = manifestEntry.lastIndexOf('/');
        if (slash < 0) return "";
        return manifestEntry.substring(0, slash + 1);
    }

    private static String stripRoot(String name, String contentRoot) {
        if (contentRoot == null || contentRoot.isEmpty()) return name;
        if (!name.startsWith(contentRoot)) return null;
        return name.substring(contentRoot.length());
    }

    /**
     * @return normalized {@code /}-separated relative path, or {@code null} if the entry is unsafe
     */
    static String normalizeZipName(String raw) {
        if (raw == null) return null;
        String name = raw.replace('\\', '/');
        if (name.contains("\\")) return null;
        if (name.startsWith("/") || name.contains("://")) return null;
        if (name.length() >= 2 && name.charAt(1) == ':') return null;
        String[] parts = name.split("/");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty() || ".".equals(part)) continue;
            if ("..".equals(part)) return null;
            if (out.length() > 0) out.append('/');
            out.append(part);
        }
        if (name.endsWith("/") && out.length() > 0) out.append('/');
        return out.toString();
    }

    static Path safeResolve(Path destRoot, String relative) throws ShareException {
        Path dest = destRoot.toAbsolutePath().normalize();
        Path resolved = dest.resolve(relative.replace('/', File.separatorChar)).normalize();
        if (!resolved.startsWith(dest)) {
            throw new ShareException("Share file contains an unsafe path");
        }
        return resolved;
    }

    private static Map<String, String> parseManifest(String body) {
        Map<String, String> map = new HashMap<>();
        for (String line : body.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int eq = trimmed.indexOf('=');
            if (eq <= 0) continue;
            map.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
        }
        return map;
    }

    private static String uniqueName(File destProfilesDir, String desired) {
        String base = ConfigProfileManager.sanitizeName(desired);
        File first = new File(destProfilesDir, base);
        if (!first.exists()) return base;
        for (int n = 2; n < 1000; n++) {
            String suffix = "_" + n;
            int max = 32 - suffix.length();
            String stem = base.length() <= max ? base : base.substring(0, max);
            String candidate = stem + suffix;
            if (!new File(destProfilesDir, candidate).exists()) return candidate;
        }
        return base + "_" + System.nanoTime();
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) deleteRecursively(child);
        }
        file.delete();
    }

    private static final class ParsedZip {
        final String name;
        final String contentRoot;

        ParsedZip(String name, String contentRoot) {
            this.name = name;
            this.contentRoot = contentRoot;
        }
    }

    static final class ShareException extends Exception {
        ShareException(String message) {
            super(message);
        }
    }
}
