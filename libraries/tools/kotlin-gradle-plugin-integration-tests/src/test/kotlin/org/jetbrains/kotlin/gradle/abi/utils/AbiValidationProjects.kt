/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.abi.utils

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*

private const val BCV_PLUGIN_ID = "org.jetbrains.kotlinx.binary-compatibility-validator"
private const val BCV_PLUGIN_LATEST_VERSION = "0.18.0"

/**
 * Creates a test project with Kotlin Android Gradle Plugin.
 */
internal fun KGPBaseTest.androidProject(
    gradleVersion: GradleVersion,
    agpVersion: String,
    jdkVersion: JdkVersions.ProvidedJdk,
    applyBcvPlugin: Boolean = false,
    buildCache: Boolean = false,
    configuration: TestProject.() -> Unit
) {
    val buildOptions = defaultBuildOptions.copy(buildCacheEnabled = buildCache, androidVersion = agpVersion)
    val project = project(
        "AndroidSimpleApp",
        gradleVersion,
        buildOptions = buildOptions,
        buildJdk = jdkVersion.location
    ) {
        if (applyBcvPlugin) {
            plugins { id(BCV_PLUGIN_ID).version(BCV_PLUGIN_LATEST_VERSION) }
        }
        addKgpToBuildScriptCompilationClasspath()
    }

    project.configuration()
}

/**
 * Creates a test project with Kotlin Multiplatform Gradle Plugin and an Android target.
 */
internal fun KGPBaseTest.kmpWithAndroidProject(
    gradleVersion: GradleVersion,
    agpVersion: String,
    jdkVersion: JdkVersions.ProvidedJdk,
    applyBcvPlugin: Boolean = false,
    buildCache: Boolean = false,
    configuration: TestProject.() -> Unit
) {
    val project = project(
        "base-kotlin-multiplatform-android-library",
        gradleVersion,
        buildJdk = jdkVersion.location,
        buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion, buildCacheEnabled = buildCache)
    ) {
        if (applyBcvPlugin) {
            plugins { id(BCV_PLUGIN_ID).version(BCV_PLUGIN_LATEST_VERSION) }
        }

        buildScriptInjection {
            applyDefaultAndroidLibraryConfiguration()

            kotlinMultiplatform.jvm()
            @Suppress("DEPRECATION")
            kotlinMultiplatform.androidTarget()
        }
    }
    project.configuration()
}
