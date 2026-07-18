CREATE TABLE IF NOT EXISTS story_campaign (
    id VARCHAR(36) PRIMARY KEY,
    access_token_hash VARCHAR(128) NOT NULL,
    version BIGINT NOT NULL,
    player_name VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at VARCHAR(40) NOT NULL,
    updated_at VARCHAR(40) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_story_campaign_updated_at
    ON story_campaign(updated_at);
