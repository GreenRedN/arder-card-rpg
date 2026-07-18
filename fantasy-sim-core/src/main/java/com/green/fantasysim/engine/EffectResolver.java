package com.green.fantasysim.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.green.fantasysim.domain.Effect;
import com.green.fantasysim.domain.PlayerState;
import com.green.fantasysim.domain.TagSet;
import com.green.fantasysim.domain.WorldState;

import java.io.InputStream;
import java.util.*;

/**
 * Loads effects.json (externalized balance table) and resolves a final Effect by:
 * - summing all matching rules
 * - applying turnType + difficulty + world-pressure multipliers
 * - adding alignment score (+1) for GOOD/NEUTRAL/EVIL picks
 *
 * Rules use a flexible matcher:
 * - "when" fields are optional
 * - value may be "*" (wildcard) or "a|b|c" (OR match)
 */
public class EffectResolver {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EffectTable {
        public String version;
        public Map<String, Double> turnTypeMultipliers = new HashMap<>();
        public DifficultyScale difficultyScale = new DifficultyScale();
        public List<Rule> rules = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DifficultyScale {
        public double min = 0.6;
        public double max = 1.4;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rule {
        public String name;
        public Map<String, String> when = new HashMap<>();
        public Effect effect = new Effect();
    }

    private final EffectTable table;
    private final List<Rule> rules;

    public EffectResolver(InputStream effectsJson) {
        try {
            ObjectMapper om = new ObjectMapper();
            this.table = om.readValue(effectsJson, EffectTable.class);
            this.rules = List.copyOf(table.rules);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load effects.json", ex);
        }
    }

    public Effect resolve(TurnType turnType, TagSet tags, WorldState world, PlayerState player, String choiceId) {
        Effect sum = new Effect();

        for (Rule r : rules) {
            if (matches(r.when, turnType, tags, world, player, choiceId)) {
                add(sum, r.effect);
            }
        }

        // Alignment score: always +1 for the selected bucket (unless AUTO/SUMMARY)
        if (choiceId != null) {
            if ("GOOD".equals(choiceId)) sum.dGood += 1;
            else if ("NEUTRAL".equals(choiceId)) sum.dNeutral += 1;
            else if ("EVIL".equals(choiceId)) sum.dEvil += 1;
        }

        // Default-safe: if no rules matched at all, generate a tiny drift so the sim never "freezes".
        if (isZero(sum) && (turnType == TurnType.AUTO || choiceId != null)) {
            sum.dPublicMood += (turnType == TurnType.AUTO ? 0 : 1);
        }

        // Apply multipliers
        double mult = multiplier(turnType, tags, world, choiceId);
        scale(sum, mult, choiceId);

        // Clamp extreme spikes (safety)
        clampEffect(sum);

        return sum;
    }

    private double multiplier(TurnType turnType, TagSet tags, WorldState world, String threatChoiceId) {
        double base = table.turnTypeMultipliers.getOrDefault(turnType.name(), 1.0);

        int diff = 50;
        try { diff = Integer.parseInt(Optional.ofNullable(tags.get("difficulty")).orElse("50")); }
        catch (Exception ignored) {}

        double diffScale = table.difficultyScale.min + (table.difficultyScale.max - table.difficultyScale.min) * (Math.max(0, Math.min(100, diff)) / 100.0);

        String threat = tags.get("threat");
        double pressure = 1.0;
        if ("demon".equals(threat)) pressure += Math.max(0, (world.demon - 60) / 100.0);
        if ("politics".equals(threat) || "war".equals(threat)) pressure += Math.max(0, (40 - world.empire) / 120.0);
        if ("bandit".equals(threat)) pressure += Math.max(0, (35 - world.publicMood) / 120.0);
        if ("faith".equals(threat)) pressure += Math.max(0, (50 - world.cult) / 140.0);
        if ("ruins".equals(threat)) pressure += Math.max(0, (65 - world.cult) / 160.0);

        // Major turns are already weighted, but push a bit harder when demon is critical
        if (turnType == TurnType.MAJOR && world.demon >= 80) pressure += 0.10;

        return base * diffScale * pressure;
    }

    private static boolean matches(Map<String,String> when, TurnType turnType, TagSet tags, WorldState world, PlayerState player, String choiceId) {
        if (when == null || when.isEmpty()) return true;

        for (Map.Entry<String,String> e : when.entrySet()) {
            String k = e.getKey();
            String want = e.getValue();
            if (want == null) continue;

            String got;
            switch (k) {
                case "turnType" -> got = turnType.name();
                case "choiceId" -> got = choiceId;
                case "job" -> got = player.job;
                case "tier" -> got = player.tier;
                case "origin" -> got = player.origin;
                case "race" -> got = player.race;
                case "faith" -> got = player.faith;
                case "worldDemonHigh" -> got = (world.demon >= 70) ? "true" : "false";
                case "worldEmpireLow" -> got = (world.empire <= 30) ? "true" : "false";
                case "worldMoodLow" -> got = (world.publicMood <= 25) ? "true" : "false";
                default -> got = tags.get(k);
            }

            if (!valueMatches(got, want)) return false;
        }
        return true;
    }

    private static boolean valueMatches(String got, String want) {
        if ("*".equals(want)) return true;
        if (got == null) return false;
        if (want.contains("|")) {
            for (String p : want.split("\\|")) if (p.trim().equals(got)) return true;
            return false;
        }
        return want.equals(got);
    }

    private static void add(Effect dst, Effect src) {
        dst.dEmpire += src.dEmpire; dst.dDemon += src.dDemon; dst.dCult += src.dCult; dst.dPublicMood += src.dPublicMood;
        dst.dHp += src.dHp; dst.dGold += src.dGold; dst.dPower += src.dPower;
        dst.dGood += src.dGood; dst.dNeutral += src.dNeutral; dst.dEvil += src.dEvil;
        dst.dHardship += src.dHardship; dst.dInsightEmpire += src.dInsightEmpire; dst.dInsightDemon += src.dInsightDemon; dst.dInsightCult += src.dInsightCult; dst.dMetaPower += src.dMetaPower;
    }

    private static boolean isZero(Effect e) {
        return e.dEmpire==0 && e.dDemon==0 && e.dCult==0 && e.dPublicMood==0 &&
                e.dHp==0 && e.dGold==0 && e.dPower==0 &&
                e.dHardship==0 && e.dInsightEmpire==0 && e.dInsightDemon==0 && e.dInsightCult==0 && e.dMetaPower==0 &&
                e.dGood==0 && e.dNeutral==0 && e.dEvil==0;
    }

    private static void scale(Effect e, double mult, String choiceId) {
        // Don't scale alignment points.
        int g=e.dGood, n=e.dNeutral, v=e.dEvil;

        e.dEmpire = (int)Math.round(e.dEmpire * mult);
        e.dDemon = (int)Math.round(e.dDemon * mult);
        e.dCult = (int)Math.round(e.dCult * mult);
        e.dPublicMood = (int)Math.round(e.dPublicMood * mult);
        e.dHp = (int)Math.round(e.dHp * mult);
        e.dGold = (int)Math.round(e.dGold * mult);
        e.dPower = (int)Math.round(e.dPower * mult);

        e.dHardship = (int)Math.round(e.dHardship * mult);
        e.dInsightEmpire = (int)Math.round(e.dInsightEmpire * mult);
        e.dInsightDemon = (int)Math.round(e.dInsightDemon * mult);
        e.dInsightCult = (int)Math.round(e.dInsightCult * mult);
        e.dMetaPower = (int)Math.round(e.dMetaPower * mult);

        e.dGood = g; e.dNeutral = n; e.dEvil = v;
    }

    private static void clampEffect(Effect e) {
        // keep within sane per-turn magnitude (before WorldState clamps 0..100)
        e.dEmpire = clamp(e.dEmpire, -12, 12);
        e.dDemon = clamp(e.dDemon, -12, 12);
        e.dCult = clamp(e.dCult, -12, 12);
        e.dPublicMood = clamp(e.dPublicMood, -12, 12);

        e.dHp = clamp(e.dHp, -20, 20);
        e.dGold = clamp(e.dGold, -50, 60);
        e.dPower = clamp(e.dPower, -8, 10);

        e.dHardship = clamp(e.dHardship, -10, 12);
        e.dInsightEmpire = clamp(e.dInsightEmpire, -10, 12);
        e.dInsightDemon = clamp(e.dInsightDemon, -10, 12);
        e.dInsightCult = clamp(e.dInsightCult, -10, 12);
        e.dMetaPower = clamp(e.dMetaPower, -10, 12);
    }

    private static int clamp(int x, int lo, int hi) { return Math.max(lo, Math.min(hi, x)); }
}
