package com.green.fantasysim.story;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Rule-owned card loop. It never asks an AI to decide outcomes, numbers, or memory writes.
 */
public class StoryEngine {
    private static final int REQUEST_HISTORY_LIMIT = 64;
    private static final int MESSAGE_HISTORY_LIMIT = 240;
    private static final int MEMORY_LIMIT = 256;

    public StoryCampaign create(String campaignId, String playerName, String race, long seed) {
        if (campaignId == null || campaignId.isBlank()) throw new IllegalArgumentException("campaign id required");
        if (playerName == null || playerName.isBlank()) throw new IllegalArgumentException("player name required");
        String normalizedRace = normalizeRace(race);

        StoryCampaign c = new StoryCampaign();
        c.id = campaignId;
        c.version = 1;
        c.seed = seed;
        c.turn = 0;
        c.chapter = 1;
        c.status = "ACTIVE";
        c.scenarioId = ArderScenario.ID;
        c.createdAt = Instant.now().toString();
        c.updatedAt = c.createdAt;

        c.player = new StoryPlayer();
        c.player.name = playerName.trim();
        c.player.race = normalizedRace;
        c.player.raceLabel = raceLabel(normalizedRace);
        c.player.job = "none";
        c.player.jobLabel = "평민";
        c.player.hp = 100;
        c.player.power = switch (normalizedRace) {
            case "elf" -> 19;
            case "beast" -> 20;
            case "dwarf" -> 20;
            default -> 18;
        };
        c.player.gold = "dwarf".equals(normalizedRace) ? 75 : 60;
        c.player.insight = 0;

        c.world = new StoryWorld();
        c.world.continent = "아르데르 대륙";
        c.world.worldMinutes = 18 * 60L + 10L;
        c.world.empireStability = 44;
        c.world.demonInfluence = 58;
        c.world.publicMood = 40;
        c.world.chapterTitle = "제1장 · 비 내리는 성문";

        c.relationships.put(ArderScenario.COMPANION_ID,
                new StoryRelationship(ArderScenario.COMPANION_ID, "세라 아벨린", "태양교단 조사관", 5, 5, 10));

        addMessage(c, "SYSTEM", "system", "기록", "",
                "선택의 결과는 코드가 확정하며, 중요한 약속과 비밀은 장기 기억에 보존됩니다.");
        enterScene(c, ArderScenario.INITIAL_SCENE);
        refreshRecap(c);
        return c;
    }

    public StoryCampaign choose(StoryCampaign c, String choiceId, String requestId) {
        Objects.requireNonNull(c, "campaign");
        if (!"ACTIVE".equals(c.status)) throw new IllegalStateException("이미 종료된 이야기입니다.");
        if (requestId == null || requestId.isBlank()) throw new IllegalArgumentException("requestId required");
        if (c.processedRequestIds.contains(requestId)) return c;
        if (c.scene == null || c.scene.choices == null) throw new IllegalStateException("선택 가능한 장면이 없습니다.");

        StoryChoice selected = c.scene.choices.stream()
                .filter(x -> choiceId != null && choiceId.equals(x.id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("현재 장면에 없는 선택지입니다."));
        if (selected.locked) throw new IllegalStateException(selected.lockedReason == null ? "잠긴 선택지입니다." : selected.lockedReason);

        c.turn++;
        addMessage(c, "PLAYER", "player", c.player.name, "선택", selected.playerLine);
        String effectSummary = apply(c, selected.effect);
        c.world.worldMinutes += Math.max(0, selected.timeMinutes);

        if (selected.memoryImportance > 0) addMemory(c, selected);
        if (!effectSummary.isBlank()) {
            addMessage(c, "SYSTEM", "system", "상태 변화", "", effectSummary);
        }

        c.eventHistory.add(c.scene.id + ":" + selected.id);
        if (c.eventHistory.size() > 120) c.eventHistory.remove(0);
        c.processedRequestIds.add(requestId);
        if (c.processedRequestIds.size() > REQUEST_HISTORY_LIMIT) c.processedRequestIds.remove(0);

        if (c.player.hp <= 0) {
            c.status = "ENDED";
            addMessage(c, "SYSTEM", "system", "이야기 종료", "", "당신의 여정은 이곳에서 끝났습니다.");
            c.scene = null;
        } else {
            enterScene(c, selected.nextSceneId);
        }

        c.updatedAt = Instant.now().toString();
        refreshRecap(c);
        return c;
    }

    public List<StoryMemory> relevantMemories(StoryCampaign c, int limit) {
        if (c == null || c.memories == null) return List.of();
        return c.memories.stream()
                .filter(m -> m.active)
                .sorted(Comparator.comparingInt((StoryMemory m) -> m.importance).reversed()
                        .thenComparing(Comparator.comparingInt((StoryMemory m) -> m.turn).reversed()))
                .limit(Math.max(0, limit))
                .toList();
    }

    private void enterScene(StoryCampaign c, String sceneId) {
        StoryScene scene = ArderScenario.scene(c, sceneId);
        c.scene = scene;
        c.world.locationId = scene.locationId;
        c.world.locationName = scene.locationName;
        c.world.objective = scene.objective;
        c.world.weather = weatherFor(scene.locationId);
        if ("hub".equals(scene.id) && c.chapter == 1) {
            c.chapter = 2;
            c.world.chapterTitle = "제2장 · 잿빛 서약";
        }
        addMessage(c, "NARRATOR", "narrator", "이야기", scene.mood, scene.narration);
        addMessage(c, "NPC", scene.speakerId, scene.speakerName, scene.mood, scene.dialogue);
    }

    private static String apply(StoryCampaign c, StoryEffect e) {
        if (e == null) return "";
        List<String> changes = new ArrayList<>();

        int oldHp = c.player.hp;
        int oldPower = c.player.power;
        int oldGold = c.player.gold;
        int oldInsight = c.player.insight;
        c.player.hp = clamp(c.player.hp + e.dHp, 0, 100);
        c.player.power = clamp(c.player.power + e.dPower, 1, 110);
        c.player.gold = Math.max(0, c.player.gold + e.dGold);
        c.player.insight = clamp(c.player.insight + e.dInsight, 0, 100);
        c.player.goodScore += e.dGood;
        c.player.neutralScore += e.dNeutral;
        c.player.evilScore += e.dEvil;

        addDelta(changes, "체력", c.player.hp - oldHp);
        addDelta(changes, "강함", c.player.power - oldPower);
        addDelta(changes, "골드", c.player.gold - oldGold);
        addDelta(changes, "통찰", c.player.insight - oldInsight);

        StoryRelationship r = c.relationships.get(ArderScenario.COMPANION_ID);
        if (r != null) {
            int oldTrust = r.trust;
            int oldAffinity = r.affinity;
            int oldGuard = r.guard;
            r.trust = clamp(r.trust + e.dTrust, 0, 100);
            r.affinity = clamp(r.affinity + e.dAffinity, 0, 100);
            r.guard = clamp(r.guard + e.dGuard, 0, 100);
            addDelta(changes, "세라 신뢰", r.trust - oldTrust);
            addDelta(changes, "세라 호감", r.affinity - oldAffinity);
            addDelta(changes, "세라 경계", r.guard - oldGuard);
        }

        c.world.empireStability = clamp(c.world.empireStability + e.dEmpire, 0, 100);
        c.world.demonInfluence = clamp(c.world.demonInfluence + e.dDemon, 0, 100);
        c.world.publicMood = clamp(c.world.publicMood + e.dMood, 0, 100);
        addDelta(changes, "제국 안정", e.dEmpire);
        addDelta(changes, "마계 영향", e.dDemon);
        addDelta(changes, "민심", e.dMood);

        if (e.addItem != null && !e.addItem.isBlank() && !c.player.inventory.contains(e.addItem)) {
            c.player.inventory.add(e.addItem);
            changes.add("획득 · " + e.addItem);
        }
        if (e.removeItem != null && c.player.inventory.remove(e.removeItem)) {
            changes.add("소모 · " + e.removeItem);
        }
        if (e.setFlag != null && !e.setFlag.isBlank()) {
            c.flags.put(e.setFlag, e.setFlagValue == null ? "true" : e.setFlagValue);
        }
        if (e.setJob != null && !e.setJob.isBlank()) {
            c.player.job = e.setJob;
            c.player.jobLabel = e.setJobLabel == null || e.setJobLabel.isBlank() ? e.setJob : e.setJobLabel;
            changes.add("직업 · " + c.player.jobLabel);
        }

        return String.join("  ·  ", changes);
    }

    private static void addMemory(StoryCampaign c, StoryChoice selected) {
        StoryMemory existing = c.memories.stream()
                .filter(m -> m.active)
                .filter(m -> Objects.equals(m.type, selected.memoryType))
                .filter(m -> Objects.equals(m.title, selected.memoryTitle))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            existing.turn = c.turn;
            existing.summary = selected.memorySummary;
            existing.importance = Math.max(existing.importance, clamp(selected.memoryImportance, 1, 100));
            selected.memoryTags.forEach(tag -> {
                if (!existing.tags.contains(tag)) existing.tags.add(tag);
            });
            return;
        }

        StoryMemory m = new StoryMemory();
        m.id = "memory-" + c.turn + "-" + (c.memories.size() + 1);
        m.turn = c.turn;
        m.type = selected.memoryType;
        m.subjectId = selected.memoryTags.contains("sera") ? ArderScenario.COMPANION_ID : "world";
        m.title = selected.memoryTitle;
        m.summary = selected.memorySummary;
        m.importance = clamp(selected.memoryImportance, 1, 100);
        m.active = true;
        m.tags.addAll(selected.memoryTags);
        c.memories.add(m);
        if (c.memories.size() > MEMORY_LIMIT) {
            StoryMemory removable = c.memories.stream()
                    .filter(memory -> !memory.tags.contains("main_quest"))
                    .min(Comparator.comparingInt((StoryMemory memory) -> memory.importance)
                            .thenComparingInt(memory -> memory.turn))
                    .orElseGet(() -> c.memories.stream()
                            .min(Comparator.comparingInt((StoryMemory memory) -> memory.importance)
                                    .thenComparingInt(memory -> memory.turn))
                            .orElse(null));
            if (removable != null) c.memories.remove(removable);
        }
    }

    private static void addMessage(StoryCampaign c, String role, String speakerId, String speakerName, String mood, String text) {
        if (text == null || text.isBlank()) return;
        long turnSequence = c.messages.stream().filter(message -> message.turn == c.turn).count() + 1;
        String id = "message-" + c.turn + "-" + turnSequence;
        c.messages.add(new StoryMessage(id, c.turn, role, speakerId, speakerName, mood, text));
        if (c.messages.size() > MESSAGE_HISTORY_LIMIT) c.messages.remove(0);
    }

    private void refreshRecap(StoryCampaign c) {
        List<StoryMemory> top = relevantMemories(c, 3);
        if (top.isEmpty()) {
            c.recap = c.player.name + "은 솔라니아 제국 서문에서 태양교단 조사관 세라를 만났다.";
            return;
        }
        c.recap = top.stream().map(m -> m.summary).reduce((a, b) -> a + " " + b).orElse("");
    }

    private static String weatherFor(String locationId) {
        if (locationId == null) return "흐림";
        if (locationId.contains("sun-dune")) return "건조한 서풍";
        if (locationId.contains("aqueduct")) return "차가운 습기";
        if (locationId.contains("chapel") || locationId.contains("copse")) return "가느다란 비";
        return "늦은 비";
    }

    private static void addDelta(List<String> out, String label, int delta) {
        if (delta == 0) return;
        out.add(label + " " + (delta > 0 ? "+" : "") + delta);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalizeRace(String race) {
        if (race == null) throw new IllegalArgumentException("race required");
        String normalized = race.trim().toLowerCase(Locale.ROOT);
        if (!List.of("human", "elf", "beast", "dwarf").contains(normalized)) {
            throw new IllegalArgumentException("invalid race");
        }
        return normalized;
    }

    private static String raceLabel(String race) {
        return switch (race) {
            case "elf" -> "엘프";
            case "beast" -> "수인";
            case "dwarf" -> "드워프";
            default -> "인간";
        };
    }
}
