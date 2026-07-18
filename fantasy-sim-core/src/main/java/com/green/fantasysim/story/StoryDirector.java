package com.green.fantasysim.story;

import java.util.ArrayList;
import java.util.List;

/**
 * Auditable trace of the narrative director for the current scene.
 *
 * The director may rewrite prose and spotlight an existing unlocked card, but
 * it cannot create choices or mutate any rule-owned state.
 */
public class StoryDirector {
    public String source = "RULE_FALLBACK";
    public boolean liveAi;
    public int turn;
    public String role = "NARRATIVE_DIRECTOR";
    public String model = "";
    public String intent = "";
    public String spotlightChoiceId = "";
    public String spotlightReason = "";
    public List<String> recalledMemoryIds = new ArrayList<>();
    public List<String> constraints = new ArrayList<>();
    public String fallbackReason = "NOT_CONFIGURED";

    public StoryDirector() {}
}
