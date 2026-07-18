package com.green.fantasysim.engine;

import com.green.fantasysim.domain.MetaState;
import com.green.fantasysim.domain.PlayerState;
import com.green.fantasysim.domain.WorldState;

public class EndingJudge {

    public static boolean isWorldCollapse(WorldState w) {
        return w.demon >= 95 && w.cult <= 10;
    }

    public static String endReasonIfAny(int day, int cycleLength, WorldState w, PlayerState p, MetaState meta) {
        if (p.isDead()) return "사망(hp<=0)";
        if (isWorldCollapse(w)) return "세계 붕괴(demon>=95 & cult<=10)";

        // Transcend ending: gated by meta and good direction
        boolean good = "GOOD".equals(p.alignmentLead());
        boolean canTranscend = good && meta.metaPower >= 80 && (meta.insightDemon >= 60 || meta.insightEmpire >= 60 || meta.insightCult >= 60);
        if (canTranscend && p.power >= 105 && w.demon <= 40) return "초월(정화 엔딩)";

        // Dark ending
        if ("EVIL".equals(p.alignmentLead()) && w.demon >= 90) return "마왕의 종자(타락 엔딩)";

        // A non-positive cycle length means an open-ended campaign. The original
        // 30-day expiry was a prototype constraint, not a world rule.
        if (cycleLength > 0 && day >= cycleLength) return "기간 만료(day==cycleLength)";
        return null;
    }

    public static String endingDirection(PlayerState p) { return p.alignmentLead(); }
}
