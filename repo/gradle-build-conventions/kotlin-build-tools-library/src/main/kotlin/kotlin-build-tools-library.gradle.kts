/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Shared setup for Kotlin Build Tools libraries under libraries/tools/kotlin-build-tools.
// See README.md next to this project and libraries/tools/kotlin-build-tools/AGENTS.md for the rules these modules follow.

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

// The artifact name is the project name, so the group carries the family name
group = "org.jetbrains.kotlin.build-tools"

configureKotlinCompileTasksGradleCompatibility()

kotlin {
    explicitApi()
}

publish {
    // Modules set `description` after this plugin is applied, so read it lazily for the POM name.
    // Without a description, fall back to the artifact name the way the publishing plugin does.
    val pomName = provider {
        project.description ?: project.name.split("-").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    }
    pom {
        name.set(pomName)
        description.set(pomName)
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

tasks {
    // Dump the API from the jar, as the other KGP-compatible libraries do
    apiBuild {
        inputJar.value(jar.flatMap { it.archiveFile })
    }
    test {
        useJUnitPlatform()
    }
}
