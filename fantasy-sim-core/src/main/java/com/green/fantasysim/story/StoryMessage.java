package com.green.fantasysim.story;

public class StoryMessage {
    public String id;
    public int turn;
    public String role;
    public String speakerId;
    public String speakerName;
    public String mood;
    public String text;

    public StoryMessage() {}

    public StoryMessage(String id, int turn, String role, String speakerId, String speakerName, String mood, String text) {
        this.id = id;
        this.turn = turn;
        this.role = role;
        this.speakerId = speakerId;
        this.speakerName = speakerName;
        this.mood = mood;
        this.text = text;
    }
}
