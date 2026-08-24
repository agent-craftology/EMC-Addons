package com.emcaddons.gui.clickgui.widget;

import com.emcaddons.gui.clickgui.GuiDraw;
import com.emcaddons.gui.clickgui.GuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class GuiTextField {
    private String text = "";
    private boolean focused;
    private int maxLength = 64;
    private int x;
    private int y;
    private int w;
    private int h = GuiTheme.SEARCH_H;
    private int cursor;
    private Consumer<String> onChange;
    private Predicate<Character> filter;
    private String placeholder = "";
    private boolean numeric;

    public void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
    }

    public void setChangedListener(Consumer<String> onChange) {
        this.onChange = onChange;
    }

    public void setNumeric(boolean numeric) {
        this.numeric = numeric;
        if (numeric) {
            this.filter = c -> Character.isDigit(c) || c == '-';
        }
    }

    public void setFilter(Predicate<Character> filter) {
        this.filter = filter;
    }

    public String getText() {
        return text;
    }

    public void setText(String value) {
        this.text = value == null ? "" : value;
        if (this.text.length() > maxLength) this.text = this.text.substring(0, maxLength);
        cursor = this.text.length();
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) cursor = text.length();
    }

    public boolean contains(double mx, double my) {
        return GuiDraw.hit(mx, my, x, y, w, h);
    }

    public void render(GuiGraphicsExtractor ctx, Font tr, int mouseX, int mouseY) {
        boolean hover = contains(mouseX, mouseY);
        GuiDraw.fillRoundRect(ctx, x, y, w, h, 5, GuiTheme.FIELD);
        int border = focused ? GuiTheme.ACCENT : (hover ? GuiTheme.FIELD_BORDER : GuiTheme.FIELD);
        if (focused || hover) {
            GuiDraw.fill(ctx, x, y, w, 1, border);
            GuiDraw.fill(ctx, x, y + h - 1, w, 1, border);
            GuiDraw.fill(ctx, x, y, 1, h, border);
            GuiDraw.fill(ctx, x + w - 1, y, 1, h, border);
        }

        int textX = x + 6;
        int textY = y + (h - 8) / 2;
        int maxTextW = w - 12;
        if (text.isEmpty() && !focused) {
            GuiDraw.text(ctx, tr, placeholder, textX, textY, GuiTheme.MUTED);
            return;
        }

        String shown = text;
        int prefix = 0;
        while (prefix < cursor && tr.width(shown) > maxTextW) {
            shown = shown.substring(1);
            prefix++;
        }
        while (tr.width(shown) > maxTextW && shown.length() > 1) {
            shown = shown.substring(0, shown.length() - 1);
        }
        GuiDraw.scissor(ctx, x + 4, y, w - 8, h);
        GuiDraw.text(ctx, tr, shown, textX, textY, GuiTheme.TITLE);
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int rel = Math.max(0, cursor - prefix);
            String before = shown.substring(0, Math.min(rel, shown.length()));
            int cx = textX + tr.width(before);
            GuiDraw.fill(ctx, cx, textY - 1, 1, 10, GuiTheme.TITLE);
        }
        GuiDraw.disableScissor(ctx);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        boolean hit = contains(mx, my);
        focused = hit && button == 0;
        if (focused) cursor = text.length();
        return hit;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    public boolean keyPressed(KeyEvent event) {
        if (!focused) return false;
        int keyCode = event.key();
        if (event.isSelectAll()) {
            cursor = text.length();
            return true;
        }
        if (event.isCopy()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(text);
            return true;
        }
        if (event.isCut()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(text);
            setTextAndNotify("");
            return true;
        }
        if (event.isPaste()) {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null) {
                StringBuilder sb = new StringBuilder(text);
                for (char c : clip.toCharArray()) {
                    if (sb.length() >= maxLength) break;
                    if (isAllowed(c)) sb.append(c);
                }
                setTextAndNotify(sb.toString());
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!text.isEmpty() && cursor > 0) {
                int next = cursor - 1;
                setTextAndNotify(text.substring(0, next) + text.substring(cursor));
                cursor = next;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (cursor < text.length()) {
                setTextAndNotify(text.substring(0, cursor) + text.substring(cursor + 1));
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            cursor = Math.max(0, cursor - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            cursor = Math.min(text.length(), cursor + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            cursor = 0;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            cursor = text.length();
            return true;
        }
        return keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!focused) return false;
        if (chr < 32) return false;
        if (!isAllowed(chr)) return false;
        if (text.length() >= maxLength) return true;
        setTextAndNotify(text.substring(0, cursor) + chr + text.substring(cursor));
        cursor++;
        return true;
    }

    private boolean isAllowed(char c) {
        if (c < 32) return false;
        if (filter != null) return filter.test(c);
        return true;
    }

    private void setTextAndNotify(String value) {
        if (value.length() > maxLength) value = value.substring(0, maxLength);
        this.text = value;
        if (cursor > text.length()) cursor = text.length();
        if (onChange != null) onChange.accept(text);
    }
}
