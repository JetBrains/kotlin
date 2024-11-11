/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.publish.PublishingExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.util.GradleVersion

fun KGPBaseTest.kotlinAndroidLibraryProject(
    gradleVersion: GradleVersion,
    agpVersion: String,
    jdkVersion: JdkVersions.ProvidedJdk,
): TestProject {
    return project(
        "base-kotlin-android-library",
        gradleVersion,
        buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
        buildJdk = jdkVersion.location,
    ) {
        buildScriptInjection { applyDefaultAndroidLibraryConfiguration() }
    }
}

fun GradleProjectBuildScriptInjectionContext.applyMavenPublishPlugin(): PublishingExtension {
    project.plugins.apply("maven-publish")
    publishing.repositories.apply {
        maven { maven ->
            maven.setUrl(project.layout.projectDirectory.dir("repo"))
        }
    }
    return publishing
}

fun GradleProjectBuildScriptInjectionContext.applyDefaultAndroidLibraryConfiguration() {
    androidLibrary.apply {
        compileSdk = 31
        defaultConfig {
            minSdk = 31
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        namespace = "org.jetbrains.kotlin.sample"
    }

    java.apply {
        toolchain.languageVersion.set(JavaLanguageVersion.of(8))
    }
}