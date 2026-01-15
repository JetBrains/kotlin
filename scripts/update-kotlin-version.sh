#!/bin/bash

# ==============================================================================
# Kotlin Version Update Helper
#
# Purpose: Regenerate BTA arguments, API dumps, and version constants 
#          when bumping the Kotlin version.
# Usage:   ./update_kotlin_version.sh <version>
# Example: ./update_kotlin_version.sh 2.4.0
# ==============================================================================

# Exit immediately if a command exits with a non-zero status
set -e

# Validate input
if [ -z "$1" ]; then
    echo "❌ Error: Missing Kotlin version parameter."
    echo "Usage: $0 <kotlin-version>"
    echo "Example: $0 2.4.0"
    exit 1
fi

KOTLIN_VERSION="$1"

# Resolve the project root assuming this script is in a subdirectory (e.g., scripts/)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/.."

# Navigate to project root to execute Gradle
cd "$PROJECT_ROOT" || exit

# Check for gradlew wrapper
if [ ! -f "./gradlew" ]; then
    echo "❌ Error: ./gradlew not found in $PROJECT_ROOT."
    exit 1
fi

echo "🚀 Starting regeneration tasks for Kotlin version: $KOTLIN_VERSION"

# Define the tasks to be executed
TASKS=(
    ":compiler:build-tools:kotlin-build-tools-impl:generateBtaArguments"
    ":compiler:build-tools:kotlin-build-tools-api:generateBtaArguments"
    ":compiler:build-tools:kotlin-build-tools-compat:generateBtaArguments"
    ":compiler:build-tools:kotlin-build-tools-api:apiDump"
    ":kotlin-gradle-plugin-api:generateKotlinVersionConstant"
    ":kotlin-gradle-plugin-api:apiDump"
)

# Execute Gradle tasks with provided properties
./gradlew "${TASKS[@]}" \
    -Pbuild.number="$KOTLIN_VERSION" \
    -PdeployVersion="$KOTLIN_VERSION"

echo "✅ Successfully regenerated artifacts for Kotlin $KOTLIN_VERSION."
