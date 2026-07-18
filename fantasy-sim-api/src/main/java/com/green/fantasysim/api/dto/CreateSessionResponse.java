package com.green.fantasysim.api.dto;

import com.green.fantasysim.domain.MetaState;
import com.green.fantasysim.domain.PlayerState;
import com.green.fantasysim.domain.WorldState;

public class CreateSessionResponse {
    public String sessionId;
    public WorldState world;
    public PlayerState player;
    public MetaState meta;

    public CreateSessionResponse() {}
    public CreateSessionResponse(String sessionId, WorldState world, PlayerState player, MetaState meta) {
        this.sessionId = sessionId;
        this.world = world;
        this.player = player;
        this.meta = meta;
    }
}
