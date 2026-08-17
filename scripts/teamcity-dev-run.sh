#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
KOTLINC="${KOTLINC:-kotlinc}"

cd "$PROJECT_ROOT"
exec "$KOTLINC" -script "$SCRIPT_DIR/teamcity-dev-run.kts" "$@"
