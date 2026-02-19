/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

description = "Shaded test jars from compiler for Gradle integration tests"

plugins {
    `java-library`
}

val projectsToInclude = listOf(
    ":compiler:incremental-compilation-impl",
)

val fixturesToInclude = listOf(
    ":compiler:test-infrastructure-utils",
    ":compiler:tests-common",
    ":compiler:tests-compiler-utils",
    ":kotlin-build-common",
)

fun Dependency.unsetTransitive() {
    if (this is ModuleDependency) {
        isTransitive = false
    }
}

dependencies {
    for (projectName in projectsToInclude) {
        api(projectTests(projectName)) { unsetTransitive() }
        embedded(projectTests(projectName)) { unsetTransitive() }
    }

    for (projectName in fixturesToInclude) {
        api(testFixtures(project(projectName))) { unsetTransitive() }
        embedded(testFixtures(project(projectName))) { unsetTransitive() }
    }
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
