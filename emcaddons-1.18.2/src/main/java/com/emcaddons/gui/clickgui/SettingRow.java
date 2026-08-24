package com.emcaddons.gui.clickgui;

import net.minecraft.client.font.TextRenderer;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Custom setting rows — no vanilla ButtonWidget / SliderWidget.
 */
public abstract class SettingRow {

    protected int x;
    protected int y;
    protected int w;
    protected int h;

    public abstract int height();

    public abstract void render(GuiDraw d, int x, int y, int w, int mouseX, int mouseY);

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        return false;
    }

    public void mouseReleased() {}

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        return false;
    }

    public void tick() {}

    public void collectFields(Consumer<GuiTextField> out) {}

    public void onLeave() {}

    protected void bounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    protected boolean hit(double mx, double my) {
        return GuiTheme.hit(x, y, w, h, mx, my);
    }

    public static final class Section extends SettingRow {
        private final String title;

        public Section(String title) {
            this.title = title;
        }

        @Override
        public int height() {
            return 18;
        }

        @Override
        public void render(GuiDraw d, int x, int y, int w, int mouseX, int mouseY) {
            bounds(x, y, w, height());
            d.text(title, x, y + 4, GuiTheme.MUTED);
        }
    }

    public static final class Label extends SettingRow {
        private final Supplier<String> text;
        private final Supplier<Integer> color;

        public Label(Supplier<String> text, Supplier<Integer> color) {
            this.text = text;
            this.color = color;
        }

        @Override
        public int height() {
            return 16;
        }

        @Override
        public void render(GuiDraw d, int x, int y, int w, int mouseX, int mouseY) {
            bounds(x, y, w, height());
            d.text(text.get(), x, y + 3, color.get());
        }
    }

    public static final class Toggle extends SettingRow {
        private final String label;
        private final BooleanSupplier value;
        private final Runnable onToggle;

        public Toggle(String label, BooleanSupplier value, Runnable onToggle) {
            this.label = label;
            this.value = value;
            this.onToggle = onToggle;
        }

        @Override
        public int height() {
            return 26;
        }

        @Override
        public void render(GuiDraw d, int x, int y, int w, int mouseX, int mouseY) {
            bounds(x, y, w, height());
            boolean on = value.getAsBoolean();
            boolean hover = hit(mouseX, mouseY);
            d.fillRoundRect(x, y, w, height(), GuiTheme.ROW_RADIUS, hover ? GuiTheme.ROW_HOVER : GuiTheme.ROW);
            if (on) d.fill(x, y + 6, 2, height() - 12, GuiTheme.ACCENT);
            d.text(label, x + 10, y + 8, GuiTheme.TITLE);
            String state = on ? "On" : "Off";
            d.textRight(state, x + w - 10, y + 8, on ? GuiTheme.ON : GuiTheme.OFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && hit(mouseX, mouseY)) {
                onToggle.run();
                return true;
            }
            return false;
        }
    }

    public static final class Cycle extends SettingRow {
        private final String label;
        private final Supplier<String> value;
        private final Runnable onCycle;

        public Cycle(String label, Supplier<String> value, Runnable onCycle) {
            this.label = label;
            this.value = value;
            this.onCycle = onCycle;
        }

        @Override
        public int height() {
            return 26;
        }

        @Override
        public void render(GuiDraw d, int x, int y, int w, int mouseX, int mouseY) {
            bounds(x, y, w, height());
            boolean hover = hit(mouseX, mouseY);
            d.fillRoundRect(x, y, w, height(), GuiTheme.ROW_RADIUS, hover ? GuiTheme.ROW_HOVER : GuiTheme.ROW);
            d.text(label, x + 10, y + 8, GuiTheme.TITLE);
            d.textRight(value.get(), x + w - 10, y + 8, GuiTheme.ACCENT);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && hit(mouseX, mouseY)) {
                onCycle.run();
                return true;
            }
            return false;
        }
    }

    public static final class Dual extends SettingRow {
        private final String left;
        private final String right;
        private final BooleanSupplier leftOn;
        private final Runnable onLeft;
        private final Runnable onRight;

        public Dual(String left, String right, BooleanSupplier leftOn, Runnable onLeft, Runnable onRight) {
            this.left = left;
            this.right = right;
            this.leftOn = leftOn;
            this.onLeft = onLeft;
            this.onRight = onRight;
        }

        @Override
        public int height() {
            return 26;
        }

        @Override
        public void render(GuiDraw d, int x, int y, int w, int mouseX, int mouseY) {
            bounds(x, y, w, height());
            int gap = 8;
            int bw = (w - gap) / 2;
            boolean leftSelected = leftOn.getAsBoolean();
            drawPill(d, x, y, bw, left, leftSelected, GuiTheme.hit(x, y, bw, height(), mouseX, mouseY));
            drawPill(d, x + bw + gap, y, w - bw - gap, right, !leftSelected, GuiTheme.hit(x + bw + gap, y, w - bw - gap, height(), mouseX, mouseY));
        }

        private void drawPill(GuiDraw d, int px, int py, int pw, String label, boolean selected, boolean hover) {
            int bg = selected ? GuiTheme.ACCENT_SOFT : (hover ? GuiTheme.PILL : GuiTheme.ROW);
            d.fillRoundRect(px, py, pw, height(), GuiTheme.ROW_RADIUS, bg);
            if (selected) d.fill(px, py + 6, 2, height() - 12, GuiTheme.ACCENT);
            d.textCenter(label, px + pw / 2, py + 8, selected ? GuiTheme.ACCENT : GuiTheme.TITLE);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !hit(mouseX, mouseY)) return false;
            int gap = 8;
            int bw = (w - gap) / 2;
            if (GuiTheme.hit(x, y, bw, h, mouseX, mouseY)) {
                onLeft.run();
            } else {
                onRight.run();
            }
            return true;
        }
    }

    public static final class Slider extends SettingRow {
        private final String label;
        private final int min;
        private final int max;
        private final int step;
        private final String suffix;
        private final IntSupplier getter;
        private final IntConsumer setter;
        private boolean dragging;

        public Slider(String label, int min, int max, int step, String suffix, IntSupplier getter, IntConsumer setter) {
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = Math.max(1, step);
            this.suffix = suffix == null ? "" : suffix;
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public int height() {
            return 36;
        }

        @Override
        public void render(GuiDraw d, int x, int y, int w, int mouseX, int mouseY) {
            bounds(x, y, w, height());
            boolean hover = hit(mouseX, mouseY) || dragging;
            d.fillRoundRect(x, y, w, height(), GuiTheme.ROW_RADIUS, hover ? GuiTheme.ROW_HOVER : GuiTheme.ROW);
            int value = getter.getAsInt();
            d.text(label, x + 10, y + 5, GuiTheme.TITLE);
            d.textRight(value + suffix, x + w - 10, y + 5, GuiTheme.ACCENT);
            int trackX = x + 10;
            int trackW = w - 20;
            int trackY = y + 24;
            d.fillRoundRect(trackX, trackY, trackW, 4, 2, GuiTheme.TRACK);
            float t = max <= min ? 0f : (value - min) / (float) (max - min);
            int fillW = Math.max(4, Math.round(trackW * t));
            d.fillRoundRect(trackX, trackY, fillW, 4, 2, GuiTheme.ACCENT);
            d.fillRoundRect(trackX + fillW - 4, trackY - 2, 8, 8, 4, GuiTheme.KNOB);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && hit(mouseX, mouseY)) {
                dragging = true;
                apply(mouseX);
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY) {
            if (!dragging) return false;
            apply(mouseX);
            return true;
        }

        @Override
        public void mouseReleased() {
            dragging = false;
        }

        private void apply(double mouseX) {
            int trackX = x + 10;
            int trackW = Math.max(1, w - 20);
            double t = (mouseX - trackX) / (double) trackW;
            if (t < 0) t = 0;
            if (t > 1) t = 1;
            int steps = (max - min) / step;
            int idx = (int) Math.round(t * steps);
            setter.accept(min + idx * step);
        }
    }

    public static final class Button extends SettingRow {
        private final Supplier<String> label;
        private final Runnable onClick;
        private final boolean danger;

        public Button(String label, Runnable onClick) {
            this(() -> label, onClick, false);
        }

        public Button(String label, Runnable onClick, boolean danger) {
            this(() -> label, onClick, danger);
        }

        public Button(Supplier<String> label, Runnable onClick, boolean danger) {
            this.label = label;
            this.onClick = onClick;
            this.danger = danger;
        }

        @Override
        public int height() {
            return 26;
        }

        @Override
        public void render(GuiDraw d, int x, int y, int w, int mouseX, int mouseY) {
            bounds(x, y, w, height());
            boolean hover = hit(mouseX, mouseY);
            int bg = hover ? GuiTheme.ROW_HOVER : GuiTheme.ROW;
            d.fillRoundRect(x, y, w, height(), GuiTheme.ROW_RADIUS, bg);
            int color = danger ? GuiTheme.DANGER : GuiTheme.ACCENT;
            d.textCenter(label.get(), x + w / 2, y + 8, color);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && hit(mouseX, mouseY)) {
                onClick.run();
                return true;
            }
            return false;
        }
    }

    public static final class Text extends SettingRow {
        private final String label;
        private final GuiTextField field;

        public Text(TextRenderer tr, String label, int fieldW, int maxLen, String value, String placeholder, Consumer<String> onChange) {
            this.label = label;
            this.field = new GuiTextField(tr, fieldW, 22, maxLen, value, placeholder);
            if (onChange != null) this.field.setChangedListener(onChange);
        }

        @Override
        public int height() {
            return 26;
        }

        @Override
        public void render(GuiDraw d, int x, int y, int w, int mouseX, int mouseY) {
            bounds(x, y, w, height());
            d.text(label, x, y + 8, GuiTheme.MUTED);
            int lx = x + d.width(label) + 8;
            int fw = Math.max(48, w - (lx - x));
            field.setBounds(lx, y, fw, 24);
            field.render(d, mouseX, mouseY, 0f);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return field.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return field.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            return field.charTyped(chr, modifiers);
        }

        @Override
        public void tick() {
            field.tick();
        }

        @Override
        public void collectFields(Consumer<GuiTextField> out) {
            out.accept(field);
        }

        public GuiTextField field() {
            return field;
        }
    }

    /** Integer fields (loop count) using Text chrome with a numeric filter. */
    public static final class Numeric extends SettingRow {
        private final Text inner;

        public Numeric(TextRenderer tr, String label, int fieldW, int maxLen, String value, String placeholder, Consumer<String> onChange) {
            this.inner = new Text(tr, label, fieldW, maxLen, value, placeholder, onChange);
        }

        @Override
        public int height() {
            return inner.height();
        }

        @Override
        public void render(GuiDraw d, int x, int y, int w, int mouseX, int mouseY) {
            inner.render(d, x, y, w, mouseX, mouseY);
            bounds(inner.x, inner.y, inner.w, inner.h);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return inner.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return inner.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            if (chr != 0 && chr != '-' && !Character.isDigit(chr)) {
                return false;
            }
            return inner.charTyped(chr, modifiers);
        }

        @Override
        public void tick() {
            inner.tick();
        }

        @Override
        public void collectFields(Consumer<GuiTextField> out) {
            inner.collectFields(out);
        }

        public GuiTextField field() {
            return inner.field();
        }
    }

    public static final class Area extends SettingRow {
        private final String label;
        private final GuiTextField xField;
        private final GuiTextField yField;
        private final GuiTextField zField;
        private final Runnable onSetHere;
        private final Runnable onClear;
        private final CoordSink sink;
        private int setX, setY, setW;
        private int clearX, clearY, clearW;

        public Area(TextRenderer tr, String label,
                    String x, String y, String z,
                    CoordSink sink, Runnable onSetHere, Runnable onClear) {
            this.label = label;
            this.sink = sink;
            this.onSetHere = onSetHere;
            this.onClear = onClear;
            this.xField = new GuiTextField(tr, 44, 22, 10, x, "X");
            this.yField = new GuiTextField(tr, 44, 22, 10, y, "Y");
            this.zField = new GuiTextField(tr, 44, 22, 10, z, "Z");
            Consumer<String> save = ignored -> flush();
            this.xField.setChangedListener(save);
            this.yField.setChangedListener(save);
            this.zField.setChangedListener(save);
        }

        @Override
        public int height() {
            return 28;
        }

        @Override
        public void render(GuiDraw d, int x, int y, int w, int mouseX, int mouseY) {
            bounds(x, y, w, height());
            d.text(label, x, y + 9, GuiTheme.TITLE);
            int fx = x + 52;
            int fw = 44;
            xField.setBounds(fx, y + 2, fw, 22);
            yField.setBounds(fx + 48, y + 2, fw, 22);
            zField.setBounds(fx + 96, y + 2, fw, 22);
            xField.render(d, mouseX, mouseY, 0f);
            yField.render(d, mouseX, mouseY, 0f);
            zField.render(d, mouseX, mouseY, 0f);

            int bx = fx + 148;
            setW = 58;
            clearW = 44;
            setX = bx;
            setY = y + 2;
            clearX = bx + setW + 6;
            clearY = y + 2;
            boolean setHover = GuiTheme.hit(setX, setY, setW, 22, mouseX, mouseY);
            boolean clearHover = GuiTheme.hit(clearX, clearY, clearW, 22, mouseX, mouseY);
            d.fillRoundRect(setX, setY, setW, 22, 5, setHover ? GuiTheme.ROW_HOVER : GuiTheme.ROW);
            d.fillRoundRect(clearX, clearY, clearW, 22, 5, clearHover ? GuiTheme.ROW_HOVER : GuiTheme.ROW);
            d.textCenter("Set Here", setX + setW / 2, setY + 7, GuiTheme.ACCENT);
            d.textCenter("Clear", clearX + clearW / 2, clearY + 7, GuiTheme.DANGER);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return false;
            if (GuiTheme.hit(setX, setY, setW, 22, mouseX, mouseY)) {
                onSetHere.run();
                return true;
            }
            if (GuiTheme.hit(clearX, clearY, clearW, 22, mouseX, mouseY)) {
                xField.setText("");
                yField.setText("");
                zField.setText("");
                onClear.run();
                return true;
            }
            boolean a = xField.mouseClicked(mouseX, mouseY, button);
            boolean b = yField.mouseClicked(mouseX, mouseY, button);
            boolean c = zField.mouseClicked(mouseX, mouseY, button);
            return a || b || c;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return xField.keyPressed(keyCode, scanCode, modifiers)
                    || yField.keyPressed(keyCode, scanCode, modifiers)
                    || zField.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            return xField.charTyped(chr, modifiers) || yField.charTyped(chr, modifiers) || zField.charTyped(chr, modifiers);
        }

        @Override
        public void tick() {
            xField.tick();
            yField.tick();
            zField.tick();
        }

        @Override
        public void collectFields(Consumer<GuiTextField> out) {
            out.accept(xField);
            out.accept(yField);
            out.accept(zField);
        }

        @Override
        public void onLeave() {
            flush();
        }

        public void setValues(String x, String y, String z) {
            xField.setText(x);
            yField.setText(y);
            zField.setText(z);
            flush();
        }

        private void flush() {
            try {
                String xs = xField.getText().trim();
                String ys = yField.getText().trim();
                String zs = zField.getText().trim();
                if (!xs.isEmpty() && !ys.isEmpty() && !zs.isEmpty()) {
                    sink.accept(Integer.parseInt(xs), Integer.parseInt(ys), Integer.parseInt(zs));
                }
            } catch (NumberFormatException ignored) {
            }
        }

        public interface CoordSink {
            void accept(int x, int y, int z);
        }
    }

    public static final class Keybind extends SettingRow {
        private final String label;
        private final IntSupplier getter;
        private final IntConsumer setter;
        private final boolean clearOnEsc;
        private boolean capturing;

        public Keybind(String label, IntSupplier getter, IntConsumer setter, boolean clearOnEsc) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.clearOnEsc = clearOnEsc;
        }

        public boolean isCapturing() {
            return capturing;
        }

        public void stopCapture() {
            capturing = false;
        }

        @Override
        public int height() {
            return 26;
        }

        @Override
        public void render(GuiDraw d, int x, int y, int w, int mouseX, int mouseY) {
            bounds(x, y, w, height());
            boolean hover = hit(mouseX, mouseY) || capturing;
            d.fillRoundRect(x, y, w, height(), GuiTheme.ROW_RADIUS, hover ? GuiTheme.ROW_HOVER : GuiTheme.ROW);
            d.text(label, x + 10, y + 8, GuiTheme.TITLE);
            String value = capturing
                    ? (clearOnEsc ? "Press key (Esc = None)..." : "Press any key...")
                    : keyName(getter.getAsInt());
            d.textRight(value, x + w - 10, y + 8, capturing ? GuiTheme.PAUSED : GuiTheme.ACCENT);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && hit(mouseX, mouseY)) {
                capturing = true;
                return true;
            }
            return false;
        }

        public boolean captureKey(int keyCode) {
            if (!capturing) return false;
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                if (clearOnEsc) setter.accept(0);
                capturing = false;
                return true;
            }
            setter.accept(keyCode);
            capturing = false;
            return true;
        }

        public static String keyName(int key) {
            if (key == 0) return "None";
            String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
            return name != null ? name : ("Key " + key);
        }
    }
}
