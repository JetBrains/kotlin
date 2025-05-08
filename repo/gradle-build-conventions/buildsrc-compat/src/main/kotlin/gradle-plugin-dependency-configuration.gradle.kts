/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import gradle.GradlePluginVariant
import plugins.KotlinBuildPublishingPlugin.Companion.DEFAULT_MAIN_PUBLICATION_NAME
import plugins.signLibraryPublication

plugins {
    `java-library`
    kotlin("jvm")
    `maven-publish`
}

configureBuildToolsApiVersionForGradleCompatibility()
configureCommonPublicationSettingsForGradle(signLibraryPublication)
addBomCheckTask()
extensions.extraProperties["kotlin.stdlib.default.dependency"] = "false"

val commonSourceSet = createGradleCommonSourceSet()
reconfigureMainSourcesSetForGradlePlugin(commonSourceSet)

// Used for Gradle 8.0+ versions
createGradlePluginVariant(
    GradlePluginVariant.GRADLE_80,
    commonSourceSet = commonSourceSet,
    isGradlePlugin = false
)

// Used for Gradle 8.1+ versions
createGradlePluginVariant(
    GradlePluginVariant.GRADLE_81,
    commonSourceSet = commonSourceSet,
    isGradlePlugin = false
)

// Used for Gradle 8.2+ versions
createGradlePluginVariant(
    GradlePluginVariant.GRADLE_82,
    commonSourceSet = commonSourceSet,
    isGradlePlugin = false
)

// Used for Gradle 8.5+ versions
createGradlePluginVariant(
    GradlePluginVariant.GRADLE_85,
    commonSourceSet = commonSourceSet,
    isGradlePlugin = false
)

// Used for Gradle 8.6+ versions
createGradlePluginVariant(
    GradlePluginVariant.GRADLE_86,
    commonSourceSet = commonSourceSet,
    isGradlePlugin = false
)

// Used for Gradle 8.8+ versions
createGradlePluginVariant(
    GradlePluginVariant.GRADLE_88,
    commonSourceSet = commonSourceSet,
    isGradlePlugin = false
)

// Used for Gradle 8.11+ versions
createGradlePluginVariant(
    GradlePluginVariant.GRADLE_811,
    commonSourceSet = commonSourceSet,
    isGradlePlugin = false
)

// Used for Gradle 8.13+ versions
createGradlePluginVariant(
    GradlePluginVariant.GRADLE_813,
    commonSourceSet = commonSourceSet,
    isGradlePlugin = false
)

publishing {
    publications {
        register<MavenPublication>(DEFAULT_MAIN_PUBLICATION_NAME) {
            from(components["java"])
            suppressAllPomMetadataWarnings() // Don't warn about additional published variants
        }
    }
}

