package com.green.fantasysim.story;

import java.util.ArrayList;
import java.util.List;

public class StoryChoice {
    public String id;
    public String text;
    public String playerLine;
    public String category;
    public String risk;
    public boolean locked;
    public String lockedReason;

    // Server-owned resolution data. API DTOs deliberately do not expose these fields.
    public String nextSceneId;
    public int timeMinutes;
    public StoryEffect effect = new StoryEffect();
    public String memoryType;
    public String memoryTitle;
    public String memorySummary;
    public int memoryImportance;
    public List<String> memoryTags = new ArrayList<>();

    public StoryChoice() {}
}
