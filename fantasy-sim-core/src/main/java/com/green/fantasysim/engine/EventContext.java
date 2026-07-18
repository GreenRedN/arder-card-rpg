package com.green.fantasysim.engine;

import com.green.fantasysim.domain.MetaState;
import com.green.fantasysim.domain.PlayerState;
import com.green.fantasysim.domain.TagSet;
import com.green.fantasysim.domain.WorldState;

public class EventContext {
    public TurnType turnType;
    public int day;
    public TagSet tags;
    public WorldState world;
    public PlayerState player;
    public MetaState meta;

    public EventContext(TurnType turnType, int day, TagSet tags, WorldState world, PlayerState player, MetaState meta) {
        this.turnType = turnType;
        this.day = day;
        this.tags = tags;
        this.world = world;
        this.player = player;
        this.meta = meta;
    }
}
