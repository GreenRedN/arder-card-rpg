package com.green.fantasysim.engine;

import com.green.fantasysim.domain.MetaState;
import com.green.fantasysim.domain.PlayerState;

/**
 * Tier is derived from job + current power band.
 * Power is the "universal stat" (11..100 common; 101+ gated).
 */
public final class TierProgression {
    private TierProgression(){}

    public static void syncTier(PlayerState p) {
        int x = p.power;

        switch (p.job) {
            case "none" -> p.tier = "commoner";
            case "knight" -> p.tier = band(x,
                    "squire", 30,
                    "knight", 50,
                    "elite_knight", 70,
                    "knight_commander", 85,
                    "sword_master", 100,
                    "transcendent_knight");
            case "mage" -> p.tier = band(x,
                    "apprentice_mage", 30,
                    "mid_mage", 50,
                    "high_mage", 70,
                    "elder_mage", 85,
                    "grand_mage", 100,
                    "transcendent_mage");
            case "priest" -> p.tier = band(x,
                    "novice_priest", 35,
                    "high_priest", 55,
                    "cardinal", 75,
                    "saint", 100,
                    "transcendent_saint");
            case "paladin" -> p.tier = band(x,
                    "paladin_initiate", 40,
                    "paladin", 60,
                    "holy_knight", 80,
                    "templar_commander", 100,
                    "transcendent_paladin");
            default -> { // adventurer
                if (x <= 30) p.tier = "apprentice_adventurer";
                else if (x <= 60) p.tier = "adventurer";
                else if (x <= 100) p.tier = "veteran_adventurer";
                else p.tier = "legendary_adventurer";
            }
        }
    }

    private static String band(int power, String t1, int max1, String t2, int max2,
                               String t3, int max3, String t4, int max4, String t5) {
        if (power <= max1) return t1;
        if (power <= max2) return t2;
        if (power <= max3) return t3;
        if (power <= max4) return t4;
        return t5;
    }

    private static String band(int power, String t1, int max1, String t2, int max2,
                               String t3, int max3, String t4, int max4,
                               String t5, int max5, String t6) {
        if (power <= max1) return t1;
        if (power <= max2) return t2;
        if (power <= max3) return t3;
        if (power <= max4) return t4;
        if (power <= max5) return t5;
        return t6;
    }

    private static String band(int power, String t1, int max1, String t2, int max2,
                               String t3, int max3, String t4, int max4,
                               String t5, int max5, String t6, int max6, String t7) {
        if (power <= max1) return t1;
        if (power <= max2) return t2;
        if (power <= max3) return t3;
        if (power <= max4) return t4;
        if (power <= max5) return t5;
        if (power <= max6) return t6;
        return t7;
    }

    public static int allowedPowerCap(PlayerState p, MetaState meta) {
        // default cap
        int cap = 100;

        // transcend condition (metagame + good alignment)
        boolean good = "GOOD".equals(p.alignmentLead());
        boolean enoughInsight = meta.insightDemon >= 60 || meta.insightEmpire >= 60 || meta.insightCult >= 60;
        if (good && meta.metaPower >= 80 && enoughInsight) cap = 110;

        return cap;
    }
}
