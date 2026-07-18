package com.green.fantasysim.api.story;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.green.fantasysim.story.StoryCampaign;
import com.green.fantasysim.story.StoryChoice;
import com.green.fantasysim.story.StoryDirector;
import com.green.fantasysim.story.StoryMemory;
import com.green.fantasysim.story.StoryMessage;
import com.green.fantasysim.story.StoryRelationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Optional OpenAI-compatible narrative director. The model may rewrite prose,
 * explain its direction, recall active memories, and spotlight one existing
 * unlocked card. Empty configuration keeps a clearly labelled rule fallback.
 */
@Component
public class NarrativeEnhancer {
    private static final Logger log = LoggerFactory.getLogger(NarrativeEnhancer.class);
    private static final List<String> DIRECTOR_CONSTRAINTS = List.of(
            "게임 수치와 성공 여부는 Java 규칙 엔진만 변경",
            "새 선택지를 만들지 않고 현재 열린 카드만 주목",
            "활성 장기 기억만 회상",
            "공식 세계관 밖 국가·신격·마계 위계 생성 금지"
    );
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String endpoint;
    private final String apiKey;
    private final String model;

    public NarrativeEnhancer(ObjectMapper objectMapper,
                             @Value("${fantasy.ai.endpoint:}") String endpoint,
                             @Value("${fantasy.ai.api-key:}") String apiKey,
                             @Value("${fantasy.ai.model:}") String model) {
        this.objectMapper = objectMapper;
        this.endpoint = endpoint == null ? "" : endpoint.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public void enhance(StoryCampaign campaign) {
        if (campaign == null || campaign.scene == null) return;
        configureRuleFallback(campaign, "NOT_CONFIGURED");
        if (endpoint.isBlank() || model.isBlank()) return;
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("temperature", 0.7);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt()),
                    Map.of("role", "user", "content", contextPrompt(campaign))
            ));

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(18))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));
            if (!apiKey.isBlank()) requestBuilder.header("Authorization", "Bearer " + apiKey);
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Narrative provider returned HTTP {}. Using curated fallback.", response.statusCode());
                campaign.director.fallbackReason = "PROVIDER_HTTP_ERROR";
                return;
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            JsonNode rendered = objectMapper.readTree(stripFence(content));
            String narration = clean(rendered.path("narration").asText(""), 2000);
            String dialogue = clean(rendered.path("dialogue").asText(""), 2000);
            if (narration.isBlank() || dialogue.isBlank()) {
                campaign.director.fallbackReason = "INVALID_OUTPUT";
                return;
            }

            campaign.scene.narration = narration;
            campaign.scene.dialogue = dialogue;
            replaceLastSceneMessages(campaign, narration, dialogue);
            applyLiveDirectorTrace(campaign, rendered);
        } catch (Exception e) {
            campaign.director.fallbackReason = "PROVIDER_ERROR";
            log.warn("Narrative provider failed. Using curated fallback: {}", e.getClass().getSimpleName());
        }
    }

    private static String systemPrompt() {
        return """
                당신은 카드 선택형 판타지 RPG의 한국어 AI 내러티브 디렉터다.
                코드는 이미 사건, 선택 후보, 결과를 확정했다. 수치, 아이템, 성공 여부, 새 선택지를 만들지 마라.
                아르데르 대륙의 공식 국가명은 솔라니아 제국, 카라자드 사막왕국,
                세븐크라운 연합왕국, 마레노스 왕국이며 동부는 실바린 숲령이다.
                태양교단과 북부 끝의 월식 마경 외에 새 국가, 신격, 마계 위계를 만들지 마라.
                활성 기억과 현재 장면의 사실을 보존하면서 문장을 연출하고, 열린 카드 중 하나만 주목시켜라.
                recalledMemoryIds에는 입력으로 받은 활성 기억 ID만, spotlightChoiceId에는 열린 카드 ID 하나만 넣어라.
                반드시 아래 형태의 JSON 하나만 출력하라. 마크다운은 금지한다.
                {"narration":"...","dialogue":"...","intent":"이번 장면의 연출 의도",
                 "spotlightChoiceId":"기존 열린 카드 ID","spotlightReason":"왜 이 선택을 주목시키는지",
                 "recalledMemoryIds":["활성 기억 ID"]}
                """;
    }

    private static String contextPrompt(StoryCampaign c) {
        StoryRelationship sera = c.relationships.get("sera");
        String memories = c.memories.stream()
                .filter(m -> m.active)
                .sorted(Comparator.comparingInt(NarrativeEnhancer::memoryScore).reversed()
                        .thenComparing(Comparator.comparingInt((StoryMemory memory) -> memory.turn).reversed()))
                .limit(8)
                .map(m -> "- " + m.id + " | 중요도 " + m.importance + " | 태그 "
                        + String.join(",", m.tags) + " | " + m.summary)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("- 아직 장기 기억 없음");
        String choices = c.scene.choices.stream()
                .filter(choice -> !choice.locked)
                .map(choice -> "- " + choice.id + " | " + choice.category + " | " + choice.risk + " | " + choice.text)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("- 열린 카드 없음");
        return """
                플레이어: %s / 종족: %s / 직업: %s / 강함: %d
                현재 장소: %s / 현재 목표: %s
                세라 관계: 신뢰 %d, 호감 %d, 경계 %d
                관련 장기 기억:
                %s

                장면 제목: %s
                화자: %s (%s), 감정: %s
                확정된 장면 묘사: %s
                확정된 대사: %s

                코드가 허용한 열린 카드:
                %s

                묘사는 2~4문장, 대사는 캐릭터답게 1~3문장으로 변주하라.
                연출 의도와 주목 이유는 각각 한 문장으로 간결하게 작성하라.
                """.formatted(c.player.name, c.player.raceLabel, c.player.jobLabel, c.player.power,
                c.world.locationName, c.world.objective,
                sera == null ? 0 : sera.trust, sera == null ? 0 : sera.affinity, sera == null ? 0 : sera.guard,
                memories, c.scene.title, c.scene.speakerName, c.scene.speakerRole, c.scene.mood,
                c.scene.narration, c.scene.dialogue, choices);
    }

    private void applyLiveDirectorTrace(StoryCampaign campaign, JsonNode rendered) {
        StoryDirector director = campaign.director;
        director.source = "LIVE_AI";
        director.liveAi = true;
        director.model = model;
        director.turn = campaign.turn;
        director.intent = clean(rendered.path("intent").asText(""), 240);
        if (director.intent.isBlank()) director.intent = fallbackIntent(campaign);

        String proposedChoiceId = clean(rendered.path("spotlightChoiceId").asText(""), 120);
        if (isUnlockedChoice(campaign, proposedChoiceId)) director.spotlightChoiceId = proposedChoiceId;
        director.spotlightReason = clean(rendered.path("spotlightReason").asText(""), 240);
        if (director.spotlightReason.isBlank()) {
            director.spotlightReason = fallbackSpotlightReason(campaign, director.spotlightChoiceId);
        }

        Set<String> activeMemoryIds = new HashSet<>();
        campaign.memories.stream().filter(memory -> memory.active).forEach(memory -> activeMemoryIds.add(memory.id));
        List<String> validatedMemories = new ArrayList<>();
        JsonNode recalled = rendered.path("recalledMemoryIds");
        if (recalled.isArray()) {
            recalled.forEach(node -> {
                String id = clean(node.asText(""), 120);
                if (activeMemoryIds.contains(id) && !validatedMemories.contains(id)) validatedMemories.add(id);
            });
        }
        if (!validatedMemories.isEmpty()) director.recalledMemoryIds = validatedMemories;
        director.fallbackReason = "";
    }

    private static void configureRuleFallback(StoryCampaign campaign, String reason) {
        StoryDirector director = new StoryDirector();
        director.source = "RULE_FALLBACK";
        director.liveAi = false;
        director.turn = campaign.turn;
        director.intent = fallbackIntent(campaign);
        director.spotlightChoiceId = fallbackSpotlightChoice(campaign);
        director.spotlightReason = fallbackSpotlightReason(campaign, director.spotlightChoiceId);
        director.recalledMemoryIds = campaign.memories.stream()
                .filter(memory -> memory.active)
                .sorted(Comparator.comparingInt(NarrativeEnhancer::memoryScore).reversed()
                        .thenComparing(Comparator.comparingInt((StoryMemory memory) -> memory.turn).reversed()))
                .limit(8)
                .map(memory -> memory.id)
                .toList();
        director.constraints = new ArrayList<>(DIRECTOR_CONSTRAINTS);
        director.fallbackReason = reason;
        campaign.director = director;
    }

    private static int memoryScore(StoryMemory memory) {
        int score = memory.importance;
        if (memory.tags.contains("main_quest")) score += 40;
        if (memory.tags.contains("oath")) score += 25;
        if (memory.tags.contains("secret")) score += 15;
        if (memory.tags.contains("sera")) score += 10;
        return score;
    }

    private static String fallbackIntent(StoryCampaign campaign) {
        StoryRelationship relation = campaign.relationships.get("sera");
        if (relation != null && relation.guard >= 55) return "세라의 높은 경계를 드러내고 신뢰 회복의 여지를 남긴다.";
        if (relation != null && relation.trust >= 60) return "축적된 신뢰와 기억이 이번 판단에 무게를 더하도록 연출한다.";
        if (!campaign.memories.isEmpty()) return "이전 선택의 기억을 현재 목표와 연결해 관계의 연속성을 보여준다.";
        return "첫 만남의 긴장과 현재 목표를 분명히 제시해 다음 선택의 의미를 세운다.";
    }

    private static String fallbackSpotlightChoice(StoryCampaign campaign) {
        return campaign.scene.choices.stream()
                .filter(choice -> !choice.locked)
                .sorted(Comparator.comparingInt((StoryChoice choice) -> fallbackScore(campaign, choice)).reversed()
                        .thenComparing(choice -> choice.id))
                .map(choice -> choice.id)
                .findFirst()
                .orElse("");
    }

    private static int fallbackScore(StoryCampaign campaign, StoryChoice choice) {
        String category = choice.category == null ? "" : choice.category;
        StoryRelationship relation = campaign.relationships.get("sera");
        int score = "안전".equals(choice.risk) ? 1 : 0;
        if (relation != null && relation.trust < 35 && category.matches(".*(관계|신뢰|대화|구조|보호).*")) score += 5;
        if (campaign.world.demonInfluence >= 60 && category.matches(".*(조사|추적|전술|전투).*")) score += 4;
        if (category.matches(".*(조사|추적|기록|결단).*")) score += 2;
        return score;
    }

    private static String fallbackSpotlightReason(StoryCampaign campaign, String choiceId) {
        StoryChoice choice = campaign.scene.choices.stream()
                .filter(candidate -> candidate.id.equals(choiceId))
                .findFirst()
                .orElse(null);
        if (choice == null) return "현재 주목할 수 있는 열린 카드가 없습니다.";
        return "현재 목표와 관계 상태를 함께 전진시킬 가능성이 있는 열린 카드입니다.";
    }

    private static boolean isUnlockedChoice(StoryCampaign campaign, String choiceId) {
        if (choiceId == null || choiceId.isBlank()) return false;
        return campaign.scene.choices.stream().anyMatch(choice -> !choice.locked && choiceId.equals(choice.id));
    }

    private static void replaceLastSceneMessages(StoryCampaign c, String narration, String dialogue) {
        boolean npcDone = false;
        boolean narratorDone = false;
        for (int i = c.messages.size() - 1; i >= 0 && (!npcDone || !narratorDone); i--) {
            StoryMessage m = c.messages.get(i);
            if (!npcDone && "NPC".equals(m.role)) {
                m.text = dialogue;
                npcDone = true;
            } else if (!narratorDone && "NARRATOR".equals(m.role)) {
                m.text = narration;
                narratorDone = true;
            }
        }
    }

    private static String stripFence(String text) {
        String s = text == null ? "" : text.trim();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) s = s.substring(firstNewline + 1, lastFence).trim();
        }
        return s;
    }

    private static String clean(String text, int maxLength) {
        if (text == null) return "";
        String cleaned = text.trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }
}
