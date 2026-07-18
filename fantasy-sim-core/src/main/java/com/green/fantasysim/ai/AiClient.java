package com.green.fantasysim.ai;

import com.green.fantasysim.domain.CycleReport;
import com.green.fantasysim.engine.EventContext;
import com.green.fantasysim.domain.MetaState;
import com.green.fantasysim.domain.PlayerState;
import com.green.fantasysim.domain.WorldState;

import java.util.List;

public interface AiClient {
    String generateEventCardJson(EventContext ctx);
    String generateAutoLine(EventContext ctx);

    String generateSummary(List<String> recentLogs, WorldState world, PlayerState player, MetaState meta);
    String generateCycleSummary(List<String> recentLogs, CycleReport report);
}
