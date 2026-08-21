/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.publish.PublishingExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import java.io.File

fun KGPBaseTest.externalAndroidLibraryProject(
    gradleVersion: GradleVersion,
    androidVersion: String,
    jdkVersion: JdkVersions.ProvidedJdk,
    namespace: String = "org.jetbrains.sample.androidlibrary",
    withJava: Boolean = false,
    androidLibraryConfiguration: KotlinMultiplatformAndroidLibraryTarget.() -> Unit = {},
    configureProject: TestProject.() -> Unit = {},
): TestProject = project(
    "empty",
    gradleVersion = gradleVersion,
    buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
    buildJdk = jdkVersion.location,
) {
    plugins {
        kotlin("multiplatform")
        id("com.android.kotlin.multiplatform.library")
    }
    buildScriptInjection {
        kotlinMultiplatform.apply {
            targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach { target ->
                target.compileSdk = 34
                target.namespace = namespace
                if (withJava) target.withJava()
                target.androidLibraryConfiguration()
            }
        }
    }
    configureProject()
}

fun KGPBaseTest.kotlinAndroidLibraryProject(
    gradleVersion: GradleVersion,
    agpVersion: String,
    jdkVersion: JdkVersions.ProvidedJdk,
): TestProject {
    return project(
        "base-kotlin-android-library",
        gradleVersion,
        buildOptions = defaultBuildOptions
            .copy(androidVersion = agpVersion)
            .suppressAgpWarningIsProperty(gradleVersion),
        buildJdk = jdkVersion.location,
    ) {
        buildScriptInjection { applyDefaultAndroidLibraryConfiguration() }
    }
}

fun GradleProjectBuildScriptInjectionContext.applyMavenPublishPlugin(localRepoDir: File? = null): PublishingExtension {
    project.plugins.apply("maven-publish")
    publishing.repositories.apply {
        maven { maven ->
            if (localRepoDir != null) maven.setUrl(localRepoDir)
            else maven.setUrl(project.layout.projectDirectory.dir("repo"))
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
