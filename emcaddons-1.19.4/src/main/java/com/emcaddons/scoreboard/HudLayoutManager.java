package com.emcaddons.scoreboard;

import com.emcaddons.EmcAddonsClient;
import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Registry of draggable HUD stat cards, anchored to the nearest screen corner (1.18.2 MatrixStack).
 *
 * <p>Positions are stored as an anchor plus a pixel offset, which describes only the card's
 * <em>desired</em> spot. Every frame {@link #layout} measures the live card sizes, clamps them into
 * the screen and pushes apart any cards whose horizontal ranges overlap, so a growing card can never
 * render through the one below it.
 */
public final class HudLayoutManager {

    /** Vertical breathing room inserted between two cards that the packer had to separate. */
    private static final int CARD_GAP = 6;

    public static final int SCALE_MIN = 50;
    public static final int SCALE_MAX = 150;
    public static final int SCALE_STEP = 5;

    public enum Anchor {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    public static final class CardState {
        public final StatCardSource source;
        private Anchor anchor;
        private int offsetX;
        private int offsetY;
        private boolean advanced;
        private boolean visible = true;

        private CardState(StatCardSource source, Anchor anchor, int offsetX, int offsetY) {
            this.source = source;
            this.anchor = anchor;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public boolean isAdvanced() {
            return advanced;
        }

        public void setAdvanced(boolean advanced) {
            this.advanced = advanced;
        }
    }

    /**
     * A card's resolved rectangle in card space, i.e. screen space divided by the HUD scale.
     * Instances are pooled and rewritten every frame, so never hold on to one.
     */
    private static final class Rect {
        private CardState card;
        private int index;
        private int x;
        private int y;
        private int w;
        private int h;
        private int desiredY;
        private boolean pinned;
    }

    private final List<CardState> cards = new ArrayList<>();
    private final Map<String, CardState> byId = new HashMap<>();
    private boolean masterVisible = true;
    private boolean advanced = false;
    private int scalePercent = 100;

    private CardState draggingState;
    private int draggingW;
    private int draggingH;
    private double dragOffsetX;
    private double dragOffsetY;

    /** Pooled rectangles plus the per-frame working lists, so the render path allocates nothing. */
    private final List<Rect> rectPool = new ArrayList<>();
    private final List<Rect> frame = new ArrayList<>();
    private final List<Rect> placed = new ArrayList<>();
    private int poolUsed;

    public CardState register(StatCardSource source, int defaultX, int defaultY) {
        CardState state = new CardState(source, Anchor.TOP_LEFT, defaultX, defaultY);
        state.advanced = this.advanced;
        cards.add(state);
        byId.put(source.id(), state);
        return state;
    }

    public List<CardState> getCards() {
        return Collections.unmodifiableList(cards);
    }

    public CardState get(String id) {
        return byId.get(id);
    }

    public void toggleCard(String id) {
        CardState card = byId.get(id);
        if (card == null) return;
        card.visible = !card.visible;
    }

    public boolean isMasterVisible() {
        return masterVisible;
    }

    public void setMasterVisible(boolean visible) {
        this.masterVisible = visible;
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public void setAdvanced(boolean advanced) {
        this.advanced = advanced;
        for (CardState c : cards) {
            c.advanced = advanced;
        }
    }

    public void toggleAdvanced() {
        setAdvanced(!this.advanced);
    }

    public int getScalePercent() {
        return clampScalePercent(scalePercent);
    }

    public void setScalePercent(int percent) {
        int next = clampScalePercent(percent);
        if (next == this.scalePercent) return;
        this.scalePercent = next;
        EmcAddonsClient mod = EmcAddonsClient.getInstance();
        if (mod != null) mod.persistHudLayout();
    }

    private static int clampScalePercent(int percent) {
        if (percent < SCALE_MIN) return SCALE_MIN;
        if (percent > SCALE_MAX) return SCALE_MAX;
        return percent;
    }

    private float scale() {
        return getScalePercent() / 100f;
    }

    public void resetPositions() {
        for (CardState c : cards) {
            c.anchor = Anchor.TOP_LEFT;
            c.offsetX = 6;
            c.offsetY = 6;
        }
    }

    private int resolveX(CardState c, int cardW, int screenW) {
        return switch (c.anchor) {
            case TOP_LEFT, BOTTOM_LEFT -> c.offsetX;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenW - c.offsetX - cardW;
        };
    }

    private int resolveY(CardState c, int cardH, int screenH) {
        return switch (c.anchor) {
            case TOP_LEFT, TOP_RIGHT -> c.offsetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenH - c.offsetY - cardH;
        };
    }

    /** Card-space width of the screen: shrinking the HUD scale buys proportionally more room. */
    private int virtualWidth() {
        return Math.max(1, (int) Math.floor(MinecraftClient.getInstance().getWindow().getScaledWidth() / scale()));
    }

    private int virtualHeight() {
        return Math.max(1, (int) Math.floor(MinecraftClient.getInstance().getWindow().getScaledHeight() / scale()));
    }

    /** {@code absX}/{@code absY} are card-space coordinates, matching what {@link #layout} produces. */
    public void setAbsolutePosition(CardState state, int absX, int absY, int cardW, int cardH) {
        int screenW = virtualWidth();
        int screenH = virtualHeight();
        int cx = absX + cardW / 2;
        int cy = absY + cardH / 2;
        boolean left = cx < screenW / 2;
        boolean top = cy < screenH / 2;
        Anchor anchor = top
                ? (left ? Anchor.TOP_LEFT : Anchor.TOP_RIGHT)
                : (left ? Anchor.BOTTOM_LEFT : Anchor.BOTTOM_RIGHT);
        int offsetX = left ? absX : screenW - absX - cardW;
        int offsetY = top ? absY : screenH - absY - cardH;
        state.anchor = anchor;
        state.offsetX = offsetX;
        state.offsetY = offsetY;
    }

    /**
     * Resolves every card into a non-overlapping rectangle.
     *
     * <p>Cards are measured, clamped into the screen, sorted by desired Y (ties broken by
     * registration index so the order never flickers) and then swept: each card drops below any
     * already-placed card whose X range overlaps it. Testing X overlap rather than the anchor corner
     * means a left-hand and a right-hand card ignore each other, while two cards in the same corner
     * form a column.
     *
     * <p>When {@code pinDragged} is set the card under the cursor keeps its desired Y exactly and the
     * others flow around it, upwards if they wanted to sit above it and downwards otherwise.
     *
     * <p>The returned list is a shared per-frame buffer, invalidated by the next {@code layout} call.
     */
    private List<Rect> layout(boolean includeHidden, boolean pinDragged) {
        frame.clear();
        placed.clear();
        poolUsed = 0;

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        if (tr == null) return frame;

        int screenW = virtualWidth();
        int screenH = virtualHeight();

        for (int i = 0; i < cards.size(); i++) {
            CardState c = cards.get(i);
            if (!includeHidden && (!c.visible || !c.source.isActive())) continue;
            Rect r = obtain();
            r.card = c;
            r.index = i;
            r.w = StatCard.width(tr, c.source, c.advanced);
            r.h = StatCard.height(c.source, c.advanced);
            r.x = clamp(resolveX(c, r.w, screenW), screenW - r.w);
            r.desiredY = clamp(resolveY(c, r.h, screenH), screenH - r.h);
            r.y = r.desiredY;
            r.pinned = pinDragged && c == draggingState;
            frame.add(r);
        }
        sortByDesiredY(frame);

        Rect pinnedRect = null;
        for (int i = 0; i < frame.size(); i++) {
            if (frame.get(i).pinned) {
                pinnedRect = frame.get(i);
                break;
            }
        }

        if (pinnedRect == null) {
            for (int i = 0; i < frame.size(); i++) {
                Rect r = frame.get(i);
                r.y = clamp(packDown(r, r.desiredY), screenH - r.h);
                placed.add(r);
            }
            return frame;
        }

        placed.add(pinnedRect);
        // Cards that wanted to sit above the pinned one, nearest first, lifted out of its way.
        for (int i = frame.size() - 1; i >= 0; i--) {
            Rect r = frame.get(i);
            if (r == pinnedRect || r.desiredY >= pinnedRect.desiredY) continue;
            int y = packUp(r, r.desiredY);
            if (y < 0) y = packDown(r, 0);
            r.y = clamp(y, screenH - r.h);
            placed.add(r);
        }
        for (int i = 0; i < frame.size(); i++) {
            Rect r = frame.get(i);
            if (r == pinnedRect || r.desiredY < pinnedRect.desiredY) continue;
            r.y = clamp(packDown(r, r.desiredY), screenH - r.h);
            placed.add(r);
        }
        return frame;
    }

    /** Lowest Y at or below {@code startY} that clears every already-placed card overlapping in X. */
    private int packDown(Rect r, int startY) {
        int y = startY;
        for (int i = 0; i < placed.size(); i++) {
            Rect o = placed.get(i);
            if (!overlapsX(r, o)) continue;
            int below = o.y + o.h + CARD_GAP;
            if (below > y) y = below;
        }
        return y;
    }

    /** Highest Y at or above {@code startY} that clears every already-placed card overlapping in X. */
    private int packUp(Rect r, int startY) {
        int y = startY;
        for (int i = 0; i < placed.size(); i++) {
            Rect o = placed.get(i);
            if (!overlapsX(r, o)) continue;
            int above = o.y - CARD_GAP - r.h;
            if (above < y) y = above;
        }
        return y;
    }

    private static boolean overlapsX(Rect a, Rect b) {
        return a.x < b.x + b.w && b.x < a.x + a.w;
    }

    private static int clamp(int value, int max) {
        if (max < 0) return 0;
        if (value < 0) return 0;
        return Math.min(value, max);
    }

    /** Insertion sort: stable, allocation-free, and the card count is tiny. */
    private static void sortByDesiredY(List<Rect> list) {
        for (int i = 1; i < list.size(); i++) {
            Rect key = list.get(i);
            int j = i - 1;
            while (j >= 0 && isAfter(list.get(j), key)) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }

    private static boolean isAfter(Rect a, Rect b) {
        if (a.desiredY != b.desiredY) return a.desiredY > b.desiredY;
        return a.index > b.index;
    }

    private Rect obtain() {
        if (poolUsed < rectPool.size()) return rectPool.get(poolUsed++);
        Rect r = new Rect();
        rectPool.add(r);
        poolUsed++;
        return r;
    }

    private boolean pushScale(MatrixStack matrices) {
        float s = scale();
        if (matrices == null || Math.abs(s - 1f) < 0.001f) return false;
        matrices.push();
        matrices.scale(s, s, 1f);
        return true;
    }

    /** Converts an ambient-space mouse coordinate into card space. */
    private double toCardSpace(double coord) {
        return coord / scale();
    }

    public void renderAll(MatrixStack matrices) {
        if (!masterVisible) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.textRenderer == null) return;
        TextRenderer tr = mc.textRenderer;
        List<Rect> rects = layout(false, false);
        if (rects.isEmpty()) return;
        boolean scaled = pushScale(matrices);
        for (int i = 0; i < rects.size(); i++) {
            Rect r = rects.get(i);
            StatCard.render(matrices, tr, r.card.source, r.x, r.y, r.w, r.h, r.card.advanced,
                    false, false, -1, -1);
        }
        if (scaled) matrices.pop();
    }

    public boolean mouseClicked(double mouseX, double mouseY, MinecraftClient mc) {
        if (mc.textRenderer == null) return false;
        double mx = toCardSpace(mouseX);
        double my = toCardSpace(mouseY);
        List<Rect> rects = layout(true, true);
        for (int i = rects.size() - 1; i >= 0; i--) {
            Rect r = rects.get(i);
            if (StatCard.hitTab(r.x, r.y, r.w, mx, my)) {
                toggleAdvanced();
                return true;
            }
            if (mx >= r.x && my >= r.y && mx < r.x + r.w && my < r.y + r.h) {
                draggingState = r.card;
                draggingW = r.w;
                draggingH = r.h;
                dragOffsetX = mx - r.x;
                dragOffsetY = my - r.y;
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (draggingState == null) return false;
        List<Rect> rects = layout(true, true);
        for (int i = 0; i < rects.size(); i++) {
            Rect r = rects.get(i);
            if (r.card == draggingState) {
                draggingW = r.w;
                draggingH = r.h;
                break;
            }
        }
        int absX = (int) Math.round(toCardSpace(mouseX) - dragOffsetX);
        int absY = (int) Math.round(toCardSpace(mouseY) - dragOffsetY);
        setAbsolutePosition(draggingState, absX, absY, draggingW, draggingH);
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY) {
        if (draggingState == null) return false;
        draggingState = null;
        return true;
    }

    public void renderEditOverlay(MatrixStack matrices, int mouseX, int mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.textRenderer == null) return;
        TextRenderer tr = mc.textRenderer;
        List<Rect> rects = layout(true, true);
        int mx = (int) Math.round(toCardSpace(mouseX));
        int my = (int) Math.round(toCardSpace(mouseY));
        boolean scaled = pushScale(matrices);
        for (int i = 0; i < rects.size(); i++) {
            Rect r = rects.get(i);
            StatCard.render(matrices, tr, r.card.source, r.x, r.y, r.w, r.h, r.card.advanced,
                    true, r.card == draggingState, mx, my);
        }
        if (scaled) matrices.pop();
    }

    /**
     * Outlines where the dragged card will land once the pointer is released and the packer stops
     * pinning it. Draws nothing while the card is already sitting in its final slot.
     */
    public void renderDropPreview(MatrixStack matrices) {
        if (draggingState == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.textRenderer == null) return;

        List<Rect> pinnedRects = layout(true, true);
        int pinnedX = Integer.MIN_VALUE;
        int pinnedY = Integer.MIN_VALUE;
        for (int i = 0; i < pinnedRects.size(); i++) {
            Rect r = pinnedRects.get(i);
            if (r.card == draggingState) {
                pinnedX = r.x;
                pinnedY = r.y;
                break;
            }
        }

        List<Rect> settled = layout(true, false);
        Rect target = null;
        for (int i = 0; i < settled.size(); i++) {
            if (settled.get(i).card == draggingState) {
                target = settled.get(i);
                break;
            }
        }
        if (target == null) return;
        if (target.x == pinnedX && target.y == pinnedY) return;

        int accent = target.card.source.accentColor();
        boolean scaled = pushScale(matrices);
        GuiDraw d = new GuiDraw(matrices, mc.textRenderer);
        d.fillRoundRect(target.x, target.y, target.w, target.h, StatCard.RADIUS,
                (accent & 0x00FFFFFF) | 0x22000000);
        drawOutline(d, target.x - 1, target.y - 1, target.w + 2, target.h + 2, GuiTheme.soft(accent));
        if (scaled) matrices.pop();
    }

    private static void drawOutline(GuiDraw d, int x, int y, int w, int h, int color) {
        d.fill(x, y, w, 1, color);
        d.fill(x, y + h - 1, w, 1, color);
        d.fill(x, y + 1, 1, h - 2, color);
        d.fill(x + w - 1, y + 1, 1, h - 2, color);
    }

    public void serialize(Properties p) {
        p.setProperty("hud.masterVisible", String.valueOf(masterVisible));
        p.setProperty("hud.advanced", String.valueOf(advanced));
        p.setProperty("hud.scale", String.valueOf(getScalePercent()));
        for (CardState c : cards) {
            String id = c.source.id();
            p.setProperty("hud." + id + ".anchor", c.anchor.name());
            p.setProperty("hud." + id + ".x", String.valueOf(c.offsetX));
            p.setProperty("hud." + id + ".y", String.valueOf(c.offsetY));
            p.setProperty("hud." + id + ".advanced", String.valueOf(c.advanced));
            p.setProperty("hud." + id + ".visible", String.valueOf(c.visible));
        }
    }

    public void deserialize(Properties p) {
        String mv = p.getProperty("hud.masterVisible");
        if (mv != null) masterVisible = Boolean.parseBoolean(mv);
        String sc = p.getProperty("hud.scale");
        if (sc != null) {
            try {
                scalePercent = clampScalePercent(Integer.parseInt(sc.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        for (CardState c : cards) {
            String id = c.source.id();
            String anchor = p.getProperty("hud." + id + ".anchor");
            if (anchor != null) {
                try {
                    c.anchor = Anchor.valueOf(anchor);
                } catch (IllegalArgumentException ignored) {
                }
            }
            String xs = p.getProperty("hud." + id + ".x");
            if (xs != null) {
                try {
                    c.offsetX = Integer.parseInt(xs);
                } catch (NumberFormatException ignored) {
                }
            }
            String ys = p.getProperty("hud." + id + ".y");
            if (ys != null) {
                try {
                    c.offsetY = Integer.parseInt(ys);
                } catch (NumberFormatException ignored) {
                }
            }
            String adv = p.getProperty("hud." + id + ".advanced");
            if (adv != null) c.advanced = Boolean.parseBoolean(adv);
            String vis = p.getProperty("hud." + id + ".visible");
            if (vis != null) c.visible = Boolean.parseBoolean(vis);
        }
        applyLoadedAdvanced(p);
    }

    private void applyLoadedAdvanced(Properties p) {
        String globalAdv = p.getProperty("hud.advanced");
        if (globalAdv != null) {
            setAdvanced(Boolean.parseBoolean(globalAdv));
            return;
        }
        boolean any = false;
        for (CardState c : cards) {
            if (c.advanced) {
                any = true;
                break;
            }
        }
        setAdvanced(any);
    }
}
