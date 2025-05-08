/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import gradle.GradlePluginVariant
import plugins.signLibraryPublication

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish")
}

// Enable signing for publications into Gradle Plugin Portal
val signPublication = !version.toString().contains("-SNAPSHOT") &&
        (project.gradle.startParameter.taskNames.contains("publishPlugins") || signLibraryPublication)

configureBuildToolsApiVersionForGradleCompatibility()
configureCommonPublicationSettingsForGradle(signPublication)
addBomCheckTask()
extensions.extraProperties["kotlin.stdlib.default.dependency"] = "false"

// common plugin bundle configuration
gradlePlugin {
    website.set("https://kotlinlang.org/")
    vcsUrl.set("https://github.com/jetbrains/kotlin")
    plugins.configureEach {
        tags.add("kotlin")
    }
}

publishing {
    publications {
        withType<MavenPublication>().configureEach {
            when {
                name == "pluginMaven" -> {
                    suppressAllPomMetadataWarnings() // Don't warn about additional published variants
                }
                name.endsWith("PluginMarkerMaven") -> {
                    pom {
                        // https://github.com/gradle/gradle/issues/8754
                        // and https://github.com/gradle/gradle/issues/6155
                        packaging = "pom"
                    }
                }
            }
        }
    }
}

tasks {
    named("install") {
        dependsOn(named("validatePlugins"))
    }
}

val commonSourceSet = createGradleCommonSourceSet()
reconfigureMainSourcesSetForGradlePlugin(commonSourceSet)
publishShadowedJar(sourceSets[SourceSet.MAIN_SOURCE_SET_NAME], commonSourceSet)

// Disabling this task, so "com.gradle.plugin-publish" will not publish unshadowed jar into Gradle Plugin Portal
// Without it 'jar' task is asked to run by "com.gradle.plugin-publish" even if artifacts are removed. The problem
// is that 'jar' task runs after shadow task plus their outputs has the same name leading to '.jar' file overwrite.
tasks.named("jar") {
    enabled = false
}

if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
    // Used for Gradle 8.0+ versions
    val gradle80SourceSet = createGradlePluginVariant(
        GradlePluginVariant.GRADLE_80,
        commonSourceSet = commonSourceSet
    )
    publishShadowedJar(gradle80SourceSet, commonSourceSet)

    // Used for Gradle 8.1+ versions
    val gradle81SourceSet = createGradlePluginVariant(
        GradlePluginVariant.GRADLE_81,
        commonSourceSet = commonSourceSet
    )
    publishShadowedJar(gradle81SourceSet, commonSourceSet)

    // Used for Gradle 8.2+ versions
    val gradle82SourceSet = createGradlePluginVariant(
        GradlePluginVariant.GRADLE_82,
        commonSourceSet = commonSourceSet
    )
    publishShadowedJar(gradle82SourceSet, commonSourceSet)

    // Used for Gradle 8.5+ versions
    val gradle85SourceSet = createGradlePluginVariant(
        GradlePluginVariant.GRADLE_85,
        commonSourceSet = commonSourceSet
    )
    publishShadowedJar(gradle85SourceSet, commonSourceSet)

    // Used for Gradle 8.6+ versions
    val gradle86SourceSet = createGradlePluginVariant(
        GradlePluginVariant.GRADLE_86,
        commonSourceSet = commonSourceSet
    )
    publishShadowedJar(gradle86SourceSet, commonSourceSet)

    // Used for Gradle 8.8+ versions
    val gradle88SourceSet = createGradlePluginVariant(
        GradlePluginVariant.GRADLE_88,
        commonSourceSet = commonSourceSet
    )
    publishShadowedJar(gradle88SourceSet, commonSourceSet)

    // Used for Gradle 8.11+ versions
    val gradle811SourceSet = createGradlePluginVariant(
        GradlePluginVariant.GRADLE_811,
        commonSourceSet = commonSourceSet
    )
    publishShadowedJar(gradle811SourceSet, commonSourceSet)

    // Used for Gradle 8.13+ versions
    val gradle813SourceSet = createGradlePluginVariant(
        GradlePluginVariant.GRADLE_813,
        commonSourceSet = commonSourceSet
    )
    publishShadowedJar(gradle813SourceSet, commonSourceSet)
}
