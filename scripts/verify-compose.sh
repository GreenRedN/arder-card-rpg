#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

for command_name in docker curl jq; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "[deploy-qa] Missing required command: $command_name" >&2
    exit 2
  fi
done

if ! docker info >/dev/null 2>&1; then
  echo "[deploy-qa] Docker daemon is not available." >&2
  exit 2
fi
if [ -z "${POSTGRES_PASSWORD:-}" ]; then
  echo "[deploy-qa] Set POSTGRES_PASSWORD without printing it, then run again." >&2
  exit 2
fi

export APP_PORT="${APP_PORT:-18080}"
export COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-fantasy-sim-qa-$$}"
base_url="http://127.0.0.1:${APP_PORT}"

cleanup() {
  status=$?
  trap - EXIT INT TERM
  if [ "$status" -ne 0 ]; then
    docker compose logs --no-color --tail=160 >&2 || true
  fi
  if [ "${KEEP_COMPOSE:-0}" != "1" ]; then
    docker compose down --volumes --remove-orphans >/dev/null 2>&1 || true
  fi
  exit "$status"
}
trap cleanup EXIT INT TERM

docker compose config --quiet
docker compose up --build --detach

for _ in $(seq 1 90); do
  if curl --fail --silent "${base_url}/api/health" | jq -e '.status == "ok" and .database == "up"' >/dev/null; then
    break
  fi
  sleep 1
done
curl --fail --silent "${base_url}/api/health" | jq -e '.status == "ok" and .database == "up"' >/dev/null

created=$(curl --fail --silent --request POST "${base_url}/api/v2/campaigns" \
  --header 'Content-Type: application/json' \
  --data '{"playerName":"배포검증","race":"elf","seed":20260718}')
campaign_id=$(jq -er '.campaign.campaignId' <<<"$created")
access_token=$(jq -er '.accessToken' <<<"$created")
choice_id=$(jq -er '[.campaign.scene.choices[] | select(.locked == false)][0].id' <<<"$created")

chosen=$(curl --fail --silent --request POST "${base_url}/api/v2/campaigns/${campaign_id}/choices" \
  --header 'Content-Type: application/json' \
  --header "X-Campaign-Token: ${access_token}" \
  --data "{\"choiceId\":\"${choice_id}\",\"requestId\":\"compose-smoke-1\"}")
jq -e '.campaign.turn == 1 and .campaign.version == 2' <<<"$chosen" >/dev/null

docker compose restart app >/dev/null
for _ in $(seq 1 60); do
  if curl --fail --silent "${base_url}/api/health" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

resumed=$(curl --fail --silent "${base_url}/api/v2/campaigns/${campaign_id}" \
  --header "X-Campaign-Token: ${access_token}")
jq -e '.accessToken == null and .campaign.turn == 1 and .campaign.version == 2' <<<"$resumed" >/dev/null

row_count=$(docker compose exec --no-TTY db sh -c \
  'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc "SELECT COUNT(*) FROM story_campaign"')
if [ "${row_count//[[:space:]]/}" -lt 1 ]; then
  echo "[deploy-qa] PostgreSQL did not retain the campaign row." >&2
  exit 1
fi

echo "[deploy-qa] PASS: image health, PostgreSQL persistence, API flow, and app restart recovery."
