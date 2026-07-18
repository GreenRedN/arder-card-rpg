package com.green.fantasysim.story;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoryEngineTest {
    private final StoryEngine engine = new StoryEngine();

    @Test
    void createsOpenEndedCampaignWithRaceSpecificCard() {
        StoryCampaign campaign = engine.create("test-id", "그린", "elf", 42L);

        assertEquals("ACTIVE", campaign.status);
        assertEquals("아르데르 대륙", campaign.world.continent);
        assertEquals("arrival", campaign.scene.id);

        engine.choose(campaign, "arrival_help", "request-1");
        assertEquals("wagon", campaign.scene.id);
        assertTrue(campaign.scene.choices.stream().anyMatch(c -> c.text.contains("[엘프]")));
        assertTrue(campaign.relationships.get("sera").trust > 5);
    }

    @Test
    void duplicateRequestDoesNotApplyEffectsTwice() {
        StoryCampaign campaign = engine.create("test-id", "그린", "human", 42L);
        engine.choose(campaign, "arrival_price", "same-request");
        int gold = campaign.player.gold;
        int turn = campaign.turn;

        engine.choose(campaign, "anything", "same-request");

        assertEquals(gold, campaign.player.gold);
        assertEquals(turn, campaign.turn);
    }

    @Test
    void importantChoiceBecomesStructuredMemory() {
        StoryCampaign campaign = engine.create("test-id", "그린", "human", 42L);
        engine.choose(campaign, "arrival_help", "request-1");

        assertFalse(campaign.memories.isEmpty());
        StoryMemory memory = campaign.memories.get(0);
        assertEquals("PROMISE", memory.type);
        assertTrue(memory.tags.contains("sera"));
        assertTrue(memory.importance >= 80);
    }

    @Test
    void campaignContinuesPastThirtyDaysAndReachesTheOpenHub() {
        StoryCampaign campaign = engine.create("long-id", "그린", "human", 42L);
        engine.choose(campaign, "arrival_help", "main-1");
        engine.choose(campaign, "wagon_seal", "main-2");
        engine.choose(campaign, "seal_give", "main-3");
        engine.choose(campaign, "ambush_guard", "main-4");
        engine.choose(campaign, "chapel_rescue", "main-5");
        engine.choose(campaign, "oath_companion", "main-6");

        assertEquals("hub", campaign.scene.id);
        assertEquals(2, campaign.chapter);
        assertEquals("모험가", campaign.player.jobLabel);

        for (int i = 0; i < 10; i++) {
            engine.choose(campaign, "hub_west", "travel-" + i);
            engine.choose(campaign, "west_pressure", "return-" + i);
        }

        assertTrue(campaign.world.worldMinutes > 30L * 24 * 60);
        assertEquals("ACTIVE", campaign.status);
        assertEquals("hub", campaign.scene.id);
        assertTrue(campaign.memories.stream().anyMatch(m -> "잿빛 서약".equals(m.title)));
    }
}
