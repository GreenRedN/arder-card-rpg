package com.green.fantasysim.api.story.dto;

import com.green.fantasysim.story.*;

import java.util.Comparator;
import java.util.List;

public record StoryCampaignView(
        String campaignId,
        long version,
        String status,
        int turn,
        int chapter,
        String scenarioId,
        String timeLabel,
        String recap,
        PlayerView player,
        WorldView world,
        List<RelationshipView> relationships,
        DirectorView director,
        SceneView scene,
        List<MessageView> messages,
        List<MemoryView> memories
) {
    public static StoryCampaignView from(StoryCampaign c) {
        PlayerView player = new PlayerView(c.player.name, c.player.race, c.player.raceLabel,
                c.player.job, c.player.jobLabel, c.player.hp, c.player.power, c.player.gold,
                c.player.insight, c.player.alignmentLabel(), List.copyOf(c.player.inventory));
        WorldView world = new WorldView(c.world.continent, c.world.locationId, c.world.locationName,
                c.world.weather, c.world.chapterTitle, c.world.objective,
                c.world.empireStability, c.world.demonInfluence, c.world.publicMood);
        List<RelationshipView> relationships = c.relationships.values().stream()
                .map(r -> new RelationshipView(r.characterId, r.name, r.role, r.affinity, r.trust, r.guard, r.stageLabel()))
                .toList();
        StoryDirector d = c.director == null ? new StoryDirector() : c.director;
        DirectorView director = new DirectorView(d.source, d.liveAi, d.turn, d.role, d.model,
                d.intent, d.spotlightChoiceId, d.spotlightReason,
                d.recalledMemoryIds == null ? List.of() : List.copyOf(d.recalledMemoryIds),
                d.constraints == null ? List.of() : List.copyOf(d.constraints), d.fallbackReason);
        SceneView scene = c.scene == null ? null : new SceneView(c.scene.id, c.scene.title,
                c.scene.speakerId, c.scene.speakerName, c.scene.speakerRole, c.scene.mood,
                c.scene.choices.stream().map(x -> new ChoiceView(x.id, x.text, x.category, x.risk,
                        x.locked, x.lockedReason)).toList());
        List<MessageView> messages = c.messages.stream()
                .map(m -> new MessageView(m.id, m.turn, m.role, m.speakerId, m.speakerName, m.mood, m.text))
                .toList();
        List<MemoryView> memories = c.memories.stream()
                .filter(m -> m.active)
                .sorted(Comparator.comparingInt((StoryMemory m) -> m.turn).reversed())
                .map(m -> new MemoryView(m.id, m.turn, m.type, m.title, m.summary, m.importance, List.copyOf(m.tags)))
                .toList();
        return new StoryCampaignView(c.id, c.version, c.status, c.turn, c.chapter, c.scenarioId,
                c.world.timeLabel(), c.recap, player, world, relationships, director, scene, messages, memories);
    }

    public record PlayerView(String name, String race, String raceLabel, String job, String jobLabel,
                             int hp, int power, int gold, int insight, String alignment,
                             List<String> inventory) {}

    public record WorldView(String continent, String locationId, String locationName, String weather,
                            String chapterTitle, String objective, int empireStability,
                            int demonInfluence, int publicMood) {}

    public record RelationshipView(String characterId, String name, String role, int affinity,
                                   int trust, int guard, String stageLabel) {}

    public record DirectorView(String source, boolean liveAi, int turn, String role, String model,
                               String intent, String spotlightChoiceId, String spotlightReason,
                               List<String> recalledMemoryIds, List<String> constraints,
                               String fallbackReason) {}

    public record SceneView(String id, String title, String speakerId, String speakerName,
                            String speakerRole, String mood, List<ChoiceView> choices) {}

    public record ChoiceView(String id, String text, String category, String risk,
                             boolean locked, String lockedReason) {}

    public record MessageView(String id, int turn, String role, String speakerId,
                              String speakerName, String mood, String text) {}

    public record MemoryView(String id, int turn, String type, String title,
                             String summary, int importance, List<String> tags) {}
}
