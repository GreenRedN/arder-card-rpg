package com.green.fantasysim.api.story;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.green.fantasysim.story.StoryCampaign;
import com.green.fantasysim.story.StoryEngine;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class NarrativeEnhancerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void unconfiguredProviderCreatesHonestRuleFallbackTrace() {
        StoryCampaign campaign = new StoryEngine().create("fallback", "그린", "human", 7L);

        new NarrativeEnhancer(objectMapper, "", "", "").enhance(campaign);

        assertEquals("RULE_FALLBACK", campaign.director.source);
        assertFalse(campaign.director.liveAi);
        assertEquals("NOT_CONFIGURED", campaign.director.fallbackReason);
        assertFalse(campaign.director.intent.isBlank());
        assertTrue(isUnlocked(campaign, campaign.director.spotlightChoiceId));
        assertFalse(campaign.director.constraints.isEmpty());
    }

    @Test
    void liveProviderMayDirectProseButOnlyWithinValidatedChoicesAndMemories() throws Exception {
        StoryEngine engine = new StoryEngine();
        StoryCampaign campaign = engine.create("live", "그린", "elf", 11L);
        engine.choose(campaign, "arrival_help", "director-test-1");
        String activeMemoryId = campaign.memories.stream().filter(memory -> memory.active)
                .map(memory -> memory.id).findFirst().orElseThrow();
        String candidateChoiceId = campaign.scene.choices.stream().filter(choice -> !choice.locked)
                .map(choice -> choice.id).findFirst().orElseThrow();

        String directorJson = objectMapper.writeValueAsString(Map.of(
                "narration", "AI 디렉터가 확정된 장면을 기억과 연결해 다시 연출했다.",
                "dialogue", "세라는 이전 약속을 기억하며 다음 판단을 물었다.",
                "intent", "이전 약속이 현재의 판단에 영향을 준다는 점을 드러낸다.",
                "spotlightChoiceId", "invented-choice",
                "spotlightReason", "활성 기억과 현재 목표를 동시에 잇는 선택이다.",
                "recalledMemoryIds", List.of(activeMemoryId, "invented-memory")
        ));
        String providerBody = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", directorJson)))
        ));
        AtomicReference<String> requestBody = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = providerBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            NarrativeEnhancer enhancer = new NarrativeEnhancer(objectMapper,
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/chat", "test-key", "test-model");
            enhancer.enhance(campaign);
        } finally {
            server.stop(0);
        }

        assertEquals("LIVE_AI", campaign.director.source);
        assertTrue(campaign.director.liveAi);
        assertEquals("test-model", campaign.director.model);
        assertNotEquals("invented-choice", campaign.director.spotlightChoiceId);
        assertTrue(isUnlocked(campaign, campaign.director.spotlightChoiceId));
        assertEquals(List.of(activeMemoryId), campaign.director.recalledMemoryIds);
        assertEquals("", campaign.director.fallbackReason);
        assertTrue(campaign.scene.narration.startsWith("AI 디렉터"));
        assertTrue(requestBody.get().contains(activeMemoryId));
        assertTrue(requestBody.get().contains(candidateChoiceId));
    }

    @Test
    void trustedKeylessOpenAiCompatibleProviderMayBeUsed() throws Exception {
        StoryCampaign campaign = new StoryEngine().create("keyless", "그린", "human", 17L);
        String choiceId = campaign.scene.choices.stream().filter(choice -> !choice.locked)
                .map(choice -> choice.id).findFirst().orElseThrow();
        String directorJson = objectMapper.writeValueAsString(Map.of(
                "narration", "로컬 디렉터가 확정된 장면을 다시 연출했다.",
                "dialogue", "세라는 조용히 다음 판단을 기다렸다.",
                "intent", "첫 선택의 긴장을 선명하게 만든다.",
                "spotlightChoiceId", choiceId,
                "spotlightReason", "현재 목표를 직접 전진시키는 열린 카드다.",
                "recalledMemoryIds", List.of()
        ));
        String providerBody = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", directorJson)))
        ));
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = providerBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            new NarrativeEnhancer(objectMapper,
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/chat", "", "local-model")
                    .enhance(campaign);
        } finally {
            server.stop(0);
        }

        assertEquals("LIVE_AI", campaign.director.source);
        assertEquals("local-model", campaign.director.model);
        assertNull(authorization.get());
    }

    @Test
    void invalidProviderOutputFallsBackWithoutChangingTheRuleResult() throws Exception {
        StoryCampaign campaign = new StoryEngine().create("invalid-output", "그린", "beast", 23L);
        String originalNarration = campaign.scene.narration;
        String originalDialogue = campaign.scene.dialogue;
        String providerBody = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", "{\"narration\":\"\",\"dialogue\":\"\"}")))
        ));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat", exchange -> {
            byte[] body = providerBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            new NarrativeEnhancer(objectMapper,
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/chat", "test-key", "bad-model")
                    .enhance(campaign);
        } finally {
            server.stop(0);
        }

        assertEquals("RULE_FALLBACK", campaign.director.source);
        assertFalse(campaign.director.liveAi);
        assertEquals("INVALID_OUTPUT", campaign.director.fallbackReason);
        assertEquals(originalNarration, campaign.scene.narration);
        assertEquals(originalDialogue, campaign.scene.dialogue);
        assertTrue(isUnlocked(campaign, campaign.director.spotlightChoiceId));
    }

    private static boolean isUnlocked(StoryCampaign campaign, String id) {
        return campaign.scene.choices.stream().anyMatch(choice -> id.equals(choice.id) && !choice.locked);
    }
}
