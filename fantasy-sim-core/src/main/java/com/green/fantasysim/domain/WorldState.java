package com.green.fantasysim.domain;

import com.green.fantasysim.util.Clamp;

public class WorldState {
    public int empire;
    public int demon;
    public int cult;
    public int publicMood;

    public WorldState() {}
    public WorldState(int empire, int demon, int cult, int publicMood) {
        this.empire = empire; this.demon = demon; this.cult = cult; this.publicMood = publicMood;
        clamp();
    }
    public void apply(Effect e) {
        empire = Clamp.between(empire + e.dEmpire, 0, 100);
        demon = Clamp.between(demon + e.dDemon, 0, 100);
        cult = Clamp.between(cult + e.dCult, 0, 100);
        publicMood = Clamp.between(publicMood + e.dPublicMood, 0, 100);
    }
    public void clamp() { apply(new Effect()); }
}
