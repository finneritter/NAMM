package com.namm.ui;

public enum IconType {
    BELL(0, 0),
    BELL_MUTED(1, 0),
    SUN(2, 0),
    MOON(3, 0),
    DOT_GREEN(0, 1),
    DOT_RED(1, 1),
    DOT_BLUE(2, 1),
    TOGGLE_ON(0, 2),
    TOGGLE_OFF(1, 2),
    COLLAPSE_DOTS(0, 3);

    public final int u;
    public final int v;
    public static final int SIZE = 16;
    public static final int ATLAS_SIZE = 128;

    IconType(int col, int row) {
        this.u = col * SIZE;
        this.v = row * SIZE;
    }
}
