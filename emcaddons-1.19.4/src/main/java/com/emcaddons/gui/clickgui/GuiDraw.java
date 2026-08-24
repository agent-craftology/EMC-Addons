package com.emcaddons.gui.clickgui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

/**
 * 1.18.2 drawing helpers: MatrixStack + DrawableHelper + textRenderer.
 * Icon glyphs match the 1.20.6 ClickGUI.
 */
public final class GuiDraw {

    public enum Icon {
        MARK, PICKAXE, SWORD, LEAF, KEY, GEAR, PATH, BOX,
        MODULES, PATHS, KEYBINDS, SETTINGS, SEARCH
    }

    private static final String[] MARK = {
            ".####...",
            "######..",
            "##..##..",
            "######..",
            "##......",
            "##......",
            "........",
            "........"
    };
    private static final String[] PICKAXE = {
            "..####..",
            ".##..##.",
            "....#.#.",
            "...#..#.",
            "..#...#.",
            ".#......",
            "#.......",
            "........"
    };
    private static final String[] SWORD = {
            "...##...",
            "...##...",
            "...##...",
            "...##...",
            ".######.",
            "...##...",
            "...##...",
            "...##..."
    };
    private static final String[] LEAF = {
            "....#...",
            "...##...",
            "..####..",
            ".######.",
            ".#####.#",
            "..####..",
            "...##...",
            "....#..."
    };
    private static final String[] KEY = {
            ".##.....",
            "#..#....",
            ".##.....",
            "..#.....",
            "..####..",
            "..#..#..",
            "........",
            "........"
    };
    private static final String[] GEAR = {
            ".#.#.#..",
            "#.###.#.",
            ".#####..",
            "###.###.",
            ".#####..",
            "#.###.#.",
            ".#.#.#..",
            "........"
    };
    private static final String[] PATH = {
            "#.......",
            ".#..##..",
            "..##..#.",
            "...#...#",
            "..#.....",
            ".##..#..",
            "....#.#.",
            "........"
    };
    private static final String[] BOX = {
            "######..",
            "#....#..",
            "#....#..",
            "#....#..",
            "#....#..",
            "######..",
            "........",
            "........"
    };
    private static final String[] SEARCH = {
            ".###....",
            "#...#...",
            "#...#...",
            ".###.#..",
            ".....##.",
            "......#.",
            "........",
            "........"
    };

    private final MatrixStack matrices;
    private final TextRenderer textRenderer;
    private boolean scissorOn;

    public GuiDraw(MatrixStack matrices, TextRenderer textRenderer) {
        this.matrices = matrices;
        this.textRenderer = textRenderer;
    }

    public MatrixStack matrices() {
        return matrices;
    }

    public TextRenderer textRenderer() {
        return textRenderer;
    }

    public static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    public void fill(int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        DrawableHelper.fill(matrices, x, y, x + w, y + h, color);
    }

    public void texture(Identifier id, int x, int y, int w, int h, int texW, int texH) {
        if (w <= 0 || h <= 0 || texW <= 0 || texH <= 0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getResourceManager() == null || mc.getResourceManager().getResource(id).isEmpty()) return;
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, id);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        DrawableHelper.drawTexture(matrices, x, y, w, h, 0f, 0f, texW, texH, texW, texH);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    public void fillRoundRect(int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r <= 0) {
            fill(x, y, w, h, color);
            return;
        }
        for (int i = 0; i < h; i++) {
            int inset = 0;
            if (i < r) {
                int dy = r - i;
                inset = r - roundSqrt(r, dy);
            } else if (i >= h - r) {
                int dy = i - (h - r - 1);
                inset = r - roundSqrt(r, dy);
            }
            fill(x + inset, y + i, w - inset * 2, 1, color);
        }
    }

    private static int roundSqrt(int r, int dy) {
        return (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
    }

    /** Soft multi-layer drop shadow behind a rounded panel, offset down-right. */
    public void dropShadow(int x, int y, int w, int h, int radius, int layers) {
        for (int i = layers; i >= 1; i--) {
            int alpha = Math.max(4, (int) (60 * (1.0 - (double) (i - 1) / layers)));
            int color = alpha << 24;
            fillRoundRect(x - i + 3, y - i + 4, w + i * 2, h + i * 2, radius + i, color);
        }
    }

    public void vGradient(int x, int y, int w, int h, int colorTop, int colorBottom) {
        if (w <= 0 || h <= 0) return;
        for (int i = 0; i < h; i++) {
            float t = h <= 1 ? 0f : (float) i / (h - 1);
            fill(x, y + i, w, 1, Anim.lerpColor(colorTop, colorBottom, t));
        }
    }

    /** Small rounded tooltip box drawn near the cursor, clamped to the screen. */
    public void tooltip(String text, int mouseX, int mouseY, int screenW, int screenH) {
        if (text == null || text.isEmpty()) return;
        int padX = 6;
        int padY = 4;
        int tw = width(text) + padX * 2;
        int th = 9 + padY * 2;
        int tx = mouseX + 12;
        int ty = mouseY - th - 6;
        if (tx + tw > screenW) tx = screenW - tw - 2;
        if (ty < 0) ty = mouseY + 16;
        fillRoundRect(tx, ty, tw, th, 5, GuiTheme.TOOLTIP_BORDER);
        fillRoundRect(tx + 1, ty + 1, tw - 2, th - 2, 4, GuiTheme.TOOLTIP_BG);
        text(text, tx + padX, ty + padY, GuiTheme.TITLE);
    }

    public void text(String s, int x, int y, int color) {
        if (s == null) return;
        textRenderer.draw(matrices, s, x, y, color);
    }

    public void textCenter(String s, int cx, int y, int color) {
        if (s == null) return;
        int w = textRenderer.getWidth(s);
        textRenderer.draw(matrices, s, cx - w / 2f, y, color);
    }

    public void textRight(String s, int right, int y, int color) {
        if (s == null) return;
        int w = textRenderer.getWidth(s);
        textRenderer.draw(matrices, s, right - w, y, color);
    }

    /** Glyph drop shadow for in-world HUD overlays. */
    public void textShadow(String s, int x, int y, int color) {
        if (s == null) return;
        textRenderer.drawWithShadow(matrices, s, x, y, color);
    }

    public void textCenterShadow(String s, int cx, int y, int color) {
        if (s == null) return;
        int w = textRenderer.getWidth(s);
        textRenderer.drawWithShadow(matrices, s, cx - w / 2f, y, color);
    }

    public void textRightShadow(String s, int right, int y, int color) {
        if (s == null) return;
        int w = textRenderer.getWidth(s);
        textRenderer.drawWithShadow(matrices, s, right - w, y, color);
    }

    public int width(String s) {
        return s == null ? 0 : textRenderer.getWidth(s);
    }

    public String ellipsize(String s, int maxW) {
        if (s == null) return "";
        if (textRenderer.getWidth(s) <= maxW) return s;
        String ell = "...";
        int ellW = textRenderer.getWidth(ell);
        if (maxW <= ellW) return ell;
        String cut = s;
        while (!cut.isEmpty() && textRenderer.getWidth(cut) + ellW > maxW) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + ell;
    }

    public void enableScissor(int x, int y, int w, int h) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null || w <= 0 || h <= 0) return;
        int[] r = GuiScale.toScreenRect(x, y, w, h);
        x = r[0];
        y = r[1];
        w = r[2];
        h = r[3];
        int scale = (int) client.getWindow().getScaleFactor();
        int screenH = client.getWindow().getScaledHeight();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (screenH - y - h) * scale, w * scale, h * scale);
        scissorOn = true;
    }

    public void disableScissor() {
        if (scissorOn) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            scissorOn = false;
        }
    }

    public void icon(Icon icon, int x, int y, int color) {
        String[] pat = switch (icon) {
            case MARK -> MARK;
            case PICKAXE -> PICKAXE;
            case SWORD -> SWORD;
            case LEAF -> LEAF;
            case KEY, KEYBINDS -> KEY;
            case GEAR, SETTINGS -> GEAR;
            case PATH, PATHS -> PATH;
            case BOX, MODULES -> BOX;
            case SEARCH -> SEARCH;
        };
        for (int row = 0; row < pat.length; row++) {
            String line = pat[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == '#') {
                    fill(x + col, y + row, 1, 1, color);
                }
            }
        }
    }
}
