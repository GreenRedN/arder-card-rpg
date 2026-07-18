package com.green.fantasysim.api.service;

import com.green.fantasysim.api.error.SessionNotFoundException;
import com.green.fantasysim.engine.*;
import com.green.fantasysim.domain.MetaState;
import com.green.fantasysim.domain.PlayerState;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final SessionEngine engine = new SessionEngine();
    private final Config baseCfg = ConfigLoader.loadDefault();

    public String createSession(PlayerState player, MetaState meta, Long seedOpt) {
        Config cfg = cloneConfig(baseCfg);
        long seed = (seedOpt != null) ? seedOpt : (baseCfg.seed + Math.abs(UUID.randomUUID().getMostSignificantBits()));
        cfg.seed = seed;

        GameSession s = SessionFactory.newSession(cfg, player, meta);
        String id = UUID.randomUUID().toString();
        sessions.put(id, s);
        return id;
    }

    public GameSession get(String id) {
        GameSession s = sessions.get(id);
        if (s == null) throw new SessionNotFoundException(id);
        return s;
    }

    public TurnOutcome next(String id) {
        return engine.next(get(id));
    }

    public TurnOutcome choose(String id, String choiceId) {
        return engine.choose(get(id), choiceId);
    }

    public String rebirth(String id) {
        GameSession ended = get(id);
        if (!ended.ended) throw new IllegalStateException("아직 사이클이 종료되지 않았다. 먼저 next/choose로 엔딩을 발생시켜야 한다.");
        GameSession s2 = SessionFactory.rebirth(ended);
        String id2 = UUID.randomUUID().toString();
        sessions.put(id2, s2);
        return id2;
    }

    private static Config cloneConfig(Config c) {
        Config x = new Config();
        x.cycleLengthDays = c.cycleLengthDays;
        x.summaryEveryDays = c.summaryEveryDays;
        x.choiceChance = c.choiceChance;
        x.majorChance = c.majorChance;
        x.autoChance = c.autoChance;
        x.seed = c.seed;
        x.aiCacheBatchSize = c.aiCacheBatchSize;
        x.majorThresholds.demonGte = c.majorThresholds.demonGte;
        x.majorThresholds.cultLte = c.majorThresholds.cultLte;
        x.majorThresholds.publicMoodLte = c.majorThresholds.publicMoodLte;
        x.majorThresholds.empireLte = c.majorThresholds.empireLte;
        x.startingWorld.empire = c.startingWorld.empire;
        x.startingWorld.demon = c.startingWorld.demon;
        x.startingWorld.cult = c.startingWorld.cult;
        x.startingWorld.publicMood = c.startingWorld.publicMood;
        return x;
    }
}
