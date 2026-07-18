package com.green.fantasysim.story;

public class StoryRelationship {
    public String characterId;
    public String name;
    public String role;
    public int affinity;
    public int trust;
    public int guard;

    public StoryRelationship() {}

    public StoryRelationship(String characterId, String name, String role, int affinity, int trust, int guard) {
        this.characterId = characterId;
        this.name = name;
        this.role = role;
        this.affinity = affinity;
        this.trust = trust;
        this.guard = guard;
    }

    public String stageLabel() {
        if (guard >= 65) return "강한 경계";
        if (trust >= 70) return "굳은 신뢰";
        if (trust >= 40) return "동료";
        if (affinity >= 30) return "호의";
        if (guard >= 35) return "경계";
        return "낯선 사이";
    }
}
