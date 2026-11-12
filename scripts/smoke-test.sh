#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://localhost:8080}"
admin_email="${BOOTSTRAP_ADMIN_EMAIL:-admin@example.com}"
admin_password="${BOOTSTRAP_ADMIN_PASSWORD:-Admin123!ChangeMe}"
customer_email="customer-$(date +%s)@example.com"
sku="DEMO-$(date +%s)"
sku_second="${sku}-B"

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
product_second="$(
  curl -fsS -X POST "${base_url}/api/v1/products" \
    -H "Authorization: Bearer ${admin_token}" \
    -H 'Content-Type: application/json' \
    -d "{\"sku\":\"${sku_second}\",\"name\":\"Demo Product B\",\"description\":\"Smoke test product B\",\"priceCents\":1500,\"currency\":\"CNY\"}"
)"
product_second_id="$(printf '%s' "${product_second}" | json_value id)"

curl -fsS -X POST "${base_url}/api/v1/inventory/stocks" \
  -H "Authorization: Bearer ${admin_token}" \
  -H 'Content-Type: application/json' \
  -d "{\"productId\":\"${product_id}\",\"availableStock\":100}" >/dev/null
curl -fsS -X POST "${base_url}/api/v1/inventory/stocks" \
  -H "Authorization: Bearer ${admin_token}" \
  -H 'Content-Type: application/json' \
  -d "{\"productId\":\"${product_second_id}\",\"availableStock\":100}" >/dev/null

customer_token="$(
  curl -fsS -X POST "${base_url}/api/v1/auth/register" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"${customer_email}\",\"password\":\"Customer123!\",\"displayName\":\"Smoke Customer\"}" |
    json_value accessToken
)"

curl -fsS -X POST "${base_url}/api/v1/cart/items" \
  -H "Authorization: Bearer ${customer_token}" \
  -H 'Content-Type: application/json' \
  -d "{\"productId\":\"${product_id}\",\"quantity\":2}" >/dev/null
curl -fsS -X POST "${base_url}/api/v1/cart/items" \
  -H "Authorization: Bearer ${customer_token}" \
  -H 'Content-Type: application/json' \
  -d "{\"productId\":\"${product_second_id}\",\"quantity\":1}" >/dev/null

updated_cart="$(
  curl -fsS -X PUT "${base_url}/api/v1/cart/items/${product_id}" \
    -H "Authorization: Bearer ${customer_token}" \
    -H 'Content-Type: application/json' \
  -d '{"quantity":2}'
)"
printf '%s' "${updated_cart}" |
  python3 -c 'import json,sys; data=json.load(sys.stdin); assert data["totalQuantity"] == 3; assert {item["productId"] for item in data["items"]} == set(sys.argv[1:])' "${product_id}" "${product_second_id}"

address="$(
  curl -fsS -X POST "${base_url}/api/v1/addresses" \
    -H "Authorization: Bearer ${customer_token}" \
    -H 'Content-Type: application/json' \
    -d '{"recipientName":"Smoke Customer","recipientPhone":"13800000000","region":"上海市 浦东新区","detail":"测试路 1 号","defaultAddress":true}'
)"
address_id="$(printf '%s' "${address}" | json_value id)"

order="$(
  curl -fsS -X POST "${base_url}/api/v1/orders" \
    -H "Authorization: Bearer ${customer_token}" \
    -H 'Content-Type: application/json' \
    -d "{\"items\":[{\"productId\":\"${product_id}\",\"quantity\":2},{\"productId\":\"${product_second_id}\",\"quantity\":1}],\"addressId\":\"${address_id}\"}"
)"
order_id="$(printf '%s' "${order}" | json_value id)"

curl -fsS "${base_url}/api/v1/orders/${order_id}" \
  -H "Authorization: Bearer ${customer_token}" >/dev/null

payment="$(
  curl -fsS -X POST "${base_url}/api/v1/payments" \
    -H "Authorization: Bearer ${customer_token}" \
    -H 'Content-Type: application/json' \
    -d "{\"orderId\":\"${order_id}\",\"channel\":\"MOCK\"}"
)"
payment_no="$(printf '%s' "${payment}" | json_value paymentNo)"

curl -fsS -X POST "${base_url}/api/v1/payments/${payment_no}/mock-pay" \
  -H "Authorization: Bearer ${customer_token}" >/dev/null

paid_order="$(
  curl -fsS "${base_url}/api/v1/orders/${order_id}" \
    -H "Authorization: Bearer ${customer_token}"
)"
printf '%s' "${paid_order}" |
  python3 -c 'import json,sys; data=json.load(sys.stdin); assert len(data["items"]) == 2; assert data["shippingAddress"]["recipientName"] == "Smoke Customer"'
order_status="$(printf '%s' "${paid_order}" | json_value status)"
[ "${order_status}" = "PAID" ] || {
  printf 'Smoke test failed: expected order %s to be PAID, got %s\n' "${order_id}" "${order_status}" >&2
  exit 1
}

cart_after_checkout="$(
  curl -fsS "${base_url}/api/v1/cart" \
    -H "Authorization: Bearer ${customer_token}"
)"
printf '%s' "${cart_after_checkout}" |
  python3 -c 'import json,sys; assert json.load(sys.stdin)["items"] == []'

my_orders="$(
  curl -fsS "${base_url}/api/v1/orders?status=PAID&page=0&size=20" \
    -H "Authorization: Bearer ${customer_token}"
)"
printf '%s' "${my_orders}" |
  python3 -c 'import json,sys; data=json.load(sys.stdin); assert data["page"]["totalElements"] >= 1; assert any(item["id"] == sys.argv[1] for item in data["content"])' "${order_id}"

other_customer_token="$(
  curl -fsS -X POST "${base_url}/api/v1/auth/register" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"other-${customer_email}\",\"password\":\"Customer123!\",\"displayName\":\"Other Customer\"}" |
    json_value accessToken
)"
other_customer_orders="$(
  curl -fsS "${base_url}/api/v1/orders" \
    -H "Authorization: Bearer ${other_customer_token}"
)"
printf '%s' "${other_customer_orders}" |
  python3 -c 'import json,sys; data=json.load(sys.stdin); assert all(item["id"] != sys.argv[1] for item in data["content"])' "${order_id}"

other_customer_cart="$(
  curl -fsS "${base_url}/api/v1/cart" \
    -H "Authorization: Bearer ${other_customer_token}"
)"
printf '%s' "${other_customer_cart}" |
  python3 -c 'import json,sys; data=json.load(sys.stdin); assert data["items"] == []'

admin_orders="$(
  curl -fsS "${base_url}/api/v1/admin/orders?keyword=${order_id}" \
    -H "Authorization: Bearer ${admin_token}"
)"
printf '%s' "${admin_orders}" |
  python3 -c 'import json,sys; data=json.load(sys.stdin); assert data["page"]["totalElements"] >= 1; assert any(item["id"] == sys.argv[1] for item in data["content"])' "${order_id}"

admin_payments="$(
  curl -fsS "${base_url}/api/v1/admin/payments?keyword=${payment_no}" \
    -H "Authorization: Bearer ${admin_token}"
)"
printf '%s' "${admin_payments}" |
  python3 -c 'import json,sys; data=json.load(sys.stdin); assert data["page"]["totalElements"] >= 1; assert any(item["paymentNo"] == sys.argv[1] for item in data["content"])' "${payment_no}"

customer_admin_status="$(
  curl -sS -o /dev/null -w '%{http_code}' \
    "${base_url}/api/v1/admin/orders" \
    -H "Authorization: Bearer ${customer_token}"
)"
[ "${customer_admin_status}" = "403" ] || {
  printf 'Smoke test failed: customer admin request expected 403, got %s\n' \
    "${customer_admin_status}" >&2
  exit 1
}

cancel_order="$(
  curl -fsS -X POST "${base_url}/api/v1/orders" \
    -H "Authorization: Bearer ${customer_token}" \
    -H 'Content-Type: application/json' \
    -d "{\"items\":[{\"productId\":\"${product_id}\",\"quantity\":3}],\"addressId\":\"${address_id}\"}"
)"
cancel_order_id="$(printf '%s' "${cancel_order}" | json_value id)"
stock_before_cancel="$(
  curl -fsS "${base_url}/api/v1/inventory/stocks/${product_id}" \
    -H "Authorization: Bearer ${customer_token}"
)"
available_before_cancel="$(printf '%s' "${stock_before_cancel}" | json_value availableStock)"

cancelled_order="$(
  curl -fsS -X POST "${base_url}/api/v1/orders/${cancel_order_id}/cancel" \
    -H "Authorization: Bearer ${customer_token}"
)"
cancelled_status="$(printf '%s' "${cancelled_order}" | json_value status)"
cancelled_reason="$(printf '%s' "${cancelled_order}" | json_value cancellationReason)"
stock_released="$(printf '%s' "${cancelled_order}" | json_value stockReleased)"
[ "${cancelled_status}" = "CANCELLED" ] &&
  [ "${cancelled_reason}" = "CUSTOMER_REQUEST" ] &&
  [ "${stock_released}" = "True" ] || {
  printf 'Smoke test failed: order %s was not cancelled with inventory released\n' \
    "${cancel_order_id}" >&2
  exit 1
}

stock_after_cancel="$(
  curl -fsS "${base_url}/api/v1/inventory/stocks/${product_id}" \
    -H "Authorization: Bearer ${customer_token}"
)"
available_after_cancel="$(printf '%s' "${stock_after_cancel}" | json_value availableStock)"
[ "${available_after_cancel}" -eq $((available_before_cancel + 3)) ] || {
  printf 'Smoke test failed: cancelled order inventory was not restored\n' >&2
  exit 1
}

if [ "${CHECK_ORDER_EXPIRY:-false}" = "true" ]; then
  expiring_order="$(
    curl -fsS -X POST "${base_url}/api/v1/orders" \
      -H "Authorization: Bearer ${customer_token}" \
      -H 'Content-Type: application/json' \
      -d "{\"items\":[{\"productId\":\"${product_id}\",\"quantity\":1}],\"addressId\":\"${address_id}\"}"
  )"
  expiring_order_id="$(printf '%s' "${expiring_order}" | json_value id)"
  sleep "${EXPIRY_WAIT_SECONDS:-7}"
  expired_order="$(
    curl -fsS "${base_url}/api/v1/orders/${expiring_order_id}" \
      -H "Authorization: Bearer ${customer_token}"
  )"
  expired_status="$(printf '%s' "${expired_order}" | json_value status)"
  expired_reason="$(printf '%s' "${expired_order}" | json_value cancellationReason)"
  expired_stock_released="$(printf '%s' "${expired_order}" | json_value stockReleased)"
  [ "${expired_status}" = "CANCELLED" ] &&
    [ "${expired_reason}" = "PAYMENT_TIMEOUT" ] &&
    [ "${expired_stock_released}" = "True" ] || {
    printf 'Smoke test failed: order %s was not expired automatically\n' \
      "${expiring_order_id}" >&2
    exit 1
  }
fi

printf 'Smoke test passed: product=%s order=%s payment=%s status=%s\n' \
  "${product_id}" "${order_id}" "${payment_no}" "${order_status}"
