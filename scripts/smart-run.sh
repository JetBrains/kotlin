#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
AFFECTED_DOMAINS_FILE="$PROJECT_ROOT/.test-federation.affected-domains.txt"
TEAMCITY_SERVER_URL="https://buildserver.labs.intellij.net"
BUILD_CONFIGURATION_PREFIX="Kotlin_KotlinDev_Domain_"

json_value() {
    local json="$1"
    local key="$2"
    local pattern

    json="${json//$'\n'/ }"
    pattern="\"${key}\"[[:space:]]*:[[:space:]]*\"([^\"]*)\""
    if [[ "$json" =~ $pattern ]]; then
        printf '%s\n' "${BASH_REMATCH[1]}"
        return 0
    fi

    pattern="\"${key}\"[[:space:]]*:[[:space:]]*([0-9]+)"
    if [[ "$json" =~ $pattern ]]; then
        printf '%s\n' "${BASH_REMATCH[1]}"
        return 0
    fi

    return 1
}

print_status_table() {
    local domain_width=6
    local domain
    local index
    local separator

    for domain in "${domains[@]}"; do
        if [[ ${#domain} -gt $domain_width ]]; then
            domain_width=${#domain}
        fi
    done

    printf -v separator '%*s' "$domain_width" ''
    separator="${separator// /-}"

    printf '%-*s | %-10s | %-12s | %s\n' "$domain_width" "Domain" "Build ID" "Status" "URL"
    printf '%s-+-%s-+-%s-+-%s\n' "$separator" "----------" "------------" "---"
    for ((index = 0; index < ${#domains[@]}; index++)); do
        printf '%-*s | %-10s | %-12s | %s\n' \
            "$domain_width" "${domains[$index]}" "${build_ids[$index]}" "${build_statuses[$index]}" "${build_urls[$index]}"
    done
}

if ! command -v teamcity >/dev/null 2>&1; then
    echo "Error: The TeamCity CLI is not installed or is not available on PATH." >&2
    echo "Install it from https://github.com/JetBrains/teamcity-cli and authenticate with:" >&2
    echo "  teamcity auth login --server $TEAMCITY_SERVER_URL" >&2
    exit 1
fi

if [[ ! -x "$PROJECT_ROOT/gradlew" ]]; then
    echo "Error: Gradle wrapper not found at $PROJECT_ROOT/gradlew." >&2
    exit 1
fi

cd "$PROJECT_ROOT"
./gradlew inferAffectedDomains -Ptest.federation.enabled=true

if [[ ! -f "$AFFECTED_DOMAINS_FILE" ]]; then
    echo "Error: inferAffectedDomains did not create $AFFECTED_DOMAINS_FILE." >&2
    exit 1
fi

domains=()
while IFS= read -r domain || [[ -n "$domain" ]]; do
    [[ -z "$domain" ]] && continue
    if [[ ! "$domain" =~ ^[A-Za-z][A-Za-z0-9]*$ ]]; then
        echo "Error: inferAffectedDomains produced an invalid domain: $domain" >&2
        exit 1
    fi
    domains+=("$domain")
done < "$AFFECTED_DOMAINS_FILE"

if [[ ${#domains[@]} -eq 0 ]]; then
    echo "No affected domains found; no TeamCity builds were started."
    exit 0
fi

export TEAMCITY_URL="$TEAMCITY_SERVER_URL"

watch_directory="$(mktemp -d "${TMPDIR:-/tmp}/kotlin-smart-run.XXXXXX")"
watch_pids=()
build_ids=()
build_statuses=()
build_urls=()
build_finished=()

terminate_watchers() {
    local pid

    for pid in "${watch_pids[@]}"; do
        if [[ -n "$pid" ]]; then
            kill "$pid" >/dev/null 2>&1 || true
        fi
    done
}

cleanup() {
    local exit_status=$?

    trap - EXIT INT TERM
    terminate_watchers
    rm -rf "$watch_directory"
    exit "$exit_status"
}

cancel_builds_and_exit() {
    local exit_status="$1"
    local build_id
    local index

    trap - EXIT INT TERM
    terminate_watchers
    echo >&2
    echo "Cancelling unfinished TeamCity builds..." >&2
    for ((index = 0; index < ${#build_ids[@]}; index++)); do
        build_id="${build_ids[$index]}"
        if [[ "$build_id" != "-" && "${build_finished[$index]}" == "0" ]]; then
            if teamcity --no-input --no-color run cancel "$build_id" --yes --comment "Cancelled by smart-run.sh" >/dev/null 2>&1; then
                echo "  Cancelled ${domains[$index]} build $build_id." >&2
            else
                echo "  Warning: Could not cancel ${domains[$index]} build $build_id." >&2
            fi
        fi
    done
    rm -rf "$watch_directory"
    exit "$exit_status"
}
trap cleanup EXIT
trap 'cancel_builds_and_exit 130' INT
trap 'cancel_builds_and_exit 143' TERM

echo "Starting ${#domains[@]} affected domain build(s) on $TEAMCITY_SERVER_URL."
status=0
for domain in "${domains[@]}"; do
    build_configuration="$BUILD_CONFIGURATION_PREFIX$domain"
    if start_result="$(teamcity --no-input --no-color run start "$build_configuration" --branch @this --json)"; then
        if build_id="$(json_value "$start_result" id)"; then
            build_ids+=("$build_id")
            build_statuses+=("QUEUED")
            build_urls+=("$(json_value "$start_result" webUrl || printf '%s\n' '-')")
            build_finished+=("0")
        else
            echo "Error: TeamCity did not return a build ID for $domain." >&2
            build_ids+=("-")
            build_statuses+=("START ERROR")
            build_urls+=("-")
            build_finished+=("1")
            status=1
        fi
    else
        echo "Error: Failed to start the $domain domain build." >&2
        build_ids+=("-")
        build_statuses+=("START ERROR")
        build_urls+=("-")
        build_finished+=("1")
        status=1
    fi
done

for ((index = 0; index < ${#domains[@]}; index++)); do
    build_id="${build_ids[$index]}"
    if [[ "$build_id" == "-" ]]; then
        watch_pids+=("")
        continue
    fi

    teamcity --no-input --no-color run watch "$build_id" --json \
        > "$watch_directory/$index.json" 2> "$watch_directory/$index.error" &
    watch_pids+=("$!")
    build_statuses[$index]="WATCHING"
done

echo
echo "Build status (updates are printed without redrawing the terminal):"
print_status_table

for ((index = 0; index < ${#domains[@]}; index++)); do
    pid="${watch_pids[$index]}"
    if [[ -z "$pid" ]]; then
        continue
    fi

    watch_status=0
    wait "$pid" || watch_status=$?
    watch_pids[$index]=""
    watch_result="$(<"$watch_directory/$index.json")"
    final_status="$(json_value "$watch_result" status || true)"
    final_url="$(json_value "$watch_result" webUrl || true)"

    if [[ -n "$final_status" && "$final_status" != "UNKNOWN" ]]; then
        build_statuses[$index]="$final_status"
    elif [[ "$watch_status" -eq 0 ]]; then
        build_statuses[$index]="FINISHED"
    else
        build_statuses[$index]="WATCH ERROR"
    fi
    if [[ -n "$final_url" ]]; then
        build_urls[$index]="$final_url"
    fi

    if [[ "$watch_status" -ne 0 || "${build_statuses[$index]}" != "SUCCESS" ]]; then
        status=1
    fi
    build_finished[$index]="1"
    printf 'Completed %-*s  %s\n' "${#domains[$index]}" "${domains[$index]}" "${build_statuses[$index]}"
done

echo
echo "Final build status:"
print_status_table

if [[ $status -ne 0 ]]; then
    echo "One or more affected domain builds failed." >&2
    exit "$status"
fi

echo "All affected domain builds completed successfully."