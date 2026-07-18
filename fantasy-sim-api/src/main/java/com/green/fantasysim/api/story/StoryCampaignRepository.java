package com.green.fantasysim.api.story;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.green.fantasysim.story.StoryCampaign;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class StoryCampaignRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public StoryCampaignRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void create(StoryCampaign campaign, String accessTokenHash) {
        jdbc.update("""
                INSERT INTO story_campaign
                    (id, access_token_hash, version, player_name, status, snapshot_json, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                campaign.id, accessTokenHash, campaign.version, campaign.player.name, campaign.status,
                toJson(campaign), campaign.createdAt, campaign.updatedAt);
    }

    public Optional<StoredCampaign> find(String id) {
        try {
            StoredCampaign stored = jdbc.queryForObject("""
                    SELECT access_token_hash, snapshot_json
                    FROM story_campaign
                    WHERE id = ?
                    """, this::map, id);
            return Optional.ofNullable(stored);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void update(StoryCampaign campaign, long expectedVersion) {
        long nextVersion = expectedVersion + 1;
        campaign.version = nextVersion;
        int changed = jdbc.update("""
                UPDATE story_campaign
                SET version = ?, player_name = ?, status = ?, snapshot_json = ?, updated_at = ?
                WHERE id = ? AND version = ?
                """,
                nextVersion, campaign.player.name, campaign.status, toJson(campaign), campaign.updatedAt,
                campaign.id, expectedVersion);
        if (changed != 1) throw new StoryConcurrentUpdateException();
    }

    private StoredCampaign map(ResultSet rs, int rowNum) throws SQLException {
        String hash = rs.getString("access_token_hash");
        String json = rs.getString("snapshot_json");
        try {
            return new StoredCampaign(objectMapper.readValue(json, StoryCampaign.class), hash);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("저장된 이야기 데이터를 읽을 수 없습니다.", e);
        }
    }

    private String toJson(StoryCampaign campaign) {
        try {
            return objectMapper.writeValueAsString(campaign);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("이야기를 저장할 수 없습니다.", e);
        }
    }

    public record StoredCampaign(StoryCampaign campaign, String accessTokenHash) {}
}
