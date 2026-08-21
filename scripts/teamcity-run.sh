#!/bin/bash
#
# teamcity-run.sh - Trigger a Kotlin Aggregate build on TeamCity.
#
# Usage:
#   scripts/teamcity-run.sh [OPTIONAL BRANCH]
#   The script can also run on Windows via bash bundled with `Git for Windows`.
#
# Description:
#   Starts the Aggregate build via KotlinDev_AggregateTrigger configuration on TeamCity.
#   Requires the TeamCity CLI 1.3.0 or newer(see the install hint printed if it is missing) and
#   authenticates via browser login on first use.
#
#   If OPTIONAL BRANCH is given, the build is triggered for that branch directly.
#   Otherwise, the script uses the current Git branch: it pushes any un-pushed
#   commits (setting an upstream if needed) and then triggers the build for it.
set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/.."

BRANCH="${1:-}"

if ! command -v teamcity >/dev/null 2>&1; then
    cat <<'EOF'
TeamCity CLI is not installed. Install it with:

macOS (Homebrew): brew install jetbrains/utils/teamcity
Linux: curl -fsSL https://jb.gg/tc/install | bash
Windows: winget install JetBrains.TeamCityCLI

Read more at: https://www.jetbrains.com/help/teamcity/teamcity-cli-get-started.html#install
EOF
    exit 1
fi

REQUIRED_TEAMCITY_VERSION="1.3.0"
TEAMCITY_VERSION="$(teamcity --version 2>/dev/null | grep -Eo 'version[[:space:]]+[0-9]+\.[0-9]+\.[0-9]+' | grep -Eo '[0-9]+\.[0-9]+\.[0-9]+' | head -n 1 || true)"
if [ -z "$TEAMCITY_VERSION" ]; then
    echo "Unable to determine the TeamCity CLI version. Please install version $REQUIRED_TEAMCITY_VERSION or newer."
    exit 1
fi
LOWEST_VERSION="$(printf '%s\n%s\n' "$REQUIRED_TEAMCITY_VERSION" "$TEAMCITY_VERSION" | sort -V | head -n 1)"
if [ "$LOWEST_VERSION" != "$REQUIRED_TEAMCITY_VERSION" ]; then
    echo "TeamCity CLI version $TEAMCITY_VERSION is too old. Please install version $REQUIRED_TEAMCITY_VERSION or newer."
    exit 1
fi

if ! teamcity auth status --json --no-input 2>/dev/null | grep -Eq '"status"[[:space:]]*:[[:space:]]*"authenticated"'; then
    echo "You are not authenticated with TeamCity, starting browser login..."
    teamcity auth login --server https://buildserver.labs.intellij.net/
fi

if [ -n "$BRANCH" ]; then
    echo "Triggering TeamCity run for branch '$BRANCH'..."
    teamcity run start Kotlin_KotlinDev_AggregateTrigger --branch "$BRANCH"
    exit 0
fi

if ! git symbolic-ref -q HEAD >/dev/null 2>&1; then
    cat <<'EOF'
You are in a detached HEAD state (not on any branch).
Please check out a branch before running this script.
EOF
    exit 1
fi

if git rev-parse --abbrev-ref --symbolic-full-name '@{u}' >/dev/null 2>&1; then
    if [ "$(git rev-list --count '@{u}..HEAD')" -gt 0 ]; then
        if [ "$(git rev-list --count 'HEAD..@{u}')" -gt 0 ]; then
            cat <<'EOF'
The current branch has diverged from its upstream and would require a force push.
Please force-push manually if that's intended, then re-run this script.
EOF
            exit 1
        fi
        echo "Pushing un-pushed commits on the current branch..."
        git push
    fi
else
    echo "Current branch has no upstream. Pushing to origin..."
    git push -u origin HEAD
fi

teamcity run start Kotlin_KotlinDev_AggregateTrigger --branch @this
