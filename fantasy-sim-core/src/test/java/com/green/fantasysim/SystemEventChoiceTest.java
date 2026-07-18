package com.green.fantasysim;

import com.green.fantasysim.domain.MetaState;
import com.green.fantasysim.domain.PlayerState;
import com.green.fantasysim.domain.TagSet;
import com.green.fantasysim.domain.WorldState;
import com.green.fantasysim.engine.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ensures SYS-* branching cards (job offers etc.) don't accidentally mutate numeric states via effects table.
 */
public class SystemEventChoiceTest {

    @Test
    void systemJobOffer_yes_changesJob_butDoesNotChangeWorldNumbers() {
        Config cfg = ConfigLoader.loadDefault();
        cfg.seed = 42;

        PlayerState p = SessionFactory.createStartingPlayer("human", "테스터", false);
        MetaState meta = SessionFactory.createMetaFromScratch(p);

        GameSession s = SessionFactory.newSession(cfg, p, meta);

        // Force a deterministic system job offer as a pending choice
        TagSet tags = new TagSet()
                .put("region", "central")
                .put("terrain", "plains")
                .put("nation", "empire")
                .put("race", "human")
                .put("faction", "empire")
                .put("threat", "politics")
                .put("theme", "order")
                .put("faithSide", "none")
                .put("politicalAxis", "empire_union_intrigue")
                .put("entryPolicy", "none")
                .put("deity", "none")
                .put("archdemon", "none")
                .put("demonTier", "none")
                .put("sin", "none")
                .put("site", "adventurer_guild")
                .put("difficulty", "50")
                .put("playerJob", "none")
                .put("playerRace", "human")
                .put("playerOrigin", "commoner");

        EventContext ctx = new EventContext(TurnType.CHOICE, 1, tags, s.world, s.player, s.meta);
        assertEquals("none", s.player.job);

        var card = SystemEventFactory.tryCreate(ctx);
        assertNotNull(card);
        assertTrue(card.id.startsWith("SYS-"));

        s.pending = new PendingChoice(TurnType.CHOICE, 1, tags, card);

        WorldState beforeW = new WorldState(s.world.empire, s.world.demon, s.world.cult, s.world.publicMood);
        int beforeHardship = s.meta.hardship;

        SessionEngine engine = new SessionEngine();
        TurnOutcome out = engine.choose(s, "YES");

        assertFalse(out.ended);
        assertEquals("adventurer", s.player.job, "YES should accept job offer");
        assertEquals(beforeW.empire, s.world.empire);
        assertEquals(beforeW.demon, s.world.demon);
        assertEquals(beforeW.cult, s.world.cult);
        assertEquals(beforeW.publicMood, s.world.publicMood);
        assertEquals(beforeHardship, s.meta.hardship, "system choice should not add hardship");
    }
}
