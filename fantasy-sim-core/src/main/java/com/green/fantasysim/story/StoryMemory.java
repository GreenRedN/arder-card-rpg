package com.green.fantasysim.story;

import java.util.ArrayList;
import java.util.List;

public class StoryMemory {
    public String id;
    public int turn;
    public String type;
    public String subjectId;
    public String title;
    public String summary;
    public int importance;
    public boolean active;
    public List<String> tags = new ArrayList<>();

    public StoryMemory() {}
}
