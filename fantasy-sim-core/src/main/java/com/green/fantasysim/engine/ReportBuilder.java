package com.green.fantasysim.engine;

import com.green.fantasysim.domain.CycleReport;
import com.green.fantasysim.domain.PlayerState;
import com.green.fantasysim.domain.WorldState;

import java.util.List;

public class ReportBuilder {

    public static CycleReport build(GameSession s, String endReason) {
        CycleReport r = new CycleReport();
        r.dayStart = s.dayStart;
        r.dayEnd = s.day;
        r.endReason = endReason;

        r.worldStart = new WorldState(s.worldStart.empire, s.worldStart.demon, s.worldStart.cult, s.worldStart.publicMood);
        r.worldEnd = new WorldState(s.world.empire, s.world.demon, s.world.cult, s.world.publicMood);

        r.playerStart = s.playerStart.shallowCopy();
        r.playerEnd = s.player.shallowCopy();

        // top 3 impacts (not only MAJOR)
        int n = Math.min(3, s.impacts.size());
        for (int i=0;i<n;i++) {
            ChoiceImpact ci = s.impacts.get(i);
            r.majorChoices.add(ci.title + " => " + ci.choiceId + " (" + ci.effectSummary + ")");
        }

        r.endingDirection = EndingJudge.endingDirection(s.player);
        return r;
    }

    public static String formatForCli(CycleReport r) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n================= CYCLE REPORT =================\n");
        sb.append("기간: Day ").append(r.dayStart).append(" ~ Day ").append(r.dayEnd).append("\n");
        sb.append("종료 원인: ").append(r.endReason).append("\n\n");

        sb.append("[WorldState]\n");
        sb.append(deltaLine("제국", r.worldStart.empire, r.worldEnd.empire));
        sb.append(deltaLine("마족", r.worldStart.demon, r.worldEnd.demon));
        sb.append(deltaLine("교단", r.worldStart.cult, r.worldEnd.cult));
        sb.append(deltaLine("민심", r.worldStart.publicMood, r.worldEnd.publicMood));

        sb.append("\n[PlayerState]\n");
        sb.append("직업/티어: ").append(r.playerStart.job).append("/").append(r.playerStart.tier)
                .append(" -> ").append(r.playerEnd.job).append("/").append(r.playerEnd.tier).append("\n");
        sb.append("종족/출신: ").append(r.playerEnd.race).append("/").append(r.playerEnd.origin).append("\n");
        sb.append(deltaLine("HP", r.playerStart.hp, r.playerEnd.hp));
        sb.append(deltaLine("Gold", r.playerStart.gold, r.playerEnd.gold));
        sb.append(deltaLine("강함", r.playerStart.power, r.playerEnd.power));

        sb.append("\n[주요 선택 3개]\n");
        if (r.majorChoices.isEmpty()) sb.append("- (기록 없음)\n");
        else for (String s : r.majorChoices) sb.append("- ").append(s).append("\n");

        sb.append("\n[정렬/엔딩 방향]\n");
        sb.append("GOOD=").append(r.playerEnd.goodScore).append(", NEUTRAL=").append(r.playerEnd.neutralScore).append(", EVIL=").append(r.playerEnd.evilScore)
                .append(" => ").append(r.endingDirection).append("\n");

        if (r.summaryText != null && !r.summaryText.isBlank()) {
            sb.append("\n[Summary]\n").append(r.summaryText).append("\n");
        }
        sb.append("================================================\n");
        return sb.toString();
    }

    private static String deltaLine(String name, int a, int b) {
        int d = b - a;
        return name + ": " + a + " -> " + b + " (" + (d>=0?"+":"") + d + ")\n";
    }
}
