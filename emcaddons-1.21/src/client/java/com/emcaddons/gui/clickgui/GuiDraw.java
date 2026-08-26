package com.emcaddons.gui.clickgui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class GuiDraw {
    private GuiDraw() {}

    public static void fill(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        ctx.fill(x, y, x + w, y + h, color);
    }

    public static void vGradient(DrawContext ctx, int x, int y, int w, int h, int colorTop, int colorBottom) {
        if (w <= 0 || h <= 0) return;
        ctx.fillGradient(x, y, x + w, y + h, colorTop, colorBottom);
    }

    /** Soft multi-layer drop shadow behind a rounded panel, offset down-right. */
    public static void dropShadow(DrawContext ctx, int x, int y, int w, int h, int radius, int layers) {
        for (int i = layers; i >= 1; i--) {
            int alpha = Math.max(4, (int) (60 * (1.0 - (double) (i - 1) / layers)));
            int color = alpha << 24;
            fillRoundRect(ctx, x - i + 3, y - i + 4, w + i * 2, h + i * 2, radius + i, color);
        }
    }

    public static void fillRoundRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r <= 0) {
            fill(ctx, x, y, w, h, color);
            return;
        }
        // Only the corner bands need per-scanline insets; the middle is one quad.
        for (int i = 0; i < r; i++) {
            int inset = r - roundSqrt(r, r - i);
            fill(ctx, x + inset, y + i, w - inset * 2, 1, color);
        }
        fill(ctx, x, y + r, w, h - r * 2, color);
        for (int i = h - r; i < h; i++) {
            int inset = r - roundSqrt(r, i - (h - r - 1));
            fill(ctx, x + inset, y + i, w - inset * 2, 1, color);
        }
    }

    private static int roundSqrt(int r, int dy) {
        return (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
    }

    public static void fillRoundOutline(DrawContext ctx, int x, int y, int w, int h, int r, int thickness, int color) {
        fillRoundRect(ctx, x, y, w, h, r, color);
        fillRoundRect(ctx, x + thickness, y + thickness, w - thickness * 2, h - thickness * 2, Math.max(0, r - thickness), GuiTheme.WINDOW);
    }

    public static void text(DrawContext ctx, TextRenderer tr, String s, int x, int y, int color) {
        ctx.drawText(tr, s, x, y, color, false);
    }

    public static void textCentered(DrawContext ctx, TextRenderer tr, String s, int cx, int y, int color) {
        if (s == null) return;
        ctx.drawText(tr, s, cx - tr.getWidth(s) / 2, y, color, false);
    }

    /** Glyph drop shadow for in-world HUD overlays. */
    public static void textShadow(DrawContext ctx, TextRenderer tr, String s, int x, int y, int color) {
        ctx.drawTextWithShadow(tr, s, x, y, color);
    }

    public static void textCenteredShadow(DrawContext ctx, TextRenderer tr, String s, int cx, int y, int color) {
        ctx.drawCenteredTextWithShadow(tr, s, cx, y, color);
    }

    public static String ellipsize(TextRenderer tr, String s, int maxW) {
        if (s == null) return "";
        if (tr.getWidth(s) <= maxW) return s;
        String ell = "...";
        int ellW = tr.getWidth(ell);
        if (maxW <= ellW) return ell;
        String cut = s;
        while (cut.length() > 0 && tr.getWidth(cut) + ellW > maxW) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + ell;
    }

    public static void scissor(DrawContext ctx, int x, int y, int w, int h) {
        int[] r = GuiScale.toScreenRect(x, y, w, h);
        ctx.enableScissor(r[0], r[1], r[0] + r[2], r[1] + r[3]);
    }

    public static void disableScissor(DrawContext ctx) {
        ctx.disableScissor();
    }

    public static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    public static boolean hasTexture(Identifier id) {
        if (id == null) return false;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getResourceManager() == null) return false;
            return client.getResourceManager().getResource(id).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public static void texture(DrawContext ctx, Identifier id, int x, int y, int w, int h) {
        texture(ctx, id, x, y, w, h, w, h);
    }

    public static void texture(DrawContext ctx, Identifier id, int x, int y, int w, int h, int texW, int texH) {
        if (w <= 0 || h <= 0 || texW <= 0 || texH <= 0 || !hasTexture(id)) return;
        ctx.drawTexture(id, x, y, w, h, 0f, 0f, texW, texH, texW, texH);
    }

    public enum Icon {
        MARK, PICKAXE, SWORD, LEAF, KEY, GEAR, PATH, BOX
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

    /** Small rounded tooltip box drawn near the cursor, clamped to the screen. */
    public static void tooltip(DrawContext ctx, TextRenderer tr, String text, int mouseX, int mouseY, int screenW, int screenH) {
        if (text == null || text.isEmpty()) return;
        int padX = 6;
        int padY = 4;
        int tw = tr.getWidth(text) + padX * 2;
        int th = 9 + padY * 2;
        int tx = mouseX + 12;
        int ty = mouseY - th - 6;
        if (tx + tw > screenW) tx = screenW - tw - 2;
        if (ty < 0) ty = mouseY + 16;
        fillRoundRect(ctx, tx, ty, tw, th, 5, GuiTheme.TOOLTIP_BORDER);
        fillRoundRect(ctx, tx + 1, ty + 1, tw - 2, th - 2, 4, GuiTheme.TOOLTIP_BG);
        text(ctx, tr, text, tx + padX, ty + padY, GuiTheme.TITLE);
    }

    public static void icon(DrawContext ctx, Icon icon, int x, int y, int color) {
        String[] pat = switch (icon) {
            case MARK -> MARK;
            case PICKAXE -> PICKAXE;
            case SWORD -> SWORD;
            case LEAF -> LEAF;
            case KEY -> KEY;
            case GEAR -> GEAR;
            case PATH -> PATH;
            case BOX -> BOX;
        };
        for (int row = 0; row < pat.length; row++) {
            String line = pat[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == '#') {
                    fill(ctx, x + col, y + row, 1, 1, color);
                }
            }
        }
    }
}
