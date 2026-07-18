package com.green.fantasysim.engine;

import com.green.fantasysim.domain.Effect;
import com.green.fantasysim.domain.EventCard;
import com.green.fantasysim.domain.MetaState;
import com.green.fantasysim.domain.PlayerState;
import com.green.fantasysim.domain.TagSet;
import com.green.fantasysim.domain.WorldState;
import com.green.fantasysim.domain.CycleReport;

/**
 * Serializable step output (works for both CLI and API).
 */
public class TurnOutcome {
    public int day;
    public TurnType turnType;
    public TagSet tags;

    public boolean needsChoice;
    public EventCard eventCard;

    public String autoLine;
    public String summaryText;

    public Effect appliedEffect;
    public String appliedEffectSummary;

    public WorldState world;
    public PlayerState player;
    public MetaState meta;

    public boolean ended;
    public String endReason;
    public CycleReport cycleReport;
}
