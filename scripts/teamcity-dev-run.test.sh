#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT_SOURCE="$SCRIPT_DIR/teamcity-dev-run.kts"
SCRIPT_WRAPPER="$SCRIPT_DIR/teamcity-dev-run.sh"
KOTLINC_FOR_TEST="${KOTLINC_FOR_TEST:-kotlinc}"

if [[ ! -f "$SCRIPT_SOURCE" ]]; then
    echo "Expected $SCRIPT_SOURCE to exist" >&2
    exit 1
fi
if [[ ! -x "$SCRIPT_WRAPPER" ]]; then
    echo "Expected $SCRIPT_WRAPPER to be executable" >&2
    exit 1
fi

TEST_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/kotlin-teamcity-dev-run-test.XXXXXX")"
trap 'rm -rf "$TEST_ROOT"' EXIT

PROJECT_ROOT="$TEST_ROOT/project"
mkdir -p "$PROJECT_ROOT/scripts" "$TEST_ROOT/bin"
cp "$SCRIPT_SOURCE" "$PROJECT_ROOT/scripts/teamcity-dev-run.kts"
cp "$SCRIPT_WRAPPER" "$PROJECT_ROOT/scripts/teamcity-dev-run.sh"
SMART_RUN_COMMAND=(env "KOTLINC=$KOTLINC_FOR_TEST" "$PROJECT_ROOT/scripts/teamcity-dev-run.sh")

cat > "$PROJECT_ROOT/gradlew" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

if [[ "$*" != "inferAffectedDomains -Ptest.federation.enabled=true" ]]; then
    echo "Unexpected Gradle arguments: $*" >&2
    exit 1
fi

printf '%s' "${AFFECTED_DOMAINS:-}" > .test-federation.affected-domains.txt
EOF
chmod +x "$PROJECT_ROOT/gradlew"

cat > "$TEST_ROOT/bin/git" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

printf '%s\n' "$*" >> "$GIT_CALLS"

current_branch="${CURRENT_BRANCH:-user/topic}"
case "$*" in
    "symbolic-ref --quiet --short HEAD")
        if [[ "${DETACHED_HEAD:-}" == "1" ]]; then
            exit 1
        fi
        printf '%s\n' "$current_branch"
        ;;
    "rev-parse --verify HEAD")
        printf '%s\n' '0123456789abcdef0123456789abcdef01234567'
        ;;
    "config --get branch.$current_branch.pushRemote" | "config --get remote.pushDefault")
        exit 1
        ;;
    "config --get branch.$current_branch.remote")
        printf '%s\n' 'company'
        ;;
    "remote get-url company")
        printf '%s\n' 'ssh://git.example/kotlin.git'
        ;;
    "remote get-url origin")
        printf '%s\n' 'ssh://git.example/kotlin.git'
        ;;
    "push company HEAD:refs/heads/"* | "push origin HEAD:refs/heads/"*)
        printf '%s\n' 'push' >> "$OPERATIONS"
        if [[ "${PUSH_FAIL:-}" == "1" ]]; then
            echo "mocked push failure" >&2
            exit 1
        fi
        ;;
    *)
        echo "Unexpected Git arguments: $*" >&2
        exit 1
        ;;
esac
EOF
chmod +x "$TEST_ROOT/bin/git"

cat > "$TEST_ROOT/bin/teamcity" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

printf '%s|%s\n' "${TEAMCITY_URL:-}" "$*" >> "$TEAMCITY_CALLS"
printf '%s\n' 'teamcity' >> "$OPERATIONS"

if [[ "${DETACHED_HEAD:-}" == "1" ]]; then
    expected_branch="dev/detached-0123456789ab"
elif [[ "${CURRENT_BRANCH:-user/topic}" == dev/* ]]; then
    expected_branch="${CURRENT_BRANCH:-user/topic}"
else
    expected_branch="dev/${CURRENT_BRANCH:-user/topic}"
fi

case "$*" in
    *"run start Kotlin_KotlinDev_Domain_Frontend --branch $expected_branch --revision 0123456789abcdef0123456789abcdef01234567 --no-push --json"*)
        printf '%s\n' '{"buildType":{"id":"Kotlin_KotlinDev_Domain_Frontend"},"id":101,"state":"queued","status":"UNKNOWN","webUrl":"https://teamcity/build/101"}'
        ;;
    *"run start Kotlin_KotlinDev_Domain_Js --branch $expected_branch --revision 0123456789abcdef0123456789abcdef01234567 --no-push --json"*)
        printf '%s\n' '{"buildType":{"id":"Kotlin_KotlinDev_Domain_Js"},"id":102,"state":"queued","status":"UNKNOWN","webUrl":"https://teamcity/build/102"}'
        ;;
    *"run watch 101 --json"*)
        if [[ "${SLOW_WATCH:-}" == "1" ]]; then
            printf '%s\n' 101 >> "$WATCH_STARTED"
            sleep 2
        fi
        printf '%s\n' '{"id":101,"state":"finished","status":"SUCCESS","webUrl":"https://teamcity/build/101"}'
        ;;
    *"run watch 102 --json"*)
        if [[ "${SLOW_WATCH:-}" == "1" ]]; then
            printf '%s\n' 102 >> "$WATCH_STARTED"
            sleep 2
        fi
        if [[ "${FAIL_JOB:-}" == "Kotlin_KotlinDev_Domain_Js" ]]; then
            printf '%s\n' '{"id":102,"state":"finished","status":"FAILURE","webUrl":"https://teamcity/build/102"}'
            exit 1
        fi
        printf '%s\n' '{"id":102,"state":"finished","status":"SUCCESS","webUrl":"https://teamcity/build/102"}'
        ;;
    *"run cancel 101 --yes --comment Cancelled by teamcity-dev-run"* | *"run cancel 102 --yes --comment Cancelled by teamcity-dev-run"*)
        if [[ "${CANCEL_COMMAND_FAIL:-}" == "1" ]]; then
            echo "mocked run cancel failure" >&2
            exit 1
        fi
        ;;
    *"api /app/rest/builds/id:101?fields=id,state --raw"*)
        printf '%s\n' '{"id":101,"state":"running"}'
        ;;
    *"api /app/rest/builds/id:102?fields=id,state --raw"*)
        printf '%s\n' '{"id":102,"state":"queued"}'
        ;;
    *"api /app/rest/builds/id:101 -X POST --input - --silent"* | *"api /app/rest/buildQueue/id:102 -X POST --input - --silent"*)
        IFS= read -r cancel_request
        printf '%s|%s\n' "$*" "$cancel_request" >> "$CANCEL_REQUESTS"
        ;;
    *)
        printf '\033[2JUnexpected TeamCity arguments: %s\n' "$*" >&2
        exit 1
        ;;
esac
EOF
chmod +x "$TEST_ROOT/bin/teamcity"

export PATH="$TEST_ROOT/bin:$PATH"
export TEAMCITY_CALLS="$TEST_ROOT/teamcity-calls.txt"
export WATCH_STARTED="$TEST_ROOT/watch-started.txt"
export CANCEL_REQUESTS="$TEST_ROOT/cancel-requests.txt"
export GIT_CALLS="$TEST_ROOT/git-calls.txt"
export OPERATIONS="$TEST_ROOT/operations.txt"
cd "$TEST_ROOT"

fail() {
    echo "$1" >&2
    exit 1
}

assert_calls_contain() {
    local expected="$1"
    [[ -f "$TEAMCITY_CALLS" ]] || fail "TeamCity was not called"
    [[ "$(<"$TEAMCITY_CALLS")" == *"$expected"* ]] || fail "Missing TeamCity call: $expected"
}

rm -f "$TEAMCITY_CALLS" "$GIT_CALLS" "$OPERATIONS"
output="$(AFFECTED_DOMAINS=$'Frontend\nJs\n' "${SMART_RUN_COMMAND[@]}" 2>&1)"
assert_calls_contain "https://buildserver.labs.intellij.net|--no-input --no-color run start Kotlin_KotlinDev_Domain_Frontend --branch dev/user/topic --revision 0123456789abcdef0123456789abcdef01234567 --no-push --json"
assert_calls_contain "https://buildserver.labs.intellij.net|--no-input --no-color run start Kotlin_KotlinDev_Domain_Js --branch dev/user/topic --revision 0123456789abcdef0123456789abcdef01234567 --no-push --json"
assert_calls_contain "https://buildserver.labs.intellij.net|--no-input --no-color run watch 101 --json"
assert_calls_contain "https://buildserver.labs.intellij.net|--no-input --no-color run watch 102 --json"
[[ "$(<"$GIT_CALLS")" == *"push company HEAD:refs/heads/dev/user/topic"* ]] || fail "Expected HEAD to be pushed to the dev branch"
[[ "$(head -n 1 "$OPERATIONS")" == "push" ]] || fail "Expected HEAD to be pushed before TeamCity was called"
[[ "$(wc -l < "$TEAMCITY_CALLS" | tr -d ' ')" == "4" ]] || fail "Expected exactly four TeamCity calls"
[[ "$output" == *"Domain"*"Build ID"*"Status"*"URL"* ]] || fail "Expected a build status table"
[[ "$output" == *"Frontend"*"101"*"SUCCESS"*"https://teamcity/build/101"* ]] || fail "Expected the Frontend result in the table"
[[ "$output" == *"Js"*"102"*"SUCCESS"*"https://teamcity/build/102"* ]] || fail "Expected the Js result in the table"
[[ "$output" != *$'\033'* ]] || fail "Expected output without terminal control sequences"

rm -f "$TEAMCITY_CALLS" "$GIT_CALLS" "$OPERATIONS"
CURRENT_BRANCH='dev/already-prefixed' AFFECTED_DOMAINS=$'Frontend\n' "${SMART_RUN_COMMAND[@]}" >/dev/null
[[ "$(<"$GIT_CALLS")" == *"push company HEAD:refs/heads/dev/already-prefixed"* ]] || \
    fail "Expected an existing dev prefix to be preserved"

rm -f "$TEAMCITY_CALLS" "$GIT_CALLS" "$OPERATIONS"
DETACHED_HEAD=1 AFFECTED_DOMAINS=$'Frontend\n' "${SMART_RUN_COMMAND[@]}" >/dev/null
[[ "$(<"$GIT_CALLS")" == *"push origin HEAD:refs/heads/dev/detached-0123456789ab"* ]] || \
    fail "Expected a detached HEAD to use a revision-based dev branch"

rm -f "$TEAMCITY_CALLS"
AFFECTED_DOMAINS='' "${SMART_RUN_COMMAND[@]}"
[[ ! -s "$TEAMCITY_CALLS" ]] || fail "Expected no TeamCity calls when no domains are affected"

rm -f "$TEAMCITY_CALLS" "$GIT_CALLS" "$OPERATIONS"
if AFFECTED_DOMAINS=$'Frontend\n' PUSH_FAIL=1 "${SMART_RUN_COMMAND[@]}"; then
    fail "Expected a failed Git push to fail teamcity-dev-run.kts"
fi
[[ ! -s "$TEAMCITY_CALLS" ]] || fail "Expected no TeamCity calls when the Git push fails"

rm -f "$TEAMCITY_CALLS"
if output="$(AFFECTED_DOMAINS=$'Frontend\nJs\n' FAIL_JOB='Kotlin_KotlinDev_Domain_Js' "${SMART_RUN_COMMAND[@]}" 2>&1)"; then
    fail "Expected a failed TeamCity build to fail teamcity-dev-run.kts"
fi
[[ "$output" == *"Js"*"102"*"FAILURE"* ]] || fail "Expected the failed build in the table"
[[ "$(wc -l < "$TEAMCITY_CALLS" | tr -d ' ')" == "4" ]] || fail "Expected all builds to be launched and watched despite one failure"

rm -f "$TEAMCITY_CALLS" "$WATCH_STARTED" "$CANCEL_REQUESTS"
set -m
AFFECTED_DOMAINS=$'Frontend\nJs\n' SLOW_WATCH=1 CANCEL_COMMAND_FAIL=1 \
    "${SMART_RUN_COMMAND[@]}" > "$TEST_ROOT/cancel-output.txt" 2>&1 &
script_pid=$!
set +m
for ((attempt = 0; attempt < 50; attempt++)); do
    if [[ -f "$WATCH_STARTED" && "$(wc -l < "$WATCH_STARTED" | tr -d ' ')" == "2" ]]; then
        break
    fi
    sleep 0.1
done
[[ -f "$WATCH_STARTED" && "$(wc -l < "$WATCH_STARTED" | tr -d ' ')" == "2" ]] || fail "Timed out waiting for mocked build watchers"
kill -INT -- "-$script_pid"
cancel_status=0
wait "$script_pid" || cancel_status=$?
[[ "$cancel_status" == "130" ]] || fail "Expected cancellation to exit with status 130, got $cancel_status"
assert_calls_contain "https://buildserver.labs.intellij.net|--no-input --no-color run cancel 101 --yes --comment Cancelled by teamcity-dev-run"
assert_calls_contain "https://buildserver.labs.intellij.net|--no-input --no-color run cancel 102 --yes --comment Cancelled by teamcity-dev-run"
assert_calls_contain "https://buildserver.labs.intellij.net|--no-input --no-color api /app/rest/builds/id:101?fields=id,state --raw"
assert_calls_contain "https://buildserver.labs.intellij.net|--no-input --no-color api /app/rest/builds/id:102?fields=id,state --raw"
assert_calls_contain "https://buildserver.labs.intellij.net|--no-input --no-color api /app/rest/builds/id:101 -X POST --input - --silent"
assert_calls_contain "https://buildserver.labs.intellij.net|--no-input --no-color api /app/rest/buildQueue/id:102 -X POST --input - --silent"
[[ "$(<"$CANCEL_REQUESTS")" == *'{"comment":"Cancelled by teamcity-dev-run","readdIntoQueue":false}'* ]] || \
    fail "Expected REST cancellation requests with a cancellation comment"
[[ "$(<"$TEST_ROOT/cancel-output.txt")" != *"Warning: Could not cancel"* ]] || \
    fail "Expected the REST fallback to cancel both builds"

rm -f "$TEAMCITY_CALLS"
if AFFECTED_DOMAINS=$'Frontend\ninvalid-domain\n' "${SMART_RUN_COMMAND[@]}"; then
    fail "Expected an invalid domain to fail teamcity-dev-run.kts"
fi
[[ ! -s "$TEAMCITY_CALLS" ]] || fail "Expected invalid domains to be rejected before launching builds"

echo "teamcity-dev-run.kts tests passed"