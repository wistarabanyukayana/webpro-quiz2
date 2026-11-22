#!/usr/bin/env bash
# smoke test against a running app
# usage: scripts/smoke.sh [base-url]
set -euo pipefail

BASE=${1:-http://localhost:8080}
RUN=$(date +%s)$RANDOM
USER=smoke$RUN
PASS='Smoke-Test-Pass-1!'
TODAY=$(TZ=Asia/Jakarta date +%F)
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
JAR=$WORK/cookies
BODY=$WORK/body
TOKEN=

fail() { echo "FAIL: $*" >&2; exit 1; }
ok() { echo "ok   $*"; }

get() {
    local status
    status=$(curl -s -o "$BODY" -w '%{http_code}' -b "$JAR" -c "$JAR" "$BASE$1")
    [[ $status == "${2:-200}" ]] || fail "GET $1 returned $status, expected ${2:-200}"
    local t
    t=$(grep -o 'name="_token" value="[^"]*"' "$BODY" | head -1 | cut -d'"' -f4 || true)
    [[ -n $t ]] && TOKEN=$t
    return 0
}

send() {
    local path=$1; shift
    local args=(--data-urlencode "_token=$TOKEN")
    for pair in "$@"; do args+=(--data-urlencode "$pair"); done
    local out
    out=$(curl -s -o /dev/null -w '%{http_code} %{redirect_url}' -b "$JAR" -c "$JAR" "${args[@]}" "$BASE$path")
    [[ ${out%% *} == 302 ]] || fail "POST $path $* returned ${out%% *}, expected 302"
    echo "${out#* }" | sed "s|^$BASE||"
}

has() { grep -q -- "$1" "$BODY" || fail "$2: missing '$1'"; }
hasnt() { ! grep -q -- "$1" "$BODY" || fail "$2: unexpected '$1'"; }

get /up; ok "health check"

get /register
[[ $(send /register username="$USER" full_name="Smoke Test" email="$USER@example.com" \
    password="$PASS" password_confirmation="$PASS") == /login ]] || fail "register did not lead to the login page"
get / 302; ok "register"

get /login
[[ $(send /login login="${USER^^}@EXAMPLE.COM" password="$PASS") == / ]] || fail "login by email failed"
get /; ok "login by email"

# Categories
send /categories name="Smoke $RUN" type=expense >/dev/null
get /categories
CAT=$(tr -d '\n' < "$BODY" | grep -o "Smoke $RUN</span>.\{0,600\}categories?edit=[0-9]*" | grep -o '[0-9]*$' || true)
[[ -n $CAT ]] || fail "new category not listed"; ok "create category $CAT"
send "/categories/$CAT" _method=PATCH name="Smoke $RUN renamed" type=expense >/dev/null
get /categories; has "Smoke $RUN renamed" "renamed category"; ok "rename category"

# Transactions
send /transactions type=expense category_id="$CAT" amount=12345 date="$TODAY" time=12:30 notes="smoke-$RUN" back=/expense >/dev/null
get "/expense?q=smoke-$RUN"
TX=$(grep -o 'action="/transactions/[0-9]*"' "$BODY" | head -1 | grep -o '[0-9]*' || true)
[[ -n $TX ]] || fail "new transaction not listed"; has "12.345" "transaction amount"; ok "create transaction $TX"
send "/transactions/$TX" _method=PATCH type=expense category_id="$CAT" amount=23456 date="$TODAY" time=12:30 \
    notes="smoke-$RUN edited" back=/expense >/dev/null
get "/expense?q=smoke-$RUN"; has "23.456" "edited amount"; has "smoke-$RUN edited" "edited notes"; ok "edit transaction"
get /; has "23.456" "dashboard list"; ok "dashboard"

# Recap and exports
MONTH=${TODAY:0:7}
get "/recap?month=$MONTH"; has "Smoke $RUN renamed" "recap category"; ok "recap"
get "/expense/export?q=smoke-$RUN"; has "smoke-$RUN edited" "expense CSV"; has "^date,time,type,category,amount,notes" "CSV header"
get /income/export; has "^date,time,type,category,amount,notes" "income CSV"
get "/recap/csv?month=$MONTH"; has ",23456," "recap CSV"
get "/recap/report?month=$MONTH"; has "Financial recap" "printable report"
get "/recap/pdf?month=$MONTH"; head -c 5 "$BODY" | grep -q '%PDF-' || fail "recap PDF is not a PDF"
ok "CSV, report and PDF exports"

# Settings
get /settings
send /settings _method=PATCH username="$USER" full_name="Smoke Renamed" email="$USER@example.com" initial_balance=1000 >/dev/null
get /settings; has 'value="Smoke Renamed"' "profile update"; hasnt 'role="alert"' "profile update"; ok "settings"

# Deletes
send "/transactions/$TX" _method=DELETE back=/expense >/dev/null
get "/expense?q=smoke-$RUN"; hasnt "action=\"/transactions/$TX\"" "deleted transaction"; ok "delete transaction"
send "/categories/$CAT" _method=DELETE >/dev/null
get /categories; hasnt "Smoke $RUN renamed" "deleted category"; ok "delete category"

send /logout >/dev/null
get / 302; ok "logout"
echo "smoke test passed against $BASE"
