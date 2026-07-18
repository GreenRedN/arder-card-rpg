package com.green.fantasysim.engine;

import com.green.fantasysim.domain.Choice;
import com.green.fantasysim.domain.EventCard;
import com.green.fantasysim.domain.PlayerState;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates deterministic "system" events that should override normal AI event generation.
 *
 * Currently used for job-route onboarding:
 * - Start as a commoner with job=none.
 * - When passing key sites, the system asks YES/NO.
 * - If YES, job becomes the relevant route starter.
 */
public final class SystemEventFactory {
    private SystemEventFactory() {}

    // Card id prefixes (used to apply post-choice effects)
    private static final String JOB_OFFER = "SYS-JOB-OFFER:";        // JOB_OFFER + job
    private static final String PALADIN_OFFER = "SYS-PALADIN-OFFER"; // priest -> paladin

    public static EventCard tryCreate(EventContext ctx) {
        if (ctx == null || ctx.tags == null || ctx.player == null) return null;
        String site = ctx.tags.get("site");
        if (site == null || site.isBlank()) return null;

        PlayerState p = ctx.player;

        // 1) Job onboarding from "none"
        if ("none".equals(p.job)) {
            return switch (site) {
                case "adventurer_guild" -> jobOffer(ctx, "adventurer",
                        "모험가 길드 앞을 지나가다",
                        "낡은 간판 아래에서 견습 모집 공지가 바람에 흔들린다. 길드원 하나가 다가와 묻는다. \"모험가를 하시겠습니까?\"");
                case "knight_order" -> jobOffer(ctx, "knight",
                        "기사단 훈련장을 스치다",
                        "검 소리와 함성이 울리는 훈련장 옆. 부단장이 다가와 묻는다. \"기사로 들어가시겠습니까?\"");
                case "mage_tower" -> jobOffer(ctx, "mage",
                        "마탑의 그림자 아래서",
                        "푸른 룬이 새겨진 문이 열리고, 견습관이 고개를 내민다. \"마법사가 되시겠습니까?\"");
                case "cathedral" -> jobOffer(ctx, "priest",
                        "대성당의 종소리",
                        "종소리가 마음을 붙잡는다. 성직자가 조용히 다가와 묻는다. \"성직자가 되시겠습니까?\"");
                default -> null;
            };
        }

        // 2) Priest -> Paladin (church job extension)
        if ("priest".equals(p.job) && "knight_order".equals(site)) {
            return paladinOffer(ctx);
        }

        return null;
    }

    /**
     * Applies system side-effects after a choice has been selected.
     * @return a short summary line (or null if nothing changed)
     */
    public static String applyPostChoiceEffects(PendingChoice pending, PlayerState player, String choiceId) {
        if (pending == null || pending.card == null || pending.card.id == null || player == null) return null;
        if (!"YES".equals(choiceId)) return null;

        String id = pending.card.id;

        if (id.startsWith(JOB_OFFER)) {
            String job = id.substring(JOB_OFFER.length());
            int metadataSeparator = job.indexOf(':');
            if (metadataSeparator >= 0) job = job.substring(0, metadataSeparator);
            acceptJob(player, job);
            return "직업 결정: " + job + " (" + player.tier + ")";
        }

        if (id.startsWith(PALADIN_OFFER)) {
            acceptJob(player, "paladin");
            return "직업 전환: paladin (" + player.tier + ")";
        }

        return null;
    }

    private static EventCard jobOffer(EventContext ctx, String targetJob, String title, String situation) {
        EventCard c = base(ctx, JOB_OFFER + targetJob, title, situation);
        c.choices = List.of(
                new Choice("YES", "예"),
                new Choice("NO", "아니오")
        );
        return c;
    }

    private static EventCard paladinOffer(EventContext ctx) {
        EventCard c = base(ctx, PALADIN_OFFER,
                "성기사 서약의 기로",
                "기사단의 예배실. 신관인 당신에게 제의가 들어온다. \"성기사로 서약하시겠습니까?\"");
        c.choices = List.of(
                new Choice("YES", "예"),
                new Choice("NO", "아니오")
        );
        return c;
    }

    private static EventCard base(EventContext ctx, String id, String title, String situation) {
        EventCard c = new EventCard();
        c.id = id + ":D" + ctx.day;
        c.title = title;
        c.situation = situation;

        // Ensure all tags match context (AiParser subset enforcement).
        c.tags = new ArrayList<>();
        for (var e : ctx.tags.entries()) {
            c.tags.add(e.getKey() + ":" + e.getValue());
        }
        return c;
    }

    private static void acceptJob(PlayerState p, String job) {
        if (p == null || job == null || job.isBlank()) return;

        p.job = job;
        // When entering a job route, tier is derived from power.
        if ("priest".equals(job) || "paladin".equals(job)) {
            // Joining church implies church faith, unless explicitly demon side.
            if (p.faith == null || "none".equals(p.faith)) p.faith = "church";
        }
        TierProgression.syncTier(p);
    }
}
