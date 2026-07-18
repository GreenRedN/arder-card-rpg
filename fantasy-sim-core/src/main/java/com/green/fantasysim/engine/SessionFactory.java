package com.green.fantasysim.engine;

import com.green.fantasysim.ai.AiClient;
import com.green.fantasysim.ai.DummyAiClient;
import com.green.fantasysim.ai.EventCache;
import com.green.fantasysim.domain.MetaState;
import com.green.fantasysim.domain.PlayerState;
import com.green.fantasysim.domain.WorldState;
import com.green.fantasysim.io.GameLogger;
import com.green.fantasysim.util.NameGenerator;
import com.green.fantasysim.util.Rand;

import java.io.InputStream;
import java.util.Objects;

public final class SessionFactory {
    private SessionFactory(){}

    private static Config cloneConfig(Config c) {
        Config x = new Config();
        x.cycleLengthDays = c.cycleLengthDays;
        x.summaryEveryDays = c.summaryEveryDays;
        x.choiceChance = c.choiceChance;
        x.majorChance = c.majorChance;
        x.autoChance = c.autoChance;
        x.seed = c.seed;
        x.aiCacheBatchSize = c.aiCacheBatchSize;
        x.majorThresholds.demonGte = c.majorThresholds.demonGte;
        x.majorThresholds.cultLte = c.majorThresholds.cultLte;
        x.majorThresholds.publicMoodLte = c.majorThresholds.publicMoodLte;
        x.majorThresholds.empireLte = c.majorThresholds.empireLte;
        x.startingWorld.empire = c.startingWorld.empire;
        x.startingWorld.demon = c.startingWorld.demon;
        x.startingWorld.cult = c.startingWorld.cult;
        x.startingWorld.publicMood = c.startingWorld.publicMood;
        return x;
    }


    public static GameSession newSession(Config cfg, PlayerState player, MetaState meta) {
        Objects.requireNonNull(cfg);
        Objects.requireNonNull(player);
        Objects.requireNonNull(meta);

        Rand rand = new Rand(cfg.seed);

        // resolver loads rules from core resources
        InputStream in = SessionFactory.class.getResourceAsStream("/rules/effects.json");
        if (in == null) throw new IllegalStateException("rules/effects.json not found");
        EffectResolver resolver = new EffectResolver(in);

        EffectApplier applier = new EffectApplier();
        EventPicker picker = new EventPicker();

        AiClient ai = new DummyAiClient(); // swap with real LLM client later
        EventCache cache = new EventCache(cfg.aiCacheBatchSize);
        GameLogger logger = new GameLogger();

        // base world
        WorldState w = new WorldState(cfg.startingWorld.empire, cfg.startingWorld.demon, cfg.startingWorld.cult, cfg.startingWorld.publicMood);

        // sync tier
        TierProgression.syncTier(player);

        return new GameSession(cfg, rand, picker, resolver, applier, ai, cache, logger, w, player, meta);
    }

    public static PlayerState createPlayer(String race, String job, String origin, String originDetail, String faith) {
        if (!TagOntology.RACES.contains(race)) throw new IllegalArgumentException("invalid race");
        if (!TagOntology.JOBS.contains(job)) throw new IllegalArgumentException("invalid job");
        if (!TagOntology.ORIGINS.contains(origin)) throw new IllegalArgumentException("invalid origin");

        PlayerState p = new PlayerState();
        p.name = "";
        p.race = race;
        p.job = job;
        p.origin = origin;
        p.originDetail = (originDetail == null ? "" : originDetail);
        String faithNorm = (faith == null || faith.isBlank()) ? "none" : faith;
        if (!TagOntology.FAITHS.contains(faithNorm)) throw new IllegalArgumentException("invalid faith");
        p.faith = faithNorm;

        // base stats
        p.hp = 100;
        p.gold = switch (origin) {
            case "noble" -> 120;
            case "royal" -> 200;
            case "orphan_cult" -> 10;
            case "nameless" -> 0;
            default -> 60;
        };
        p.power = switch (origin) {
            case "royal" -> 22;
            case "noble" -> 20;
            case "orphan_cult" -> 18;
            default -> 18;
        };
        if ("nameless".equals(origin)) p.power = 19;

        // race modifiers
        if ("dwarf".equals(race)) p.gold += 30;
        if ("elf".equals(race)) p.power += 1;
        if ("beast".equals(race)) p.hp = 100; // already capped

        // job modifiers
        if ("knight".equals(job)) p.power += 2;
        if ("mage".equals(job)) p.power += 2;
        if ("priest".equals(job)) p.power += 1;
        if ("paladin".equals(job)) p.power += 2;

        // init alignment
        p.goodScore = 0; p.neutralScore = 0; p.evilScore = 0;

        TierProgression.syncTier(p);
        return p;
    }

    /**
     * New canonical start flow:
     * - user selects only race (+ optional "nameless")
     * - user sets name (unless nameless)
     * - always starts as a commoner with job=none
     */
    public static PlayerState createStartingPlayer(String race, String name, boolean nameless) {
        if (!TagOntology.RACES.contains(race)) throw new IllegalArgumentException("invalid race");

        if (nameless) {
            PlayerState p = createPlayer(race, "none", "nameless", "", "none");
            p.name = "이름 없는 자";
            return p;
        }

        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        PlayerState p = createPlayer(race, "none", "commoner", "", "none");
        p.name = name.trim();
        return p;
    }

    public static MetaState createMetaFromScratch(PlayerState p) {
        MetaState m = new MetaState();
        if ("nameless".equals(p.origin)) {
            // nameless starts with slight meta edge (but loses rebirth option later)
            m.insightDemon = 5;
            m.metaPower = 5;
        }
        return m;
    }

    public static GameSession rebirth(GameSession ended) {
        if (ended == null) throw new IllegalArgumentException("session null");
        if (!ended.ended) throw new IllegalStateException("아직 사이클이 종료되지 않았다. (ended=false)");
        if ("nameless".equals(ended.player.origin)) throw new IllegalStateException("무명의 자는 환생 불가");

        // keep meta, reset world + player baseline with slight meta carry
        MetaState meta = ended.meta;
        PlayerState old = ended.player;

        // Random reincarnation: job resets to none, origin rolls, name becomes random.
        Rand rebirthRand = new Rand(ended.cfg.seed + ended.cycleNo * 997L + 31L);
        PlayerState p = createReincarnatedPlayer(rebirthRand, old.race);

        // small meta carry into baseline power (capped)
        p.power = Math.min(100, p.power + meta.metaPower / 20);
        TierProgression.syncTier(p);

        Config cfg2 = cloneConfig(ended.cfg);
        cfg2.seed = ended.cfg.seed + ended.cycleNo * 1000L + 17L; // new seed drift

        GameSession s2 = newSession(cfg2, p, meta);
        s2.cycleNo = ended.cycleNo + 1;
        return s2;
    }

    private static PlayerState createReincarnatedPlayer(Rand rand, String race) {
        // Origins:
        // - commoner / noble / royal
        // - orphan_cult (kidnapped by demon worshippers)
        int r = rand.nextInt(100);
        String origin;
        String originDetail = "";
        String faith = "none";

        if (r < 55) {
            origin = "commoner";
            originDetail = switch (rand.nextInt(4)) {
                case 0 -> "farmer";
                case 1 -> "merchant";
                case 2 -> "soldier";
                default -> "artisan";
            };
        } else if (r < 80) {
            origin = "noble";
            originDetail = switch (rand.nextInt(4)) {
                case 0 -> "baron";
                case 1 -> "count";
                case 2 -> "marquis";
                default -> "duke";
            };
        } else if (r < 90) {
            origin = "royal";
            originDetail = "succession_" + (1 + rand.nextInt(4));
        } else {
            origin = "orphan_cult";
            originDetail = "kidnapped_orphan";
            faith = "demon_worshippers";
        }

        PlayerState p = createPlayer(race, "none", origin, originDetail, faith);
        p.name = NameGenerator.randomName(rand, race);
        return p;
    }
}
