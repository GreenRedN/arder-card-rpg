package com.green.fantasysim.util;

public final class Clamp {
    private Clamp() {}
    public static int between(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }
}
