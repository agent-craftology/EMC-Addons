package com.emcaddons.gui.clickgui;

/**
 * Self-timed exponential-decay float animator used for smooth GUI transitions
 * (hover elevation, toggle knob slide, page fade, scroll easing, etc).
 * Each instance tracks real elapsed time internally, so callers just set a
 * target and read {@link #update(float)} once per render frame.
 */
public final class Anim {
    private float current;
    private float target;
    private long lastNanos = -1L;

    public Anim(float initial) {
        this.current = initial;
        this.target = initial;
    }

    public void set(float value) {
        this.target = value;
    }

    public void snapTo(float value) {
        this.current = value;
        this.target = value;
        this.lastNanos = -1L;
    }

    public float get() {
        return current;
    }

    public float target() {
        return target;
    }

    public boolean isSettled() {
        return Math.abs(target - current) < 0.001f;
    }

    /** Advances the animation and returns the new current value. speed = decay rate per second (higher is snappier). */
    public float update(float speed) {
        long now = System.nanoTime();
        if (lastNanos < 0) {
            lastNanos = now;
            current = target;
            return current;
        }
        float dt = Math.min((now - lastNanos) / 1_000_000_000f, 0.1f);
        lastNanos = now;
        if (isSettled()) {
            current = target;
            return current;
        }
        float factor = 1f - (float) Math.exp(-speed * dt);
        current += (target - current) * factor;
        if (Math.abs(target - current) < 0.001f) current = target;
        return current;
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int ra = aa + Math.round((ba - aa) * t);
        int rr = ar + Math.round((br - ar) * t);
        int rg = ag + Math.round((bg - ag) * t);
        int rb = ab + Math.round((bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }
}
