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

json_number_value() {
    local json="$1"
    local key="$2"
    local pattern

    json="${json//$'\n'/ }"
    pattern="\"${key}\"[[:space:]]*:[[:space:]]*([0-9]+)"
    if [[ "$json" =~ $pattern ]]; then
        printf '%s\n' "${BASH_REMATCH[1]}"
        return 0
    fi

    return 1
}

resolve_push_remote() {
    local current_branch="$1"
    local candidate
    local remote
    local remotes=()

    candidate=""
    if [[ -n "$current_branch" ]]; then
        candidate="$(git config --get "branch.$current_branch.pushRemote" || true)"
    fi
    if [[ -z "$candidate" ]]; then
        candidate="$(git config --get remote.pushDefault || true)"
    fi
    if [[ -z "$candidate" && -n "$current_branch" ]]; then
        candidate="$(git config --get "branch.$current_branch.remote" || true)"
    fi

    if [[ -n "$candidate" ]]; then
        if [[ "$candidate" == "." ]]; then
            echo "Error: The current branch is configured to push to the local repository." >&2
            return 1
        fi
        if ! git remote get-url "$candidate" >/dev/null 2>&1; then
            echo "Error: The configured Git push remote '$candidate' does not exist." >&2
            return 1
        fi
        printf '%s\n' "$candidate"
        return 0
    fi

    if git remote get-url origin >/dev/null 2>&1; then
        printf '%s\n' origin
        return 0
    fi

    while IFS= read -r remote; do
        [[ -n "$remote" ]] && remotes+=("$remote")
    done < <(git remote)
    if [[ ${#remotes[@]} -eq 1 ]]; then
        printf '%s\n' "${remotes[0]}"
        return 0
    fi

    echo "Error: Could not determine which Git remote should receive the smart branch." >&2
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

if ! command -v git >/dev/null 2>&1; then
    echo "Error: Git is not installed or is not available on PATH." >&2
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

if ! head_revision="$(git rev-parse --verify HEAD)"; then
    echo "Error: Could not determine the current Git HEAD." >&2
    exit 1
fi
current_branch="$(git symbolic-ref --quiet --short HEAD || true)"
if [[ -z "$current_branch" ]]; then
    teamcity_branch="smart/detached-${head_revision:0:12}"
elif [[ "$current_branch" == smart/* ]]; then
    teamcity_branch="$current_branch"
else
    teamcity_branch="smart/$current_branch"
fi
push_remote="$(resolve_push_remote "$current_branch")"

echo "Pushing HEAD $head_revision to $push_remote/$teamcity_branch."
if ! git push "$push_remote" "HEAD:refs/heads/$teamcity_branch"; then
    echo "Error: Could not push HEAD to $push_remote/$teamcity_branch; no TeamCity builds were started." >&2
    exit 1
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

cancel_build_via_api() {
    local build_id="$1"
    local error_file="$2"
    local build
    local endpoint
    local state

    if ! build="$(teamcity --no-input --no-color api "/app/rest/builds/id:$build_id?fields=id,state" --raw 2>> "$error_file")"; then
        return 1
    fi

    state="$(json_value "$build" state || true)"
    case "$state" in
        finished)
            return 0
            ;;
        queued)
            endpoint="/app/rest/buildQueue/id:$build_id"
            ;;
        running)
            endpoint="/app/rest/builds/id:$build_id"
            ;;
        *)
            printf 'Could not determine build %s state from TeamCity REST response.\n' "$build_id" >> "$error_file"
            return 1
            ;;
    esac

    printf '%s\n' '{"comment":"Cancelled by smart-run.sh","readdIntoQueue":false}' | \
        teamcity --no-input --no-color api "$endpoint" -X POST --input - --silent 2>> "$error_file"
}

print_cancel_errors() {
    local error_file="$1"
    local line

    while IFS= read -r line || [[ -n "$line" ]]; do
        printf '    %s\n' "$line" >&2
    done < "$error_file"
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
    local error_file
    local index

    trap - EXIT INT TERM
    terminate_watchers
    echo >&2
    echo "Cancelling unfinished TeamCity builds..." >&2
    for ((index = 0; index < ${#build_ids[@]}; index++)); do
        build_id="${build_ids[$index]}"
        if [[ "$build_id" != "-" && "${build_finished[$index]}" == "0" ]]; then
            error_file="$watch_directory/cancel-$index.error"
            if teamcity --no-input --no-color run cancel "$build_id" --yes --comment "Cancelled by smart-run.sh" \
                >/dev/null 2> "$error_file"; then
                echo "  Cancelled ${domains[$index]} build $build_id." >&2
            elif cancel_build_via_api "$build_id" "$error_file"; then
                echo "  Cancelled ${domains[$index]} build $build_id using the REST fallback." >&2
            else
                echo "  Warning: Could not cancel ${domains[$index]} build $build_id." >&2
                print_cancel_errors "$error_file"
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
    if start_result="$(teamcity --no-input --no-color run start "$build_configuration" \
        --branch "$teamcity_branch" --revision "$head_revision" --no-push --json)"; then
        if build_id="$(json_number_value "$start_result" id)"; then
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