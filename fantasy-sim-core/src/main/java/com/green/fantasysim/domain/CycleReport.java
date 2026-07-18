package com.green.fantasysim.domain;

import java.util.ArrayList;
import java.util.List;

public class CycleReport {
    public int dayStart;
    public int dayEnd;
    public String endReason;

    public WorldState worldStart;
    public WorldState worldEnd;

    public PlayerState playerStart;
    public PlayerState playerEnd;

    public List<String> majorChoices = new ArrayList<>();
    public String endingDirection;
    public String summaryText;
}
