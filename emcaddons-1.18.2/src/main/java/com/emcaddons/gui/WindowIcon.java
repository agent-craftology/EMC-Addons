package com.emcaddons.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.DefaultResourcePack;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class WindowIcon {

    private static final Identifier LOGO = new Identifier("emcaddons", "textures/gui/logo.png");
    private static final Identifier VANILLA_16 = new Identifier("icons/icon_16x16.png");
    private static final Identifier VANILLA_32 = new Identifier("icons/icon_32x32.png");

    private WindowIcon() {}

    public static void apply(MinecraftClient client, boolean craftologyEnabled) {
        if (client == null || client.getWindow() == null) return;
        long handle = client.getWindow().getHandle();
        if (handle == 0L) return;
        try {
            if (craftologyEnabled) {
                applyCraftology(client, handle);
            } else {
                applyVanilla(client, handle);
            }
        } catch (Exception ignored) {
        }
    }

    private static void applyCraftology(MinecraftClient client, long handle) throws Exception {
        NativeImage src = readResource(client, LOGO);
        if (src == null) return;
        NativeImage scaled16 = null;
        NativeImage scaled32 = null;
        try {
            scaled16 = scaleNearest(src, 16, 16);
            scaled32 = scaleNearest(src, 32, 32);
            setGlfwIcons(handle, scaled16, scaled32, src);
        } finally {
            if (scaled16 != null) scaled16.close();
            if (scaled32 != null) scaled32.close();
            src.close();
        }
    }

    private static void applyVanilla(MinecraftClient client, long handle) throws Exception {
        NativeImage icon16 = readVanillaIcon(client, VANILLA_16);
        NativeImage icon32 = readVanillaIcon(client, VANILLA_32);
        if (icon16 == null || icon32 == null) {
            if (icon16 != null) icon16.close();
            if (icon32 != null) icon32.close();
            return;
        }
        try {
            setGlfwIcons(handle, icon16, icon32);
        } finally {
            icon16.close();
            icon32.close();
        }
    }

    private static NativeImage readResource(MinecraftClient client, Identifier id) {
        try {
            ResourceManager manager = client.getResourceManager();
            if (manager == null || !manager.containsResource(id)) return null;
            try (Resource resource = manager.getResource(id)) {
                return NativeImage.read(resource.getInputStream());
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static NativeImage readVanillaIcon(MinecraftClient client, Identifier id) {
        NativeImage fromManager = readResource(client, id);
        if (fromManager != null) return fromManager;
        try {
            DefaultResourcePack pack = client.getResourcePackProvider().getPack();
            if (pack == null || !pack.contains(ResourceType.CLIENT_RESOURCES, id)) return null;
            try (InputStream in = pack.open(ResourceType.CLIENT_RESOURCES, id)) {
                return NativeImage.read(in);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static NativeImage scaleNearest(NativeImage src, int dstW, int dstH) {
        NativeImage dst = new NativeImage(dstW, dstH, false);
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        for (int y = 0; y < dstH; y++) {
            int srcY = y * srcH / dstH;
            for (int x = 0; x < dstW; x++) {
                int srcX = x * srcW / dstW;
                dst.setColor(x, y, src.getColor(srcX, srcY));
            }
        }
        return dst;
    }

    private static ByteBuffer toGlfwRgba(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer buf = MemoryUtil.memAlloc(width * height * 4);
        try {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int abgr = image.getColor(x, y);
                    buf.put((byte) (abgr & 0xFF));
                    buf.put((byte) ((abgr >> 8) & 0xFF));
                    buf.put((byte) ((abgr >> 16) & 0xFF));
                    buf.put((byte) ((abgr >> 24) & 0xFF));
                }
            }
            buf.flip();
            return buf;
        } catch (Throwable t) {
            MemoryUtil.memFree(buf);
            throw t;
        }
    }

    private static void setGlfwIcons(long handle, NativeImage... images) {
        List<ByteBuffer> pixels = new ArrayList<>(images.length);
        GLFWImage.Buffer glfwImages = null;
        try {
            glfwImages = GLFWImage.malloc(images.length);
            for (int i = 0; i < images.length; i++) {
                ByteBuffer rgba = toGlfwRgba(images[i]);
                pixels.add(rgba);
                glfwImages.position(i);
                glfwImages.width(images[i].getWidth());
                glfwImages.height(images[i].getHeight());
                glfwImages.pixels(rgba);
            }
            glfwImages.position(0);
            GLFW.glfwSetWindowIcon(handle, glfwImages);
        } finally {
            if (glfwImages != null) glfwImages.free();
            for (ByteBuffer buf : pixels) {
                MemoryUtil.memFree(buf);
            }
        }
    }
}
