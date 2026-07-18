package com.green.fantasysim.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameLogger {
    private final List<String> lines = new ArrayList<>();
    public void append(String line) { lines.add(line); }
    public List<String> lastNDays(int n) {
        if (lines.isEmpty()) return Collections.emptyList();
        int from = Math.max(0, lines.size() - n);
        return lines.subList(from, lines.size());
    }
}
