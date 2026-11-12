#!/usr/bin/env bash
# Checks the "spendly" Keycloak realm imported by infra/docker-compose.yml:
# for every demo user, runs the real Authorization Code + PKCE (S256) flow of the
# "spendly-web" client (the password grant is disabled on purpose), then checks the
# access token: subject (fixed id), realm roles and "spendly-api" audience.
#
# Usage: infra/scripts/check-keycloak-realm.sh   (Keycloak must be up; requires curl, jq, openssl)
# Base URL overridable with KEYCLOAK_URL (default http://localhost:8180).
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
REALM="spendly"
CLIENT_ID="spendly-web"
REDIRECT_URI="http://localhost:4200/"
OIDC="${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect"

# username:password:expected sub:expected application roles (sorted, comma separated)
USERS=(
  "alice:alice:00000000-0000-4000-8000-000000000001:EMPLOYEE"
  "bob:bob:00000000-0000-4000-8000-000000000002:EMPLOYEE"
  "marc:marc:00000000-0000-4000-8000-000000000003:EMPLOYEE,MANAGER"
  "claire:claire:00000000-0000-4000-8000-000000000004:ACCOUNTANT,EMPLOYEE"
  "sophie:sophie:00000000-0000-4000-8000-000000000005:ADMIN"
)
APP_ROLES='["EMPLOYEE","MANAGER","ACCOUNTANT","ADMIN"]'

b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

jwt_payload() {
  local part
  part="$(cut -d. -f2 <<<"$1" | tr '_-' '/+')"
  while ((${#part} % 4)); do part+="="; done
  openssl base64 -d -A <<<"$part"
}

token_for() {
  local username="$1" password="$2" jar verifier challenge login_page action location code
  jar="$(mktemp)"
  # shellcheck disable=SC2064
  trap "rm -f '$jar'" RETURN
  verifier="$(openssl rand 48 | b64url)"
  challenge="$(printf '%s' "$verifier" | openssl dgst -sha256 -binary | b64url)"

  # 1. Authorization request: Keycloak answers with its login form.
  login_page="$(curl -sf -c "$jar" -b "$jar" -G "${OIDC}/auth" \
    --data-urlencode "client_id=${CLIENT_ID}" \
    --data-urlencode "redirect_uri=${REDIRECT_URI}" \
    --data-urlencode "response_type=code" \
    --data-urlencode "scope=openid" \
    --data-urlencode "state=$(openssl rand -hex 8)" \
    --data-urlencode "code_challenge=${challenge}" \
    --data-urlencode "code_challenge_method=S256")"
  action="$(grep -o 'id="kc-form-login"[^>]*action="[^"]*"' <<<"$login_page" |
    sed -E 's/.*action="([^"]*)".*/\1/; s/&amp;/\&/g' || true)"
  [[ -n "$action" ]] || {
    echo "login form not found" >&2
    return 1
  }

  # 2. Submit the credentials: Keycloak redirects to the SPA with ?code=...
  location="$(curl -s -o /dev/null -c "$jar" -b "$jar" -w '%{redirect_url}' "$action" \
    --data-urlencode "username=${username}" \
    --data-urlencode "password=${password}" \
    --data-urlencode "credentialId=")"
  code="$(sed -nE 's/.*[?&]code=([^&]+).*/\1/p' <<<"$location")"
  [[ -n "$code" ]] || {
    echo "no authorization code (redirect: '${location}')" >&2
    return 1
  }

  # 3. Exchange the code, proving possession of the PKCE verifier.
  curl -sf "${OIDC}/token" \
    --data-urlencode "grant_type=authorization_code" \
    --data-urlencode "client_id=${CLIENT_ID}" \
    --data-urlencode "redirect_uri=${REDIRECT_URI}" \
    --data-urlencode "code=${code}" \
    --data-urlencode "code_verifier=${verifier}" | jq -er .access_token
}

failures=0
fail() {
  echo "KO  $1"
  failures=$((failures + 1))
}

for entry in "${USERS[@]}"; do
  IFS=: read -r username password expected_sub expected_roles <<<"$entry"
  if ! token="$(token_for "$username" "$password")"; then
    fail "${username}: unable to obtain a token"
    continue
  fi
  claims="$(jwt_payload "$token")"
  sub="$(jq -r .sub <<<"$claims")"
  roles="$(jq -r --argjson app "$APP_ROLES" \
    '[.realm_access.roles[] | select(. as $r | $app | index($r))] | sort | join(",")' <<<"$claims")"
  has_aud="$(jq -r '(.aud | if type == "array" then . else [.] end) | index("spendly-api") != null' <<<"$claims")"
  if [[ "$sub" == "$expected_sub" && "$roles" == "$expected_roles" && "$has_aud" == "true" ]]; then
    echo "OK  ${username}: sub=${sub} roles=${roles} aud contains spendly-api"
  else
    fail "${username}: sub=${sub} (expected ${expected_sub}), roles=${roles} (expected ${expected_roles}), spendly-api audience=${has_aud}"
  fi
done

# The public client must refuse an authorization request without PKCE...
no_pkce="$(curl -s -o /dev/null -w '%{redirect_url}' -G "${OIDC}/auth" \
  --data-urlencode "client_id=${CLIENT_ID}" --data-urlencode "redirect_uri=${REDIRECT_URI}" \
  --data-urlencode "response_type=code" --data-urlencode "scope=openid")"
if [[ "$no_pkce" == *"error=invalid_request"* ]]; then
  echo "OK  authorization request without PKCE is rejected"
else
  fail "authorization request without PKCE is not rejected (redirect: '${no_pkce}')"
fi

# ...and the password grant (Resource Owner Password Credentials).
password_grant="$(curl -s -o /dev/null -w '%{http_code}' "${OIDC}/token" \
  --data-urlencode "grant_type=password" --data-urlencode "client_id=${CLIENT_ID}" \
  --data-urlencode "username=alice" --data-urlencode "password=alice")"
if [[ "$password_grant" == "400" || "$password_grant" == "401" ]]; then
  echo "OK  password grant is disabled (HTTP ${password_grant})"
else
  fail "password grant answered HTTP ${password_grant}"
fi

if ((failures > 0)); then
  echo "${failures} check(s) failed" >&2
  exit 1
fi
echo "Realm ${REALM}: all checks passed"
