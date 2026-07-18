package com.green.fantasysim.domain;

import java.util.ArrayList;
import java.util.List;

public class EventCard {
    public String id;
    public String title;
    public String situation;
    public List<Choice> choices = new ArrayList<>();
    public List<String> tags = new ArrayList<>();
    public EventCard() {}
}
