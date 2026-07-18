package com.green.fantasysim.story;

public class StoryWorld {
    public String continent;
    public String locationId;
    public String locationName;
    public String weather;
    public String chapterTitle;
    public String objective;
    public long worldMinutes;
    public int empireStability;
    public int demonInfluence;
    public int publicMood;

    public StoryWorld() {}

    public String timeLabel() {
        long safeMinutes = Math.max(0, worldMinutes);
        long day = safeMinutes / 1440L + 1L;
        long minuteOfDay = safeMinutes % 1440L;
        long hour = minuteOfDay / 60L;
        long minute = minuteOfDay % 60L;
        return "제" + day + "일 " + String.format("%02d:%02d", hour, minute);
    }
}
