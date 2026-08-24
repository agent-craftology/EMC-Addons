package com.emcaddons.scoreboard;

public final class StatRow {
    public final String label;
    public final String value;
    public final int valueColor;
    public final float progress;

    public StatRow(String label, String value) {
        this(label, value, com.emcaddons.gui.clickgui.GuiTheme.TITLE, -1f);
    }

    public StatRow(String label, String value, int valueColor) {
        this(label, value, valueColor, -1f);
    }

    public StatRow(String label, String value, int valueColor, float progress) {
        this.label = label;
        this.value = value;
        this.valueColor = valueColor;
        this.progress = progress;
    }
}
