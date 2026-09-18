#!/usr/bin/env bash
#
# Generates a synthetic multi-module Kotlin/JVM project for profiling the task-backed import models POC.
#
#   generate-project.sh <target-dir> [jvm-modules=30] [kotlin-version=2.5.255-SNAPSHOT] [baseline-repo=<target-dir>/../baseline-repo]
#
# Layout: `jvm-N` modules (kotlin("jvm"), main + test). Every module exposes its predecessor as an `api` dependency
# (so the compile classpath of `jvm-N` transitively contains all N-1 predecessors) and has a handful of large external
# dependencies (~100 transitive artifacts), so dependency resolution is a realistic part of the model computation.
# The Kotlin Gradle plugin is resolved from `mavenLocal()` — run `./gradlew install` in the Kotlin repository first.
# With `-PkotlinBaseline` the baseline Maven repository (the artifacts of the parent commit of the POC, same version;
# see README.md) is preferred for both the plugin and the dependencies; whatever it lacks comes from `mavenLocal()`.
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
        // the Kotlin artifacts of the parent commit of the POC, see README.md
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
        if (providers.gradleProperty("kotlinBaseline").isPresent) maven("file://$BASELINE_REPO")
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
    projectDependency=""
    if ((i > 1)); then
        projectDependency="    api(project(\":jvm-$((i - 1))\"))"$'\n'
    fi
    cat > "$TARGET_DIR/$module/build.gradle.kts" <<EOF
plugins {
    kotlin("jvm")
}

dependencies {
$projectDependency    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("io.ktor:ktor-server-core:3.1.3")
    implementation("io.ktor:ktor-client-cio:3.1.3")
    implementation("org.springframework.boot:spring-boot-starter-web:3.4.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
    testImplementation("io.mockk:mockk:1.13.17")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.4")
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
