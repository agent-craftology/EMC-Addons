package com.emcaddons.scoreboard;

import com.emcaddons.gui.clickgui.GuiDraw;

import java.util.List;

public interface StatCardSource {
    String id();

    String title();

    GuiDraw.Icon icon();

    int accentColor();

    boolean isActive();

    List<StatRow> basicRows();

    List<StatRow> advancedRows();

    default double[] sparklineValues() { return null; }

    default String sparklineLabel() { return ""; }

    default boolean showIcon() { return true; }

    default String sparklineValueText() { return ""; }
}
