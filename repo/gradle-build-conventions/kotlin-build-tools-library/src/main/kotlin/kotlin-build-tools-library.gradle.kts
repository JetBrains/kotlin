/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Shared setup for Kotlin Build Tools libraries under build-tools/libraries.
// See README.md next to this project and build-tools/libraries/AGENTS.md for the rules these modules follow.

import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("test-inputs-check")
}

// Applied here rather than in `plugins {}`: generating accessors for it would apply it to a synthetic
// project without the repository's build properties. Nothing below needs accessors from it.
apply(plugin = "common-configuration")

private val libs = project.the<LibrariesForLibs>()

configureKotlinCompileTasksGradleCompatibility()

kotlin {
    explicitApi()
}

publish {
    // The group is the repository-wide org.jetbrains.kotlin, so the prefix keeps the family together
    artifactId = "build-tools-${project.name}"

    // Modules set `description` after this plugin is applied, so read it lazily for the POM description.
    // Without one, keep what the publishing plugin already chose (the humanised artifact name).
    pom {
        val fallback = description.get()
        description.set(provider { project.description }.orElse(fallback))
    }
}
standardPublicJars()

dependencies {
    // The integration that loads a library provides the stdlib
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))

    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(kotlin("test-junit5", coreDepsVersion))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Dump the API from the jar, as the other KGP-compatible libraries do
tasks.apiBuild {
    inputJar.value(tasks.jar.flatMap { it.archiveFile })
}

// The repository's test task factory attaches the instrumenting agent that test-inputs-check relies on
extensions.configure<ProjectTestsExtension>("projectTests") {
    testTask {
        useJUnitPlatform()
    }
}
