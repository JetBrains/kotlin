#!/usr/bin/env bash
#
# Runs gradle-profiler with the Kotlin import models build actions on its classpath.
#
#   run-profiler.sh <project-dir> [gradle-profiler arguments...] [scenario names...]
#
# Defaults (override by passing the corresponding gradle-profiler argument explicitly):
#   --benchmark --warmups 3 --iterations 10 --gradle-version 9.7.0
#   --scenario-file import-models.scenarios --output-dir <project-dir>/../profiler-output
#
# Environment:
#   KOTLIN_REPO           Kotlin repository root (default: derived from this script's location)
#   GRADLE_PROFILER_HOME  gradle-profiler distribution with a `lib` directory (default: derived from `gradle-profiler` on PATH)
#   GRADLE_USER_HOME_DIR  passed as `--gradle-user-home`; defaults to the integration tests' test-kit cache when it exists
#                         (it already has the Gradle distributions and dependencies cached)
#
# Prerequisites: `./gradlew install :kotlin-gradle-plugin-integration-tests:testClasses` in the Kotlin repository.
set -euo pipefail

PROJECT_DIR=${1:?"profiled project directory is required (see generate-project.sh)"}
shift

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
KOTLIN_REPO=${KOTLIN_REPO:-$(cd "$SCRIPT_DIR/../../../../../../.." && pwd)}
IT_MODULE="$KOTLIN_REPO/libraries/tools/kotlin-gradle-plugin-integration-tests"

if [[ -z "${GRADLE_PROFILER_HOME:-}" ]]; then
    launcher=$(readlink -f "$(command -v gradle-profiler)")
    # Homebrew installs a wrapper in bin/ that execs libexec/bin/gradle-profiler
    GRADLE_PROFILER_HOME=$(dirname "$(dirname "$launcher")")
    if [[ ! -d "$GRADLE_PROFILER_HOME/lib" && -d "$GRADLE_PROFILER_HOME/libexec/lib" ]]; then
        GRADLE_PROFILER_HOME="$GRADLE_PROFILER_HOME/libexec"
    fi
fi
[[ -d "$GRADLE_PROFILER_HOME/lib" ]] || { echo "gradle-profiler lib directory not found under $GRADLE_PROFILER_HOME" >&2; exit 1; }

TEST_CLASSES="$IT_MODULE/build/classes/kotlin/test"
[[ -f "$TEST_CLASSES/org/jetbrains/kotlin/gradle/KotlinImportModelsAllProjectsBuildAction.class" ]] \
    || { echo "Build action classes not found; run ./gradlew :kotlin-gradle-plugin-integration-tests:testClasses" >&2; exit 1; }

# The published jar relocates protobuf, and the build action classes are compiled against the relocated packages.
IMPORT_MODELS_JAR=$(ls -t "${MAVEN_LOCAL:-$HOME/.m2/repository}"/org/jetbrains/kotlin/kotlin-import-models/*/kotlin-import-models-*.jar 2>/dev/null \
    | grep -v -- '-sources\|-javadoc' | head -1)
[[ -n "$IMPORT_MODELS_JAR" ]] || { echo "kotlin-import-models jar not found in Maven local; run ./gradlew install" >&2; exit 1; }

CP="$GRADLE_PROFILER_HOME/lib/*:$TEST_CLASSES:$IMPORT_MODELS_JAR"

ARGS=("$@")
has_arg() { local a; for a in "${ARGS[@]}"; do [[ "$a" == "$1" ]] && return 0; done; return 1; }

has_arg --benchmark || has_arg --profile || ARGS+=(--benchmark)
has_arg --warmups || ARGS+=(--warmups 3)
has_arg --iterations || ARGS+=(--iterations 10)
has_arg --gradle-version || ARGS+=(--gradle-version 9.7.0)
has_arg --scenario-file || ARGS+=(--scenario-file "$SCRIPT_DIR/import-models.scenarios")
has_arg --output-dir || ARGS+=(--output-dir "$PROJECT_DIR/../profiler-output")
if ! has_arg --gradle-user-home; then
    GRADLE_USER_HOME_DIR=${GRADLE_USER_HOME_DIR:-$IT_MODULE/build/testKitCache}
    [[ -d "$GRADLE_USER_HOME_DIR" ]] && ARGS+=(--gradle-user-home "$GRADLE_USER_HOME_DIR")
fi

echo "gradle-profiler: $GRADLE_PROFILER_HOME"
echo "build action classes: $TEST_CLASSES"
echo "kotlin-import-models: $IMPORT_MODELS_JAR"
echo "arguments: ${ARGS[*]}"

exec java -cp "$CP" org.gradle.profiler.Main --project-dir "$PROJECT_DIR" "${ARGS[@]}"
