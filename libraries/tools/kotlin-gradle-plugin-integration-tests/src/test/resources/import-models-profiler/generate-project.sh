#!/usr/bin/env bash
#
# Generates a synthetic multi-module Kotlin/JVM project for profiling the task-backed import models POC.
#
#   generate-project.sh <target-dir> [jvm-modules=30] [kotlin-version=2.5.255-SNAPSHOT] [baseline-repo=<target-dir>/../baseline-repo]
#
# Layout: `jvm-N` modules (kotlin("jvm"), main + test). Every module depends on its predecessor, and all modules have
# an external dependency, so dependency resolution is a realistic part of the model computation.
# The Kotlin Gradle plugin is resolved from `mavenLocal()` — run `./gradlew install` in the Kotlin repository first.
# With `-PkotlinBaseline` the plugin is taken from the baseline Maven repository instead (the plugin of the parent
# commit of the POC, same version; see README.md), everything else still comes from `mavenLocal()`.
set -euo pipefail

TARGET_DIR=${1:?"target directory is required"}
JVM_MODULES=${2:-30}
KOTLIN_VERSION=${3:-2.5.255-SNAPSHOT}

mkdir -p "$TARGET_DIR"
TARGET_DIR=$(cd "$TARGET_DIR" && pwd)
BASELINE_REPO=${4:-$(dirname "$TARGET_DIR")/baseline-repo}
mkdir -p "$BASELINE_REPO"
BASELINE_REPO=$(cd "$BASELINE_REPO" && pwd)

cat > "$TARGET_DIR/settings.gradle.kts" <<EOF
pluginManagement {
    repositories {
        // the Kotlin Gradle plugin of the parent commit of the POC, see README.md
        if (providers.gradleProperty("kotlinBaseline").isPresent) maven("file://$BASELINE_REPO")
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "$KOTLIN_VERSION"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "import-models-profiler-project"
EOF

{
    echo "org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g"
    echo "org.gradle.parallel=true"
} > "$TARGET_DIR/gradle.properties"

# Root project deliberately has no Kotlin plugin: the model builder must not be requested for it.
cat > "$TARGET_DIR/build.gradle.kts" <<EOF
// root project of the import models profiling fixture
EOF

for ((i = 1; i <= JVM_MODULES; i++)); do
    module="jvm-$i"
    echo "include(\":$module\")" >> "$TARGET_DIR/settings.gradle.kts"
    mkdir -p "$TARGET_DIR/$module/src/main/kotlin" "$TARGET_DIR/$module/src/test/kotlin"
    dependencies="    implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2\")"
    if ((i > 1)); then
        dependencies+=$'\n'"    implementation(project(\":jvm-$((i - 1))\"))"
    fi
    cat > "$TARGET_DIR/$module/build.gradle.kts" <<EOF
plugins {
    kotlin("jvm")
}

dependencies {
$dependencies
    testImplementation(kotlin("test"))
}
EOF
    cat > "$TARGET_DIR/$module/src/main/kotlin/Jvm$i.kt" <<EOF
package jvm$i

class Jvm$i {
    fun describe(): String = "jvm-$i"
}
EOF
    cat > "$TARGET_DIR/$module/src/test/kotlin/Jvm${i}Test.kt" <<EOF
package jvm$i

import kotlin.test.Test
import kotlin.test.assertEquals

class Jvm${i}Test {
    @Test
    fun describes() = assertEquals("jvm-$i", Jvm$i().describe())
}
EOF
done

echo "Generated $JVM_MODULES JVM modules in $TARGET_DIR (Kotlin $KOTLIN_VERSION, baseline repository: $BASELINE_REPO)"
