package com.green.fantasysim.engine;

public class ChoiceImpact {
    public String title;
    public String choiceId;
    public String effectSummary;
    public int magnitude;

    public ChoiceImpact() {}
    public ChoiceImpact(String title, String choiceId, String effectSummary, int magnitude) {
        this.title = title;
        this.choiceId = choiceId;
        this.effectSummary = effectSummary;
        this.magnitude = magnitude;
    }
}
