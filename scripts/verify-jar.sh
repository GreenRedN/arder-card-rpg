#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

for command_name in java curl jq; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "[jar-qa] Missing required command: $command_name" >&2
    exit 2
  fi
done

jar="fantasy-sim-api/target/fantasy-sim-api-2.1.0.jar"
if [ ! -f "$jar" ]; then
  ./mvnw --batch-mode -DskipTests package
fi

mkdir -p .runtime
port="${JAR_SMOKE_PORT:-18083}"
database_path="$PWD/.runtime/jar-smoke-db"
log_path="$PWD/.runtime/jar-smoke.log"
response_a="$PWD/.runtime/jar-smoke-choice-a.json"
response_b="$PWD/.runtime/jar-smoke-choice-b.json"
base_url="http://127.0.0.1:${port}"
app_pid=""
rm -f "${database_path}.mv.db" "${database_path}.trace.db" "$log_path" "$response_a" "$response_b"

stop_app() {
  if [ -n "$app_pid" ]; then
    kill "$app_pid" >/dev/null 2>&1 || true
    wait "$app_pid" >/dev/null 2>&1 || true
    app_pid=""
  fi
}

cleanup() {
  status=$?
  trap - EXIT INT TERM
  stop_app
  if [ "$status" -ne 0 ]; then
    tail -120 "$log_path" >&2 || true
  fi
  exit "$status"
}
trap cleanup EXIT INT TERM

start_app() {
  SERVER_PORT="$port" \
  DB_URL="jdbc:h2:file:${database_path};MODE=PostgreSQL;AUTO_SERVER=FALSE" \
  DB_USERNAME=sa \
  DB_PASSWORD= \
  FANTASY_AI_ENDPOINT= \
  FANTASY_AI_API_KEY= \
  FANTASY_AI_MODEL= \
  java -jar "$jar" >"$log_path" 2>&1 &
  app_pid=$!

  for _ in $(seq 1 60); do
    if curl --fail --silent "${base_url}/api/health" \
      | jq -e '.status == "ok" and .database == "up"' >/dev/null 2>&1; then
      return
    fi
    sleep 1
  done
  echo "[jar-qa] Application did not become ready." >&2
  exit 1
}

start_app
created=$(curl --fail --silent --request POST "${base_url}/api/v2/campaigns" \
  --header 'Content-Type: application/json' \
  --data '{"playerName":"JAR검증","race":"dwarf","seed":20260718}')
campaign_id=$(jq -er '.campaign.campaignId' <<<"$created")
access_token=$(jq -er '.accessToken' <<<"$created")
choice_id=$(jq -er '[.campaign.scene.choices[] | select(.locked == false)][0].id' <<<"$created")
jq -e '.campaign.director.source == "RULE_FALLBACK" and .campaign.turn == 0' <<<"$created" >/dev/null

curl --fail --silent --request POST "${base_url}/api/v2/campaigns/${campaign_id}/choices" \
  --header 'Content-Type: application/json' \
  --header "X-Campaign-Token: ${access_token}" \
  --data "{\"choiceId\":\"${choice_id}\",\"requestId\":\"jar-smoke-concurrent-1\"}" \
  --output "$response_a" &
choice_pid_a=$!
curl --fail --silent --request POST "${base_url}/api/v2/campaigns/${campaign_id}/choices" \
  --header 'Content-Type: application/json' \
  --header "X-Campaign-Token: ${access_token}" \
  --data "{\"choiceId\":\"${choice_id}\",\"requestId\":\"jar-smoke-concurrent-1\"}" \
  --output "$response_b" &
choice_pid_b=$!
wait "$choice_pid_a"
wait "$choice_pid_b"
jq -e '.campaign.turn == 1 and .campaign.version == 2' "$response_a" >/dev/null
jq -e '.campaign.turn == 1 and .campaign.version == 2' "$response_b" >/dev/null

stop_app
start_app
resumed=$(curl --fail --silent "${base_url}/api/v2/campaigns/${campaign_id}" \
  --header "X-Campaign-Token: ${access_token}")
jq -e '.accessToken == null and .campaign.turn == 1 and .campaign.version == 2' <<<"$resumed" >/dev/null

echo "[jar-qa] PASS: packaged JAR, DB-aware health, concurrent idempotency, and restart recovery."
