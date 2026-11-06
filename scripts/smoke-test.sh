#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://localhost:8080}"
admin_email="${BOOTSTRAP_ADMIN_EMAIL:-admin@example.com}"
admin_password="${BOOTSTRAP_ADMIN_PASSWORD:-Admin123!ChangeMe}"
customer_email="customer-$(date +%s)@example.com"
sku="DEMO-$(date +%s)"

json_value() {
  python3 -c 'import json,sys; print(json.load(sys.stdin)[sys.argv[1]])' "$1"
}

admin_token="$(
  curl -fsS -X POST "${base_url}/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"${admin_email}\",\"password\":\"${admin_password}\"}" |
    json_value accessToken
)"

product="$(
  curl -fsS -X POST "${base_url}/api/v1/products" \
    -H "Authorization: Bearer ${admin_token}" \
    -H 'Content-Type: application/json' \
    -d "{\"sku\":\"${sku}\",\"name\":\"Demo Product\",\"description\":\"Smoke test product\",\"priceCents\":9900,\"currency\":\"CNY\"}"
)"
product_id="$(printf '%s' "${product}" | json_value id)"

curl -fsS -X POST "${base_url}/api/v1/inventory/stocks" \
  -H "Authorization: Bearer ${admin_token}" \
  -H 'Content-Type: application/json' \
  -d "{\"productId\":\"${product_id}\",\"availableStock\":100}" >/dev/null

customer_token="$(
  curl -fsS -X POST "${base_url}/api/v1/auth/register" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"${customer_email}\",\"password\":\"Customer123!\",\"displayName\":\"Smoke Customer\"}" |
    json_value accessToken
)"

order="$(
  curl -fsS -X POST "${base_url}/api/v1/orders" \
    -H "Authorization: Bearer ${customer_token}" \
    -H 'Content-Type: application/json' \
    -d "{\"productId\":\"${product_id}\",\"quantity\":2}"
)"
order_id="$(printf '%s' "${order}" | json_value id)"

curl -fsS "${base_url}/api/v1/orders/${order_id}" \
  -H "Authorization: Bearer ${customer_token}" >/dev/null

printf 'Smoke test passed: product=%s order=%s\n' "${product_id}" "${order_id}"
