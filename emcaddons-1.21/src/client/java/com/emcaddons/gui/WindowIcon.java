package com.emcaddons.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class WindowIcon {
    private static final Identifier LOGO = Identifier.of("emcaddons", "textures/gui/logo.png");
    private static final Identifier VANILLA_16 = Identifier.of("minecraft", "icons/icon_16x16.png");
    private static final Identifier VANILLA_32 = Identifier.of("minecraft", "icons/icon_32x32.png");

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
        NativeImage src = loadImage(client, LOGO);
        if (src == null) return;
        List<NativeImage> owned = new ArrayList<>();
        owned.add(src);
        try {
            owned.add(scaleNearest(src, 16));
            owned.add(scaleNearest(src, 32));
            setGlfwIcon(handle, owned);
        } finally {
            closeAll(owned);
        }
    }

    private static void applyVanilla(MinecraftClient client, long handle) throws Exception {
        List<NativeImage> owned = new ArrayList<>();
        try {
            NativeImage icon16 = loadImage(client, VANILLA_16);
            if (icon16 != null) owned.add(icon16);
            NativeImage icon32 = loadImage(client, VANILLA_32);
            if (icon32 != null) owned.add(icon32);
            if (owned.isEmpty()) return;
            setGlfwIcon(handle, owned);
        } finally {
            closeAll(owned);
        }
    }

    private static NativeImage loadImage(MinecraftClient client, Identifier id) {
        try {
            Optional<Resource> resource = client.getResourceManager().getResource(id);
            if (resource.isPresent()) {
                try (InputStream in = resource.get().getInputStream()) {
                    return NativeImage.read(in);
                }
            }
            InputSupplier<InputStream> supplier = client.getDefaultResourcePack().open(ResourceType.CLIENT_RESOURCES, id);
            if (supplier == null) return null;
            try (InputStream in = supplier.get()) {
                return NativeImage.read(in);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static NativeImage scaleNearest(NativeImage src, int size) {
        NativeImage dst = new NativeImage(NativeImage.Format.RGBA, size, size, false);
        int sw = src.getWidth();
        int sh = src.getHeight();
        for (int y = 0; y < size; y++) {
            int sy = y * sh / size;
            for (int x = 0; x < size; x++) {
                int sx = x * sw / size;
                dst.setColor(x, y, src.getColor(sx, sy));
            }
        }
        return dst;
    }

    private static void setGlfwIcon(long handle, List<NativeImage> images) {
        if (images.isEmpty()) return;
        GLFWImage.Buffer buffer = GLFWImage.malloc(images.size());
        List<ByteBuffer> pixels = new ArrayList<>(images.size());
        try {
            for (int i = 0; i < images.size(); i++) {
                NativeImage image = images.get(i);
                ByteBuffer rgba = abgrToRgba(image);
                pixels.add(rgba);
                buffer.position(i);
                buffer.width(image.getWidth());
                buffer.height(image.getHeight());
                buffer.pixels(rgba);
            }
            buffer.position(0);
            GLFW.glfwSetWindowIcon(handle, buffer);
        } finally {
            for (ByteBuffer pixelBuffer : pixels) {
                MemoryUtil.memFree(pixelBuffer);
            }
            buffer.free();
        }
    }

    private static ByteBuffer abgrToRgba(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer pixels = MemoryUtil.memAlloc(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int abgr = image.getColor(x, y);
                pixels.put((byte) (abgr & 0xFF));
                pixels.put((byte) ((abgr >> 8) & 0xFF));
                pixels.put((byte) ((abgr >> 16) & 0xFF));
                pixels.put((byte) ((abgr >> 24) & 0xFF));
            }
        }
        pixels.flip();
        return pixels;
    }

    private static void closeAll(List<NativeImage> images) {
        for (NativeImage image : images) {
            image.close();
        }
    }
}
