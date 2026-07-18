package com.green.fantasysim.api.story;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:story-api-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "fantasy.ai.endpoint=",
        "fantasy.ai.api-key=",
        "fantasy.ai.model="
})
@AutoConfigureMockMvc
class StoryCampaignApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void campaignCanBeCreatedChosenIdempotentlyAndResumed() throws Exception {
        MvcResult createdResult = mvc.perform(post("/api/v2/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerName":"그린","race":"elf","seed":12345}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.campaign.turn").value(0))
                .andExpect(jsonPath("$.campaign.version").value(1))
                .andExpect(jsonPath("$.campaign.player.race").value("elf"))
                .andExpect(jsonPath("$.campaign.director.source").value("RULE_FALLBACK"))
                .andExpect(jsonPath("$.campaign.director.liveAi").value(false))
                .andExpect(jsonPath("$.campaign.director.spotlightChoiceId").isNotEmpty())
                .andExpect(jsonPath("$.campaign.director.constraints").isArray())
                .andReturn();

        JsonNode created = json(createdResult);
        String campaignId = created.at("/campaign/campaignId").asText();
        String token = created.path("accessToken").asText();
        String choiceId = created.at("/campaign/scene/choices/0/id").asText();
        assertFalse(campaignId.isBlank());
        assertFalse(token.isBlank());
        assertFalse(choiceId.isBlank());
        assertFalse(created.at("/campaign/scene/choices/0").has("nextSceneId"));
        assertFalse(created.at("/campaign/scene/choices/0").has("effect"));

        mvc.perform(get("/api/v2/campaigns/{id}", campaignId)
                        .header(StoryCampaignController.TOKEN_HEADER, "wrong-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        mvc.perform(get("/api/v2/campaigns/{id}", campaignId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        String chooseBody = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("choiceId", choiceId)
                .put("requestId", "integration-request-1"));

        mvc.perform(post("/api/v2/campaigns/{id}/choices", campaignId)
                        .header(StoryCampaignController.TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chooseBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaign.turn").value(1))
                .andExpect(jsonPath("$.campaign.version").value(2))
                .andExpect(jsonPath("$.campaign.memories[0].type").value("PROMISE"))
                .andExpect(jsonPath("$.campaign.director.recalledMemoryIds[0]").isNotEmpty())
                .andExpect(jsonPath("$.campaign.director.spotlightChoiceId").isNotEmpty());

        // A retried mobile request must not apply the card twice.
        mvc.perform(post("/api/v2/campaigns/{id}/choices", campaignId)
                        .header(StoryCampaignController.TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chooseBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaign.turn").value(1))
                .andExpect(jsonPath("$.campaign.version").value(2));

        mvc.perform(get("/api/v2/campaigns/{id}", campaignId)
                        .header(StoryCampaignController.TOKEN_HEADER, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.campaign.turn").value(1))
                .andExpect(jsonPath("$.campaign.memories[0].type").value("PROMISE"))
                .andExpect(jsonPath("$.campaign.messages").isArray());
    }

    @Test
    void webClientIsServedFromTheApi() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.database").value("up"));

        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ARDER CHRONICLES")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"director-card\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"director-status\"")));

        mvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/javascript"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("renderDirector")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("is-director-pick")));

        mvc.perform(get("/app.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".director-card")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".director-badge")));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }
}
