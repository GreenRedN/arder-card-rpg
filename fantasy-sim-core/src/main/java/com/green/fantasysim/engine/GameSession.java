package com.green.fantasysim.engine;

import com.green.fantasysim.ai.AiClient;
import com.green.fantasysim.ai.EventCache;
import com.green.fantasysim.domain.MetaState;
import com.green.fantasysim.domain.PlayerState;
import com.green.fantasysim.domain.WorldState;
import com.green.fantasysim.io.GameLogger;
import com.green.fantasysim.util.Rand;

import java.util.ArrayList;
import java.util.List;

public class GameSession {
    public final Config cfg;
    public final Rand rand;

    public final EventPicker picker;
    public final EffectResolver resolver;
    public final EffectApplier applier;
    public final AiClient ai;
    public final EventCache cache;
    public final GameLogger logger;

    public int day; // 0..cycleLengthDays
    public int cycleNo;

    public WorldState world;
    public PlayerState player;
    public MetaState meta;

    // cycle start snapshots
    public WorldState worldStart;
    public PlayerState playerStart;
    public int dayStart;

    // record impactful choices (title + choice + effect)
    public final List<ChoiceImpact> impacts = new ArrayList<>();

    // pending choice
    public PendingChoice pending;

    // session end state (for rebirth gating)
    public boolean ended;
    public String endReason;

    public GameSession(Config cfg, Rand rand, EventPicker picker, EffectResolver resolver, EffectApplier applier,
                       AiClient ai, EventCache cache, GameLogger logger,
                       WorldState world, PlayerState player, MetaState meta) {
        this.cfg = cfg; this.rand = rand;
        this.picker = picker; this.resolver = resolver; this.applier = applier;
        this.ai = ai; this.cache = cache; this.logger = logger;
        this.world = world; this.player = player; this.meta = meta;
        this.day = 0;
        this.cycleNo = 1;
        this.ended = false;
        this.endReason = null;
        beginCycleSnapshots();
    }

    public void beginCycleSnapshots() {
        this.dayStart = Math.max(1, day+1);
        this.worldStart = new WorldState(world.empire, world.demon, world.cult, world.publicMood);
        this.playerStart = player.shallowCopy();
    }
}
