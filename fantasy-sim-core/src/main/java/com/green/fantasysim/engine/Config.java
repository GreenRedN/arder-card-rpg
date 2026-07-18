package com.green.fantasysim.engine;

import com.green.fantasysim.domain.WorldState;

public class Config {
    public int cycleLengthDays = 30;
    public int summaryEveryDays = 7;

    public double choiceChance = 0.40;
    public double majorChance = 0.12;
    public double autoChance  = 0.48;

    public long seed = 20260205L;

    public int aiCacheBatchSize = 10;

    public Thresholds majorThresholds = new Thresholds();
    public WorldState startingWorld = new WorldState(44, 58, 33, 40);

    public static class Thresholds {
        public int demonGte = 85;
        public int cultLte = 15;
        public int publicMoodLte = 20;
        public int empireLte = 20;
    }
}
