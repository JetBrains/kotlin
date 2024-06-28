/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

description = "Shaded test jars from compiler for Gradle integration tests"

plugins {
    `java-library`
}

val projectsToInclude = listOf(
    ":compiler:test-infrastructure-utils",
    ":compiler:tests-common",
    ":compiler:incremental-compilation-impl",
    ":kotlin-build-common"
)

dependencies {
    for (projectName in projectsToInclude) {
        api(projectTests(projectName)) { isTransitive = false }
        embedded(projectTests(projectName)) { isTransitive = false }
    }

    embedded(intellijJavaRt())
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
