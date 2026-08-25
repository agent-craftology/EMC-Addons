package com.emcaddons.scoreboard;

import com.emcaddons.gui.clickgui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class HudLayoutManager {
    public enum Anchor {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    public static final class CardState {
        final StatCardSource source;
        Anchor anchor = Anchor.TOP_LEFT;
        int offsetX;
        int offsetY;
        boolean advanced = false;
        boolean visible = true;

        CardState(StatCardSource source, int offsetX, int offsetY) {
            this.source = source;
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

    private final List<CardState> cards = new ArrayList<>();
    private boolean masterVisible = true;
    private CardState draggingCard;
    private int dragOffX;
    private int dragOffY;

    public void register(StatCardSource source, int defaultX, int defaultY) {
        cards.add(new CardState(source, defaultX, defaultY));
    }

    public List<CardState> getCards() {
        return cards;
    }

    public boolean isMasterVisible() {
        return masterVisible;
    }

    public void setMasterVisible(boolean masterVisible) {
        this.masterVisible = masterVisible;
    }

    public CardState get(String id) {
        for (CardState c : cards) {
            if (c.source.id().equals(id)) return c;
        }
        return null;
    }

    public void toggleCard(String id) {
        CardState card = get(id);
        if (card == null) return;
        card.visible = !card.visible;
    }

    public int resolveX(CardState card, int cardW) {
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return switch (card.anchor) {
            case TOP_LEFT, BOTTOM_LEFT -> card.offsetX;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenW - cardW - card.offsetX;
        };
    }

    public int resolveY(CardState card, int cardH) {
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return switch (card.anchor) {
            case TOP_LEFT, TOP_RIGHT -> card.offsetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenH - cardH - card.offsetY;
        };
    }

    public void setAbsolutePosition(CardState card, int absX, int absY, int cardW, int cardH) {
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int centerX = absX + cardW / 2;
        int centerY = absY + cardH / 2;
        boolean left = centerX < screenW / 2;
        boolean top = centerY < screenH / 2;
        card.anchor = left
                ? (top ? Anchor.TOP_LEFT : Anchor.BOTTOM_LEFT)
                : (top ? Anchor.TOP_RIGHT : Anchor.BOTTOM_RIGHT);
        card.offsetX = left ? absX : screenW - cardW - absX;
        card.offsetY = top ? absY : screenH - cardH - absY;
    }

    public void renderAll(GuiGraphicsExtractor ctx) {
        if (!masterVisible) return;
        Minecraft mc = Minecraft.getInstance();
        Font tr = mc.font;
        if (tr == null) return;
        for (CardState c : cards) {
            if (!c.visible || !c.source.isActive()) continue;
            int w = StatCard.width(tr, c.source, c.advanced);
            int h = StatCard.height(c.source, c.advanced);
            int x = resolveX(c, w);
            int y = resolveY(c, h);
            StatCard.render(ctx, tr, c.source, x, y, w, h, c.advanced, false, false, -1, -1);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, Minecraft mc) {
        Font tr = mc.font;
        if (tr == null) return false;
        for (int i = cards.size() - 1; i >= 0; i--) {
            CardState c = cards.get(i);
            if (!c.visible) continue;
            int w = StatCard.width(tr, c.source, c.advanced);
            int h = StatCard.height(c.source, c.advanced);
            int x = resolveX(c, w);
            int y = resolveY(c, h);
            if (StatCard.hitTab(x, y, w, mouseX, mouseY)) {
                c.advanced = !c.advanced;
                return true;
            }
            if (GuiDraw.hit(mouseX, mouseY, x, y, w, h)) {
                draggingCard = c;
                dragOffX = (int) Math.round(mouseX - x);
                dragOffY = (int) Math.round(mouseY - y);
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (draggingCard == null) return false;
        Minecraft mc = Minecraft.getInstance();
        Font tr = mc.font;
        if (tr == null) return false;
        int w = StatCard.width(tr, draggingCard.source, draggingCard.advanced);
        int h = StatCard.height(draggingCard.source, draggingCard.advanced);
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int newX = (int) Math.round(mouseX - dragOffX);
        int newY = (int) Math.round(mouseY - dragOffY);
        newX = Math.max(0, Math.min(screenW - w, newX));
        newY = Math.max(0, Math.min(screenH - h, newY));
        setAbsolutePosition(draggingCard, newX, newY, w, h);
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY) {
        boolean wasDragging = draggingCard != null;
        draggingCard = null;
        return wasDragging;
    }

    public void renderEditOverlay(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Font tr = mc.font;
        if (tr == null) return;
        for (CardState c : cards) {
            if (!c.visible) continue;
            int w = StatCard.width(tr, c.source, c.advanced);
            int h = StatCard.height(c.source, c.advanced);
            int x = resolveX(c, w);
            int y = resolveY(c, h);
            boolean dragging = c == draggingCard;
            StatCard.render(ctx, tr, c.source, x, y, w, h, c.advanced, true, dragging, mouseX, mouseY);
        }
    }

    public void resetPositions() {
        int y = 6;
        for (CardState c : cards) {
            c.anchor = Anchor.TOP_LEFT;
            c.offsetX = 6;
            c.offsetY = y;
            y += 84;
        }
    }

    public void serialize(Properties p) {
        p.setProperty("hud.masterVisible", String.valueOf(masterVisible));
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
        for (CardState c : cards) {
            String id = c.source.id();
            String a = p.getProperty("hud." + id + ".anchor");
            if (a != null) {
                try {
                    c.anchor = Anchor.valueOf(a);
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
    }
}
