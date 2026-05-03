#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080/api/v1}"

request() {
  local method="$1"
  local path="$2"
  local body="${3:-}"

  if [ -n "$body" ]; then
    curl --fail --silent --show-error \
      -X "$method" \
      -H "Content-Type: application/json" \
      --data "$body" \
      "$API_BASE_URL$path"
  else
    curl --fail --silent --show-error \
      -X "$method" \
      "$API_BASE_URL$path"
  fi
}

extract_json_string() {
  local field="$1"
  sed -n "s/.*\"$field\":\"\([^\"]*\)\".*/\1/p"
}

print_step() {
  printf "\n==> %s\n" "$1"
}

print_step "Health check"
request GET "/health"
printf "\n"

print_step "Seed baseline data"
request POST "/dev/seed"
printf "\n"

print_step "Upload payment runbook"
request POST "/runbooks" '{
  "serviceName": "payment-service",
  "title": "Demo Payment p99 Latency Runbook",
  "content": "For payment-service p99_latency incidents, inspect recent deployments, payment gateway latency, database response time, connection pool saturation, and downstream processor health. If the payment-service is saturated, scale conservatively. If a recent deployment aligns with the incident start, prepare rollback after human approval."
}'
printf "\n"

print_step "Send p99 latency alert"
alert_response="$(request POST "/alerts" '{
  "serviceName": "payment-service",
  "metric": "p99_latency",
  "value": 1850,
  "threshold": 900,
  "severity": "SEV1",
  "region": "us-east-1",
  "fingerprint": "demo-payment-p99-latency-1",
  "rawPayload": "{\"source\":\"demo\",\"message\":\"payment p99 latency high\"}"
}')"
printf "%s\n" "$alert_response"
incident_id="$(printf "%s" "$alert_response" | extract_json_string "incidentId")"

if [ -z "$incident_id" ]; then
  echo "Could not extract incidentId from alert response" >&2
  exit 1
fi

print_step "Send duplicate p99 latency alert"
request POST "/alerts" '{
  "serviceName": "payment-service",
  "metric": "p99_latency",
  "value": 1900,
  "threshold": 900,
  "severity": "SEV1",
  "region": "us-east-1",
  "fingerprint": "demo-payment-p99-latency-2",
  "rawPayload": "{\"source\":\"demo\",\"message\":\"duplicate payment p99 latency high\"}"
}'
printf "\n"

print_step "List incidents"
request GET "/incidents"
printf "\n"

print_step "Analyze incident $incident_id"
request POST "/incidents/$incident_id/analyze"
printf "\n"

print_step "Approve remediation"
request POST "/incidents/$incident_id/approve" '{
  "approvedBy": "demo-operator",
  "actionType": "REMEDIATION"
}'
printf "\n"

print_step "Resolve incident"
request POST "/incidents/$incident_id/resolve"
printf "\n"

print_step "Print audit logs"
request GET "/audit-logs"
printf "\n"
