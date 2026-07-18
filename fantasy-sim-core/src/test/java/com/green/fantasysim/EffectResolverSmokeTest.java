package com.green.fantasysim;

import com.green.fantasysim.domain.PlayerState;
import com.green.fantasysim.domain.TagSet;
import com.green.fantasysim.domain.WorldState;
import com.green.fantasysim.engine.EffectResolver;
import com.green.fantasysim.engine.TurnType;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class EffectResolverSmokeTest {

    @Test
    void resolve_returnsEffectAndClamps() {
        InputStream in = EffectResolverSmokeTest.class.getResourceAsStream("/rules/effects.json");
        assertNotNull(in, "effects.json must be on classpath");

        EffectResolver r = new EffectResolver(in);

        TagSet tags = new TagSet()
                .put("region", "north")
                .put("threat", "monster")
                .put("theme", "survival")
                .put("difficulty", "80");

        WorldState w = new WorldState();
        w.empire = 40; w.demon = 60; w.cult = 30; w.publicMood = 50;

        PlayerState p = new PlayerState();
        p.race = "human"; p.job = "adventurer"; p.tier = "veteran"; p.origin = "commoner";
        p.hp = 70; p.gold = 10; p.power = 55;

        var e = r.resolve(TurnType.CHOICE, tags, w, p, "GOOD");
        assertNotNull(e);
        // safety clamp in resolver should keep deltas in a reasonable range
        assertTrue(Math.abs(e.dHp) <= 25);
        assertTrue(Math.abs(e.dGold) <= 25);
    }
}
