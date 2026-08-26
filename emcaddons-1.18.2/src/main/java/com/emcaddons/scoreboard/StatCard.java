package com.emcaddons.scoreboard;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draggable HUD stat card chrome, pixel-matched to TalonMC 1.20.6
 * (B/A pill, header icon, advanced sparkline) on 1.18.2 MatrixStack.
 */
public final class StatCard {
    public static final int RADIUS = 8;

    /**
     * Trades sparkline fidelity for draw calls. Lower settings widen each sampled column,
     * swap the per-column gradient for a flat fill and square off the track, which lets
     * {@link GuiDraw#fillRoundRect} collapse to a single quad instead of one per scanline.
     */
    public enum GraphQuality {
        HIGH("High", 1, 1, true, 4, true),
        MEDIUM("Medium", 2, 3, true, 4, true),
        LOW("Low", 4, 4, false, 0, false);

        public final String displayName;
        final int columnStep;
        /** Snapping the line to every Nth row merges far more columns than widening them. */
        final int yQuantize;
        final boolean gradientArea;
        final int trackRadius;
        final boolean headDot;

        GraphQuality(String displayName, int columnStep, int yQuantize, boolean gradientArea,
                     int trackRadius, boolean headDot) {
            this.displayName = displayName;
            this.columnStep = columnStep;
            this.yQuantize = yQuantize;
            this.gradientArea = gradientArea;
            this.trackRadius = trackRadius;
            this.headDot = headDot;
        }

        public static String[] displayNames() {
            GraphQuality[] all = values();
            String[] names = new String[all.length];
            for (int i = 0; i < all.length; i++) names[i] = all[i].displayName;
            return names;
        }

        public static GraphQuality fromIndex(int index) {
            GraphQuality[] all = values();
            return index >= 0 && index < all.length ? all[index] : HIGH;
        }
    }

    private static final int PAD_LEFT = 10;
    private static final int PAD_RIGHT = 10;
    private static final int PAD_TOP = 6;
    private static final int PAD_BOTTOM = 6;
    private static final int HEADER_H = 20;
    private static final int ROW_H = 12;
    private static final int PROGRESS_EXTRA = 5;
    private static final int ICON_W = 8;
    private static final int ICON_GAP = 6;
    private static final int PILL_W = 34;
    private static final int PILL_H = 14;
    private static final int PILL_GAP = 8;
    private static final int COL_GAP = 12;
    private static final int MIN_W = 150;
    private static final int HANDLE_W = 4;
    private static final int HANDLE_GAP = 6;
    private static final int GRAPH_H = 36;
    private static final int GRAPH_LABEL_H = 10;
    /** Breathing room between the sparkline and the first stat row below it. */
    private static final int GRAPH_GAP = 4;
    /** Vertical space a {@link StatRow#separator()} occupies, line included. */
    private static final int SEPARATOR_H = 6;

    /** Sparkline geometry per card id, rebuilt only when the data, width or quality changes. */
    private static final Map<String, SparkCache> SPARK_CACHE = new HashMap<>();

    private StatCard() {}

    /**
     * Run-length encoded sparkline columns. Coordinates are relative to the graph box so the
     * cache survives the card being dragged around.
     */
    private static final class SparkCache {
        int version = Integer.MIN_VALUE;
        int width = -1;
        int quality = -1;
        int[] runX = new int[0];
        int[] runW = new int[0];
        int[] runY = new int[0];
        int runCount;
        int lastLineY;
        boolean flat;
    }

    public static int width(TextRenderer tr, StatCardSource source, boolean advanced) {
        int titleW = tr.getWidth(source.title());
        int iconSpace = source.showIcon() ? ICON_W + ICON_GAP : 0;
        int headerW = PAD_LEFT + iconSpace + titleW + PILL_GAP + PILL_W + PAD_RIGHT;
        int maxLabel = 0;
        int maxValue = 0;
        for (StatRow row : rows(source, advanced)) {
            if (row.separator) continue;
            maxLabel = Math.max(maxLabel, tr.getWidth(row.label));
            maxValue = Math.max(maxValue, tr.getWidth(row.value));
        }
        int rowW = PAD_LEFT + maxLabel + COL_GAP + maxValue + PAD_RIGHT;
        return Math.max(MIN_W, Math.max(headerW, rowW));
    }

    public static int height(StatCardSource source, boolean advanced) {
        int rowsH = 0;
        for (StatRow row : rows(source, advanced)) {
            if (row.separator) {
                rowsH += SEPARATOR_H;
                continue;
            }
            rowsH += ROW_H + (row.progress >= 0 ? PROGRESS_EXTRA : 0);
        }
        if (hasSparkline(source, advanced)) {
            rowsH += GRAPH_LABEL_H + GRAPH_H + GRAPH_GAP;
        }
        return PAD_TOP + HEADER_H + rowsH + PAD_BOTTOM;
    }

    public static boolean hitTab(int cardX, int cardY, int cardW, double mouseX, double mouseY) {
        int px = cardX + cardW - PAD_RIGHT - PILL_W;
        int py = cardY + PAD_TOP + 3;
        return GuiDraw.hit(mouseX, mouseY, px, py, PILL_W, PILL_H);
    }

    public static void render(MatrixStack matrices, TextRenderer tr, StatCardSource source,
                              int x, int y, int w, int h, boolean advanced,
                              boolean editMode, boolean dragging, int mouseX, int mouseY) {
        GuiDraw d = new GuiDraw(matrices, tr);
        int accent = source.accentColor();

        d.dropShadow(x, y, w, h, RADIUS, 3);
        if (editMode) {
            int outlineColor = dragging ? accent : GuiTheme.FIELD_BORDER;
            d.fillRoundRect(x - 1, y - 1, w + 2, h + 2, RADIUS + 1, outlineColor);
        }
        d.fillRoundRect(x, y, w, h, RADIUS, GuiTheme.HUD_BG);
        d.fill(x + 3, y + 6, 3, h - 12, accent);

        int headerY = y + PAD_TOP;
        int titleX = x + PAD_LEFT;
        if (source.showIcon() && source.icon() != null) {
            d.icon(source.icon(), x + PAD_LEFT, headerY + 6, accent);
            titleX = x + PAD_LEFT + ICON_W + ICON_GAP;
        }
        d.textShadow(source.title(), titleX, headerY + 6, GuiTheme.HUD_TITLE);

        int pillX = x + w - PAD_RIGHT - PILL_W;
        int pillY = headerY + 3;
        int halfW = PILL_W / 2;
        d.fillRoundRect(pillX, pillY, PILL_W, PILL_H, 7, GuiTheme.PILL);
        int activeX = advanced ? pillX + halfW : pillX;
        d.fillRoundRect(activeX, pillY, halfW, PILL_H, 7, GuiTheme.ACCENT_SOFT);
        d.textCenterShadow("B", pillX + halfW / 2, pillY + 3, advanced ? GuiTheme.HUD_MUTED : GuiTheme.HUD_TITLE);
        d.textCenterShadow("A", pillX + halfW + halfW / 2, pillY + 3, advanced ? GuiTheme.HUD_TITLE : GuiTheme.HUD_MUTED);

        if (editMode) {
            int handleX = pillX - HANDLE_GAP - HANDLE_W;
            drawDragHandle(d, handleX, headerY + 2, dragging ? accent : GuiTheme.HUD_MUTED);
        }

        int dividerY = headerY + HEADER_H - 2;
        d.fill(x + PAD_LEFT, dividerY, w - PAD_LEFT - PAD_RIGHT, 1, GuiTheme.soft(accent));

        int rowY = y + PAD_TOP + HEADER_H;
        int valueRight = x + w - PAD_RIGHT;
        if (hasSparkline(source, advanced)) {
            int graphW = w - PAD_LEFT - PAD_RIGHT;
            drawSparkline(d, source, x + PAD_LEFT, rowY, graphW);
            rowY += GRAPH_LABEL_H + GRAPH_H + GRAPH_GAP;
        }
        for (StatRow row : rows(source, advanced)) {
            if (row.separator) {
                d.fill(x + PAD_LEFT, rowY + SEPARATOR_H / 2, w - PAD_LEFT - PAD_RIGHT, 1, GuiTheme.soft(accent));
                rowY += SEPARATOR_H;
                continue;
            }
            d.textShadow(row.label, x + PAD_LEFT, rowY, GuiTheme.HUD_MUTED);
            int vw = tr.getWidth(row.value);
            int valueColor = row.valueColor == GuiTheme.TITLE ? GuiTheme.HUD_TITLE : row.valueColor;
            d.textShadow(row.value, valueRight - vw, rowY, valueColor);
            rowY += ROW_H;
            if (row.progress >= 0) {
                int barW = w - PAD_LEFT - PAD_RIGHT;
                int barY = rowY + 1;
                d.fill(x + PAD_LEFT, barY, barW, 2, GuiTheme.TRACK);
                int fillW = Math.round(barW * Math.max(0f, Math.min(1f, row.progress)));
                d.fill(x + PAD_LEFT, barY, fillW, 2, accent);
                rowY += PROGRESS_EXTRA;
            }
        }
    }

    private static void drawDragHandle(GuiDraw d, int x, int y, int color) {
        for (int col = 0; col < 2; col++) {
            for (int row = 0; row < 4; row++) {
                d.fill(x + col * 3, y + row * 3, 1, 1, color);
            }
        }
    }

    private static List<StatRow> rows(StatCardSource source, boolean advanced) {
        return advanced ? source.advancedRows() : source.basicRows();
    }

    private static boolean hasSparkline(StatCardSource source, boolean advanced) {
        if (!advanced) return false;
        double[] values = source.sparklineValues();
        return values != null && values.length > 0;
    }

    private static void drawSparkline(GuiDraw d, StatCardSource source, int x, int y, int w) {
        String label = source.sparklineLabel();
        if (label != null && !label.isEmpty()) {
            d.textShadow(label, x, y, GuiTheme.HUD_MUTED);
        }
        String valueText = source.sparklineValueText();
        if (valueText != null && !valueText.isEmpty()) {
            d.textRightShadow(valueText, x + w, y, source.accentColor());
        }
        double[] values = source.sparklineValues();
        if (values == null || values.length == 0 || w <= 0) return;

        GraphQuality quality = quality(source);
        int graphY = y + GRAPH_LABEL_H;
        d.fillRoundRect(x, graphY, w, GRAPH_H, quality.trackRadius, GuiTheme.TRACK);

        SparkCache cache = SPARK_CACHE.computeIfAbsent(source.id(), id -> new SparkCache());
        int version = source.sparklineVersion();
        if (cache.version != version || cache.width != w || cache.quality != quality.ordinal()) {
            buildSparkCache(cache, values, w, quality);
            cache.version = version;
            cache.width = w;
            cache.quality = quality.ordinal();
        }

        int accent = source.accentColor();
        if (cache.flat) {
            d.fill(x, graphY + GRAPH_H / 2, w, 1, accent);
            return;
        }

        int graphBottom = graphY + GRAPH_H;
        int areaTop = (accent & 0x00FFFFFF) | 0x55000000;
        int areaBottom = accent & 0x00FFFFFF;
        int areaFlat = (accent & 0x00FFFFFF) | 0x2A000000;
        for (int i = 0; i < cache.runCount; i++) {
            int rx = x + cache.runX[i];
            int ry = graphY + cache.runY[i];
            int rw = cache.runW[i];
            if (quality.gradientArea) {
                d.vGradient(rx, ry, rw, graphBottom - ry, areaTop, areaBottom);
            } else {
                d.fill(rx, ry, rw, graphBottom - ry, areaFlat);
            }
            d.fill(rx, ry, rw, 2, accent);
        }
        if (quality.headDot) {
            int hx = Math.max(x, Math.min(x + w - 5, x + w - 1 - 2));
            int hy = Math.max(graphY, Math.min(graphBottom - 5, graphY + cache.lastLineY - 2));
            d.fillRoundRect(hx, hy, 5, 5, 2, accent);
        }
    }

    private static GraphQuality quality(StatCardSource source) {
        GraphQuality quality = source.graphQuality();
        return quality != null ? quality : GraphQuality.HIGH;
    }

    private static void buildSparkCache(SparkCache cache, double[] values, int w, GraphQuality quality) {
        cache.runCount = 0;
        cache.flat = false;
        cache.lastLineY = GRAPH_H / 2;
        int n = values.length;
        double min = values[0];
        double max = values[0];
        for (int i = 1; i < n; i++) {
            if (values[i] < min) min = values[i];
            if (values[i] > max) max = values[i];
        }
        if (max == min) {
            cache.flat = true;
            return;
        }
        double range = max - min;
        int step = Math.max(1, quality.columnStep);
        int capacity = (w + step - 1) / step;
        if (cache.runX.length < capacity) {
            cache.runX = new int[capacity];
            cache.runW = new int[capacity];
            cache.runY = new int[capacity];
        }
        int previousY = Integer.MIN_VALUE;
        for (int px = 0; px < w; px += step) {
            int columnW = Math.min(step, w - px);
            double idx = (w <= 1) ? (n - 1) : px / (double) (w - 1) * (n - 1);
            int i0 = (int) Math.floor(idx);
            int i1 = Math.min(n - 1, i0 + 1);
            double frac = idx - i0;
            double sample = values[i0] + (values[i1] - values[i0]) * frac;
            double t = (sample - min) / range;
            int lineY = (int) Math.round((1.0 - t) * (GRAPH_H - 1));
            if (quality.yQuantize > 1) lineY = (lineY / quality.yQuantize) * quality.yQuantize;
            lineY = Math.max(0, Math.min(GRAPH_H - 2, lineY));
            if (lineY == previousY && cache.runCount > 0) {
                cache.runW[cache.runCount - 1] += columnW;
            } else {
                cache.runX[cache.runCount] = px;
                cache.runW[cache.runCount] = columnW;
                cache.runY[cache.runCount] = lineY;
                cache.runCount++;
                previousY = lineY;
            }
            cache.lastLineY = lineY;
        }
    }
}
