package com.green.fantasysim.api.story;

import com.green.fantasysim.api.story.dto.CreateStoryCampaignRequest;
import com.green.fantasysim.api.story.dto.StoryCampaignEnvelope;
import com.green.fantasysim.api.story.dto.StoryCampaignView;
import com.green.fantasysim.api.story.dto.StoryChoiceRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/campaigns")
public class StoryCampaignController {
    public static final String TOKEN_HEADER = "X-Campaign-Token";
    private final StoryCampaignService service;

    public StoryCampaignController(StoryCampaignService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<StoryCampaignEnvelope> create(@Valid @RequestBody CreateStoryCampaignRequest request) {
        StoryCampaignService.CreatedCampaign created = service.create(request.playerName, request.race, request.seed);
        return noStore(new StoryCampaignEnvelope(created.accessToken(), StoryCampaignView.from(created.campaign())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryCampaignEnvelope> get(@PathVariable("id") String id,
                                                      @RequestHeader(TOKEN_HEADER) String accessToken) {
        return noStore(new StoryCampaignEnvelope(null, StoryCampaignView.from(service.get(id, accessToken))));
    }

    @PostMapping("/{id}/choices")
    public ResponseEntity<StoryCampaignEnvelope> choose(@PathVariable("id") String id,
                                                         @RequestHeader(TOKEN_HEADER) String accessToken,
                                                         @Valid @RequestBody StoryChoiceRequest request) {
        return noStore(new StoryCampaignEnvelope(null,
                StoryCampaignView.from(service.choose(id, accessToken, request.choiceId, request.requestId))));
    }

    private static ResponseEntity<StoryCampaignEnvelope> noStore(StoryCampaignEnvelope body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
