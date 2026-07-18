package com.green.fantasysim.engine;

import com.green.fantasysim.domain.EventCard;
import com.green.fantasysim.domain.TagSet;

public class PendingChoice {
    public TurnType turnType;
    public int day;
    public TagSet tags;
    public EventCard card;

    public PendingChoice() {}
    public PendingChoice(TurnType turnType, int day, TagSet tags, EventCard card) {
        this.turnType = turnType;
        this.day = day;
        this.tags = tags;
        this.card = card;
    }
}
