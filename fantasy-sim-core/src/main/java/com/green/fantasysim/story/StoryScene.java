package com.green.fantasysim.story;

import java.util.ArrayList;
import java.util.List;

public class StoryScene {
    public String id;
    public String title;
    public String locationId;
    public String locationName;
    public String objective;
    public String speakerId;
    public String speakerName;
    public String speakerRole;
    public String mood;
    public String narration;
    public String dialogue;
    public List<StoryChoice> choices = new ArrayList<>();

    public StoryScene() {}
}
