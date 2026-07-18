package com.green.fantasysim.story;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializable source of truth for one open-ended story campaign.
 * AI prose is stored as messages, while rules and memories remain structured.
 */
public class StoryCampaign {
    public String id;
    public long version;
    public long seed;
    public int turn;
    public int chapter;
    public String status;
    public String scenarioId;
    public String createdAt;
    public String updatedAt;

    public StoryPlayer player;
    public StoryWorld world;
    public Map<String, StoryRelationship> relationships = new LinkedHashMap<>();
    public Map<String, String> flags = new LinkedHashMap<>();
    public List<StoryMemory> memories = new ArrayList<>();
    public List<StoryMessage> messages = new ArrayList<>();
    public List<String> processedRequestIds = new ArrayList<>();
    public List<String> eventHistory = new ArrayList<>();
    public StoryScene scene;
    public StoryDirector director = new StoryDirector();
    public String recap;

    public StoryCampaign() {}
}
