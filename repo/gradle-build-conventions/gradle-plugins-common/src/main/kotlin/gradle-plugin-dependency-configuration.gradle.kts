/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
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
createGradlePluginVariants(
    commonSourceSet = commonSourceSet,
    publishShadowedJar = false
)

publishing {
    publications {
        register<MavenPublication>(DEFAULT_MAIN_PUBLICATION_NAME) {
            from(components["java"])
            suppressAllPomMetadataWarnings() // Don't warn about additional published variants
        }
    }
}

disableCoroutinesStacktraceRecoveryInTestsIfGradleEmbeddedStdlibIsInRuntimeClasspath()