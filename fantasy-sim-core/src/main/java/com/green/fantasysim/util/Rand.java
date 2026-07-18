package com.green.fantasysim.util;

import java.util.Random;

public final class Rand {
    private final Random r;
    public Rand(long seed) { this.r = new Random(seed); }
    public int nextInt(int bound) { return r.nextInt(bound); }
    public double nextDouble() { return r.nextDouble(); }
    public int weightedIndex(int[] weights) {
        int sum = 0;
        for (int w : weights) sum += Math.max(0, w);
        if (sum <= 0) return 0;
        int x = r.nextInt(sum);
        int acc = 0;
        for (int i = 0; i < weights.length; i++) {
            acc += Math.max(0, weights[i]);
            if (x < acc) return i;
        }
        return weights.length - 1;
    }
}
