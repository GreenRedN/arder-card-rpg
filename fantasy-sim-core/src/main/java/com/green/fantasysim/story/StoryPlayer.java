package com.green.fantasysim.story;

import java.util.ArrayList;
import java.util.List;

public class StoryPlayer {
    public String name;
    public String race;
    public String raceLabel;
    public String job;
    public String jobLabel;
    public int hp;
    public int power;
    public int gold;
    public int insight;
    public int goodScore;
    public int neutralScore;
    public int evilScore;
    public List<String> inventory = new ArrayList<>();

    public StoryPlayer() {}

    public String alignmentLabel() {
        if (goodScore > neutralScore && goodScore > evilScore) return "선의";
        if (evilScore > goodScore && evilScore > neutralScore) return "야심";
        if (neutralScore > goodScore && neutralScore > evilScore) return "실리";
        return "미정";
    }
}
