#!/usr/bin/env bash
set -euo pipefail
base=${BASE_URL:-http://localhost:8080}
user_password=${USER_PASSWORD:-user-local}
admin_password=${ADMIN_PASSWORD:-admin-local}
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
for attempt in $(seq 1 60); do
  if curl -fsS "$base/actuator/health" > "$work/health.json" 2>/dev/null; then break; fi
  if [ "$attempt" = 60 ]; then echo 'API did not become healthy' >&2; exit 1; fi
  sleep 2
done
curl -fsS "$base/v3/api-docs" > "$work/openapi.json"
curl -fsS "$base/swagger-ui/index.html" > /dev/null
code=$(curl -sS -o "$work/unauthorized.json" -w '%{http_code}' "$base/api/orders")
[ "$code" = 401 ]
code=$(curl -sS -u "user:$user_password" -H 'Content-Type: application/json' \
  -d '{"lines":[{"productId":1,"quantity":2}]}' \
  -o "$work/order.json" -w '%{http_code}' "$base/api/orders")
[ "$code" = 201 ]
id=$(python3 -c 'import json,sys; o=json.load(open(sys.argv[1])); assert o["status"]=="NEW" and o["total"]==498; print(o["id"])' "$work/order.json")
code=$(curl -sS -u "user:$user_password" -X PATCH -H 'Content-Type: application/json' \
  -d '{"status":"PAID"}' -o /dev/null -w '%{http_code}' "$base/api/orders/$id/status")
[ "$code" = 403 ]
code=$(curl -sS -u "admin:$admin_password" -X PATCH -H 'Content-Type: application/json' \
  -d '{"status":"SHIPPED"}' -o "$work/conflict.json" -w '%{http_code}' "$base/api/orders/$id/status")
[ "$code" = 409 ]
for next in PAID SHIPPED CANCELLED; do
  curl -fsS -u "admin:$admin_password" -X PATCH -H 'Content-Type: application/json' \
    -d "{\"status\":\"$next\"}" "$base/api/orders/$id/status" > "$work/status.json"
  python3 -c 'import json,sys; assert json.load(open(sys.argv[1]))["status"]==sys.argv[2]' "$work/status.json" "$next"
done
curl -fsS -u "user:$user_password" "$base/api/orders?status=CANCELLED&size=100" > "$work/page.json"
python3 - "$work" "$id" <<'PY'
import json, pathlib, sys
p = pathlib.Path(sys.argv[1])
read = lambda name: json.loads((p / name).read_text())
assert read('health.json')['status'] == 'UP'
assert '/api/orders' in read('openapi.json')['paths']
assert read('unauthorized.json')['status'] == 401
assert read('conflict.json')['status'] == 409
assert any(o['id'] == int(sys.argv[2]) for o in read('page.json')['content'])
PY
printf 'Smoke PASS: health, OpenAPI, Swagger UI, 401, 403, 409, order %s lifecycle and filtering\n' "$id"
