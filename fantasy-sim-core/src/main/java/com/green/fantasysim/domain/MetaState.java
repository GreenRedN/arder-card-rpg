package com.green.fantasysim.domain;

import com.green.fantasysim.util.Clamp;

public class MetaState {
    public int hardship;
    public int insightEmpire;
    public int insightDemon;
    public int insightCult;
    public int metaPower;

    public MetaState() {}
    public void apply(Effect e) {
        hardship = Clamp.between(hardship + e.dHardship, 0, 100);
        insightEmpire = Clamp.between(insightEmpire + e.dInsightEmpire, 0, 100);
        insightDemon = Clamp.between(insightDemon + e.dInsightDemon, 0, 100);
        insightCult = Clamp.between(insightCult + e.dInsightCult, 0, 100);
        metaPower = Clamp.between(metaPower + e.dMetaPower, 0, 100);
    }
}
