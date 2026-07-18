package com.green.fantasysim.api.story;

import com.green.fantasysim.story.StoryCampaign;
import com.green.fantasysim.story.StoryEngine;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class StoryCampaignService {
    private static final int LOCK_STRIPES = 256;
    private final StoryCampaignRepository repository;
    private final StoryTokenService tokenService;
    private final NarrativeEnhancer narrativeEnhancer;
    private final StoryEngine engine = new StoryEngine();
    private final SecureRandom random = new SecureRandom();
    private final ReentrantLock[] locks = createLocks();

    public StoryCampaignService(StoryCampaignRepository repository,
                                StoryTokenService tokenService,
                                NarrativeEnhancer narrativeEnhancer) {
        this.repository = repository;
        this.tokenService = tokenService;
        this.narrativeEnhancer = narrativeEnhancer;
    }

    public CreatedCampaign create(String playerName, String race, Long requestedSeed) {
        String id = UUID.randomUUID().toString();
        String accessToken = tokenService.issue();
        long seed = requestedSeed == null ? random.nextLong() : requestedSeed;
        StoryCampaign campaign = engine.create(id, playerName, race, seed);
        narrativeEnhancer.enhance(campaign);
        repository.create(campaign, tokenService.hash(accessToken));
        return new CreatedCampaign(accessToken, campaign);
    }

    public StoryCampaign get(String id, String accessToken) {
        return authenticated(id, accessToken).campaign();
    }

    public StoryCampaign choose(String id, String accessToken, String choiceId, String requestId) {
        ReentrantLock lock = locks[Math.floorMod(id.hashCode(), locks.length)];
        lock.lock();
        try {
            StoryCampaignRepository.StoredCampaign stored = authenticated(id, accessToken);
            StoryCampaign campaign = stored.campaign();
            if (campaign.processedRequestIds.contains(requestId)) return campaign;

            long expectedVersion = campaign.version;
            engine.choose(campaign, choiceId, requestId);
            narrativeEnhancer.enhance(campaign);
            repository.update(campaign, expectedVersion);
            return campaign;
        } finally {
            lock.unlock();
        }
    }

    private static ReentrantLock[] createLocks() {
        ReentrantLock[] result = new ReentrantLock[LOCK_STRIPES];
        for (int i = 0; i < result.length; i++) result[i] = new ReentrantLock();
        return result;
    }

    private StoryCampaignRepository.StoredCampaign authenticated(String id, String accessToken) {
        StoryCampaignRepository.StoredCampaign stored = repository.find(id)
                .orElseThrow(() -> new StoryNotFoundException(id));
        if (!tokenService.matches(accessToken, stored.accessTokenHash())) throw new StoryAccessDeniedException();
        return stored;
    }

    public record CreatedCampaign(String accessToken, StoryCampaign campaign) {}
}
