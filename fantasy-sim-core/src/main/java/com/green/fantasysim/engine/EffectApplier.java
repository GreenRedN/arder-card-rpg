package com.green.fantasysim.engine;

import com.green.fantasysim.domain.Effect;
import com.green.fantasysim.domain.MetaState;
import com.green.fantasysim.domain.PlayerState;
import com.green.fantasysim.domain.WorldState;
import com.green.fantasysim.util.Clamp;

public class EffectApplier {

    public void apply(WorldState world, PlayerState player, MetaState meta, Effect e, int difficulty) {
        world.apply(e);
        player.apply(e);
        meta.apply(e);

        // implicit meta gain: surviving hardship turns into metaPower slowly
        if (e.dHp < 0 && player.hp > 0) {
            int extraHard = Math.max(0, (-e.dHp) / 4);
            meta.hardship = Clamp.between(meta.hardship + extraHard, 0, 100);
            if (extraHard > 0 && difficulty >= 60) {
                meta.metaPower = Clamp.between(meta.metaPower + 1, 0, 100);
            }
        }

        // gate power
        int cap = TierProgression.allowedPowerCap(player, meta);
        player.power = Math.min(player.power, cap);

        // keep power in common band lower bound
        player.power = Math.max(11, player.power);

        // sync tier
        TierProgression.syncTier(player);
    }

    public static String summarize(Effect e) {
        StringBuilder sb = new StringBuilder();
        if (e.dEmpire != 0) sb.append("제국 ").append(delta(e.dEmpire)).append(" ");
        if (e.dDemon != 0) sb.append("마족 ").append(delta(e.dDemon)).append(" ");
        if (e.dCult != 0) sb.append("교단 ").append(delta(e.dCult)).append(" ");
        if (e.dPublicMood != 0) sb.append("민심 ").append(delta(e.dPublicMood)).append(" ");
        if (e.dHp != 0) sb.append("HP ").append(delta(e.dHp)).append(" ");
        if (e.dGold != 0) sb.append("G ").append(delta(e.dGold)).append(" ");
        if (e.dPower != 0) sb.append("강함 ").append(delta(e.dPower)).append(" ");
        if (e.dMetaPower != 0) sb.append("메타힘 ").append(delta(e.dMetaPower)).append(" ");
        if (e.dInsightEmpire != 0) sb.append("통찰(제국) ").append(delta(e.dInsightEmpire)).append(" ");
        if (e.dInsightDemon != 0) sb.append("통찰(마족) ").append(delta(e.dInsightDemon)).append(" ");
        if (e.dInsightCult != 0) sb.append("통찰(교단) ").append(delta(e.dInsightCult)).append(" ");
        return sb.toString().trim();
    }

    private static String delta(int x) { return (x > 0 ? "+"+x : String.valueOf(x)); }
}
