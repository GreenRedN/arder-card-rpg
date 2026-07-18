package com.green.fantasysim.engine;

import com.green.fantasysim.ai.AiParser;
import com.green.fantasysim.domain.CycleReport;
import com.green.fantasysim.domain.Effect;
import com.green.fantasysim.domain.EventCard;
import com.green.fantasysim.domain.PlayerState;
import com.green.fantasysim.domain.TagSet;
import com.green.fantasysim.domain.WorldState;

import java.util.Comparator;

public class SessionEngine {

    public TurnOutcome next(GameSession s) {
        TurnOutcome out = new TurnOutcome();

        // If waiting for a choice, repeat the pending card (idempotent)
        if (s.pending != null) {
            out.day = s.pending.day;
            out.turnType = s.pending.turnType;
            out.tags = s.pending.tags;
            out.needsChoice = true;
            out.eventCard = s.pending.card;
            snapshot(s, out);
            return out;
        }

        // Advance day
        s.day++;
        out.day = s.day;

        TurnType tt = pickTurnType(s);
        out.turnType = tt;

        if (tt == TurnType.SUMMARY) {
            String summary = s.ai.generateSummary(s.logger.lastNDays(s.cfg.summaryEveryDays), s.world, s.player, s.meta);
            s.logger.append("Day " + s.day + " [SUMMARY] " + summary);
            out.summaryText = summary;
            out.needsChoice = false;
            snapshot(s, out);

            String end = EndingJudge.endReasonIfAny(s.day, s.cfg.cycleLengthDays, s.world, s.player, s.meta);
            if (end != null) finish(s, out, end);
            return out;
        }

        TagSet tags = s.picker.pick(s.rand, s.world, s.player);
        out.tags = tags;

        EventContext ctx = new EventContext(tt, s.day, tags, s.world, s.player, s.meta);

        if (tt == TurnType.AUTO) {
            String line = s.ai.generateAutoLine(ctx);
            out.autoLine = line;

            int diff = safeDiff(tags);
            Effect e = s.resolver.resolve(tt, tags, s.world, s.player, null);
            s.applier.apply(s.world, s.player, s.meta, e, diff);

            out.appliedEffect = e;
            out.appliedEffectSummary = EffectApplier.summarize(e);

            s.logger.append("Day " + s.day + " [AUTO] " + line + " | " + out.appliedEffectSummary);
            snapshot(s, out);

            String end = EndingJudge.endReasonIfAny(s.day, s.cfg.cycleLengthDays, s.world, s.player, s.meta);
            if (end != null) finish(s, out, end);
            return out;
        }

        // CHOICE / MAJOR => generate event card and wait for input
        // 1) system events (job offers etc.) have priority over AI cache
        EventCard card = SystemEventFactory.tryCreate(ctx);
        if (card == null) {
            card = s.cache.nextOrCreate(ctx, s.ai);
        }
        AiParser.validateCard(card, tags); // safety; ensures tags subset + choice schema

        s.pending = new PendingChoice(tt, s.day, tags, card);

        out.needsChoice = true;
        out.eventCard = card;
        snapshot(s, out);
        return out;
    }

    public TurnOutcome choose(GameSession s, String choiceId) {
        TurnOutcome out = new TurnOutcome();

        if (s.pending == null) {
            out.ended = false;
            out.endReason = "선택 대기 상태가 아니다. 먼저 next를 호출해 이벤트를 받아야 한다.";
            snapshot(s, out);
            return out;
        }

        if (choiceId == null || choiceId.isBlank()) {
            out.endReason = "choiceId가 비어있다.";
            snapshot(s, out);
            return out;
        }

        PendingChoice p = s.pending;
        out.day = p.day;
        out.turnType = p.turnType;
        out.tags = p.tags;
        out.needsChoice = false;

        // Must be one of the pending card's offered choices (prevents arbitrary ids).
        if (p.card == null || p.card.choices == null || p.card.choices.stream().noneMatch(x -> choiceId.equals(x.id))) {
            out.endReason = "잘못된 choiceId. (이번 이벤트 카드에 없는 선택지)";
            snapshot(s, out);
            return out;
        }

        int diff = safeDiff(p.tags);
        Effect e;
        boolean systemCard = (p.card != null && p.card.id != null && p.card.id.startsWith("SYS-"));
        if (systemCard) {
            // System branching (job offers etc.) should not mutate numeric states via balance table.
            e = new Effect();
        } else {
            e = s.resolver.resolve(p.turnType, p.tags, s.world, s.player, choiceId);
        }
        s.applier.apply(s.world, s.player, s.meta, e, diff);

        // Apply system side-effects (e.g. job unlocked by YES)
        String sysSummary = SystemEventFactory.applyPostChoiceEffects(p, s.player, choiceId);

        out.appliedEffect = e;
        out.appliedEffectSummary = EffectApplier.summarize(e);
        if ((out.appliedEffectSummary == null || out.appliedEffectSummary.isBlank()) && sysSummary != null) {
            out.appliedEffectSummary = sysSummary;
        } else if (sysSummary != null) {
            out.appliedEffectSummary = out.appliedEffectSummary + " | " + sysSummary;
        }

        // log + impacts
        String title = (p.card != null && p.card.title != null) ? p.card.title : "(untitled)";
        s.logger.append("Day " + s.day + " [" + p.turnType + "] " + title + " => " + choiceId + " | " + out.appliedEffectSummary);

        int mag = magnitude(e);
        s.impacts.add(new ChoiceImpact(title, choiceId, out.appliedEffectSummary, mag));
        s.impacts.sort(Comparator.comparingInt((ChoiceImpact x) -> x.magnitude).reversed());
        if (s.impacts.size() > 12) s.impacts.subList(12, s.impacts.size()).clear();

        s.pending = null; // consumed

        snapshot(s, out);

        String end = EndingJudge.endReasonIfAny(s.day, s.cfg.cycleLengthDays, s.world, s.player, s.meta);
        if (end != null) finish(s, out, end);

        return out;
    }

    private static int safeDiff(TagSet tags) {
        try { return Integer.parseInt(tags.get("difficulty")); }
        catch (Exception e) { return 50; }
    }

    private TurnType pickTurnType(GameSession s) {
        // summary rhythm has priority (but not on day 0)
        if (s.cfg.summaryEveryDays > 0 && s.day > 0 && s.day % s.cfg.summaryEveryDays == 0) return TurnType.SUMMARY;

        boolean majorByThreshold =
                (s.world.demon >= s.cfg.majorThresholds.demonGte) ||
                (s.world.cult <= s.cfg.majorThresholds.cultLte) ||
                (s.world.publicMood <= s.cfg.majorThresholds.publicMoodLte) ||
                (s.world.empire <= s.cfg.majorThresholds.empireLte);

        // when thresholds are screaming, bump major chance
        double majorChance = s.cfg.majorChance + (majorByThreshold ? 0.18 : 0.0);

        double x = s.rand.nextDouble();
        double a = clamp01(s.cfg.autoChance);
        double c = clamp01(s.cfg.choiceChance);
        double m = clamp01(majorChance);

        // normalize (in case config doesn't sum to 1)
        double sum = a + c + m;
        if (sum <= 0) { a = 0.5; c = 0.4; m = 0.1; sum = 1.0; }
        a /= sum; c /= sum; m /= sum;

        if (x < m) return TurnType.MAJOR;
        if (x < m + c) return TurnType.CHOICE;
        return TurnType.AUTO;
    }

    private static double clamp01(double x) { return Math.max(0.0, Math.min(1.0, x)); }

    private static int magnitude(Effect e) {
        int m = 0;
        m += Math.abs(e.dEmpire) + Math.abs(e.dDemon) + Math.abs(e.dCult) + Math.abs(e.dPublicMood);
        m += Math.abs(e.dHp);
        m += Math.abs(e.dPower) * 2;
        m += Math.abs(e.dGold) / 5;
        m += Math.abs(e.dMetaPower);
        return m;
    }

    private static void snapshot(GameSession s, TurnOutcome out) {
        out.world = new WorldState(s.world.empire, s.world.demon, s.world.cult, s.world.publicMood);
        out.player = s.player.shallowCopy();
        out.meta = new com.green.fantasysim.domain.MetaState();
        out.meta.hardship = s.meta.hardship;
        out.meta.insightEmpire = s.meta.insightEmpire;
        out.meta.insightDemon = s.meta.insightDemon;
        out.meta.insightCult = s.meta.insightCult;
        out.meta.metaPower = s.meta.metaPower;
    }

    private static void finish(GameSession s, TurnOutcome out, String endReason) {
        out.ended = true;
        out.endReason = endReason;

        s.ended = true;
        s.endReason = endReason;

        CycleReport report = ReportBuilder.build(s, endReason);
        // final cycle summary (AI)
        String cycleSummary = s.ai.generateCycleSummary(s.logger.lastNDays(Math.max(7, s.cfg.cycleLengthDays)), report);
        report.summaryText = cycleSummary;

        out.cycleReport = report;
    }
}
