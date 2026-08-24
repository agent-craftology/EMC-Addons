package com.emcaddons.gui.clickgui;

import com.emcaddons.EmcAddonsClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

/**
 * Click-GUI-only scale (does not change Minecraft's vanilla GUI scale).
 * Drawing is scaled around the screen center; mouse coords use the inverse.
 */
public final class GuiScale {
    public static final float MIN = 0.50f;
    public static final float MAX = 1.50f;
    public static final float DEFAULT = 1.00f;
    public static final int MIN_PCT = 50;
    public static final int MAX_PCT = 150;
    public static final int STEP_PCT = 5;

    private static float active = 1f;
    private static int screenW;
    private static int screenH;
    private static boolean pushed;

    private GuiScale() {}

    public static float current() {
        EmcAddonsClient mod = EmcAddonsClient.getInstance();
        return clamp(mod == null ? DEFAULT : mod.getClickGuiScale());
    }

    public static float clamp(float scale) {
        if (Float.isNaN(scale) || Float.isInfinite(scale)) return DEFAULT;
        if (scale < MIN) return MIN;
        if (scale > MAX) return MAX;
        return Math.round(scale * 20f) / 20f;
    }

    public static int toPercent(float scale) {
        return Math.round(clamp(scale) * 100f);
    }

    public static float fromPercent(int percent) {
        int snapped = MIN_PCT + Math.round((percent - MIN_PCT) / (float) STEP_PCT) * STEP_PCT;
        if (snapped < MIN_PCT) snapped = MIN_PCT;
        if (snapped > MAX_PCT) snapped = MAX_PCT;
        return snapped / 100f;
    }

    public static void begin(GuiGraphicsExtractor ctx, int width, int height) {
        if (ctx == null) return;
        begin(ctx.pose(), width, height);
    }

    public static void begin(Matrix3x2fStack matrices, int width, int height) {
        screenW = width;
        screenH = height;
        active = current();
        pushed = matrices != null && Math.abs(active - 1f) >= 0.001f;
        if (!pushed) return;
        matrices.pushMatrix();
        matrices.translate(width / 2f, height / 2f);
        matrices.scale(active, active);
        matrices.translate(-width / 2f, -height / 2f);
    }

    public static void end(GuiGraphicsExtractor ctx) {
        if (ctx == null) return;
        end(ctx.pose());
    }

    public static void end(Matrix3x2fStack matrices) {
        if (pushed && matrices != null) matrices.popMatrix();
        pushed = false;
        active = 1f;
    }

    public static double mouseX(double screenX, int width) {
        float s = current();
        return width / 2.0 + (screenX - width / 2.0) / s;
    }

    public static double mouseY(double screenY, int height) {
        float s = current();
        return height / 2.0 + (screenY - height / 2.0) / s;
    }

    public static int mouseXi(int screenX, int width) {
        return (int) Math.round(mouseX(screenX, width));
    }

    public static int mouseYi(int screenY, int height) {
        return (int) Math.round(mouseY(screenY, height));
    }

    /** Map a GUI-space rect to screen pixels for scissor while a scale push is active. */
    public static int[] toScreenRect(int x, int y, int w, int h) {
        if (Math.abs(active - 1f) < 0.001f) {
            return new int[] { x, y, w, h };
        }
        float cx = screenW / 2f;
        float cy = screenH / 2f;
        int sx = Math.round(cx + (x - cx) * active);
        int sy = Math.round(cy + (y - cy) * active);
        int sw = Math.max(1, Math.round(w * active));
        int sh = Math.max(1, Math.round(h * active));
        return new int[] { sx, sy, sw, sh };
    }
}
