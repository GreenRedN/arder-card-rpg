#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

for command_name in java curl jq; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "[ai-qa] Missing required command: $command_name" >&2
    exit 2
  fi
done
if [ -z "${FANTASY_AI_ENDPOINT:-}" ] || [ -z "${FANTASY_AI_MODEL:-}" ]; then
  echo "[ai-qa] FANTASY_AI_ENDPOINT and FANTASY_AI_MODEL are required." >&2
  exit 2
fi

jar="fantasy-sim-api/target/fantasy-sim-api-2.1.0.jar"
if [ ! -f "$jar" ]; then
  ./mvnw --batch-mode -DskipTests package
fi

mkdir -p .runtime
port="${AI_SMOKE_PORT:-18081}"
database_path="$PWD/.runtime/live-ai-smoke-db"
log_path="$PWD/.runtime/live-ai-smoke.log"

SERVER_PORT="$port" \
DB_URL="jdbc:h2:file:${database_path};MODE=PostgreSQL;AUTO_SERVER=FALSE" \
DB_USERNAME=sa \
DB_PASSWORD= \
FANTASY_AI_ENDPOINT="$FANTASY_AI_ENDPOINT" \
FANTASY_AI_API_KEY="${FANTASY_AI_API_KEY:-}" \
FANTASY_AI_MODEL="$FANTASY_AI_MODEL" \
java -jar "$jar" >"$log_path" 2>&1 &
app_pid=$!

cleanup() {
  status=$?
  trap - EXIT INT TERM
  kill "$app_pid" >/dev/null 2>&1 || true
  wait "$app_pid" >/dev/null 2>&1 || true
  exit "$status"
}
trap cleanup EXIT INT TERM

base_url="http://127.0.0.1:${port}"
for _ in $(seq 1 60); do
  if curl --fail --silent "${base_url}/api/health" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
curl --fail --silent "${base_url}/api/health" >/dev/null

created=$(curl --fail --silent --request POST "${base_url}/api/v2/campaigns" \
  --header 'Content-Type: application/json' \
  --data '{"playerName":"AI연결검증","race":"human","seed":20260718}')
source_name=$(jq -er '.campaign.director.source' <<<"$created")
if [ "$source_name" != "LIVE_AI" ]; then
  reason=$(jq -r '.campaign.director.fallbackReason // "UNKNOWN"' <<<"$created")
  echo "[ai-qa] FAIL: provider did not produce a valid live response (fallback=${reason})." >&2
  exit 1
fi

spotlight_id=$(jq -er '.campaign.director.spotlightChoiceId' <<<"$created")
jq -e --arg id "$spotlight_id" \
  '.campaign.scene.choices | any(.[]; .id == $id and .locked == false)' <<<"$created" >/dev/null

echo "[ai-qa] PASS: external response accepted and spotlight choice validated by the rule engine."
