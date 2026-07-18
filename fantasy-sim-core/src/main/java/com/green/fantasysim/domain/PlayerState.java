package com.green.fantasysim.domain;

import com.green.fantasysim.util.Clamp;

public class PlayerState {
    public String name;   // display name (user-chosen at first life; random on reincarnation)
    public String race;   // human/elf/beast/dwarf
    public String job;    // none/adventurer/knight/mage/priest/paladin
    public String tier;   // job-tier string
    public String origin; // commoner/noble/royal/orphan_cult/nameless
    public String originDetail; // e.g. commoner:farmer, noble:duke, royal:succession_2
    public String faith;  // none/church/demon_worshippers

    public int hp;        // 0..100
    public int gold;      // 0..∞
    public int power;     // 1.. (11..100 typical, 101+ gated)
    public int goodScore, neutralScore, evilScore;

    public PlayerState() {}

    public void apply(Effect e) {
        hp = Clamp.between(hp + e.dHp, 0, 100);
        gold = Math.max(0, gold + e.dGold);
        power = Math.max(1, power + e.dPower);
        goodScore += e.dGood;
        neutralScore += e.dNeutral;
        evilScore += e.dEvil;
    }

    public boolean isDead() { return hp <= 0; }

    public String alignmentLead() {
        if (goodScore > neutralScore && goodScore > evilScore) return "GOOD";
        if (evilScore > goodScore && evilScore > neutralScore) return "EVIL";
        if (neutralScore > goodScore && neutralScore > evilScore) return "NEUTRAL";
        return "MIXED";
    }

    public PlayerState shallowCopy() {
        PlayerState s = new PlayerState();
        s.name = name;
        s.race = race; s.job = job; s.tier = tier; s.origin = origin; s.originDetail = originDetail; s.faith = faith;
        s.hp = hp; s.gold = gold; s.power = power;
        s.goodScore = goodScore; s.neutralScore = neutralScore; s.evilScore = evilScore;
        return s;
    }
}
