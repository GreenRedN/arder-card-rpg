#!/usr/bin/env bash
set -euo pipefail

BASE="http://localhost:8080/api"

echo "1) create session"
RESP=$(curl -sS -X POST "$BASE/sessions" -H "Content-Type: application/json" \
  -d '{"race":"human","name":"그린","nameless":false,"seed":12345}')
echo "$RESP" | python - <<'PY'
import json,sys
d=json.load(sys.stdin)
print("sessionId =", d["sessionId"])
PY

SID=$(echo "$RESP" | python - <<'PY'
import json,sys
d=json.load(sys.stdin)
print(d["sessionId"])
PY
)

echo
echo "2) next"
NEXT=$(curl -sS -X POST "$BASE/sessions/$SID/next")
echo "$NEXT" | python - <<'PY'
import json,sys
d=json.load(sys.stdin)
print("day =", d.get("day"), "needsChoice =", d.get("needsChoice"))
if d.get("needsChoice"):
    card=d.get("eventCard") or {}
    print("card =", card.get("id"), card.get("title"))
    for c in (card.get("choices") or []):
        print(" -", c.get("id"), ":", c.get("text"))
PY

echo
echo "3) choose YES (if neededChoice=true, otherwise this may return 409)"
curl -sS -X POST "$BASE/sessions/$SID/choose" -H "Content-Type: application/json" -d '{"choiceId":"YES"}' || true

echo
echo "4) state"
curl -sS "$BASE/sessions/$SID/state" | python -m json.tool
