/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.AbstractConfigurationCacheIT
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Configuration cache in Android project")
@AndroidGradlePluginTests
class ConfigurationCacheForAndroidIT : AbstractConfigurationCacheIT() {
    @DisplayName("works in android plus kapt project")
    @GradleAndroidTest
    fun testAndroidKaptProject(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "kapt2/android-dagger",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            gradleProperties.append("\nkapt.incremental.apt=false")

            testConfigurationCacheOf(
                ":app:compileDebugKotlin",
                ":app:kaptDebugKotlin",
                ":app:kaptGenerateStubsDebugKotlin"
            )
        }
    }

    @DisplayName("works in android project")
    @GradleAndroidTest
    fun testKotlinAndroidProject(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            testConfigurationCacheOf(
                ":Lib:compileFlavor1DebugKotlin",
                ":Android:compileFlavor1DebugKotlin"
            )
        }
    }

    @DisplayName("works with android tests")
    @GradleAndroidTest
    fun testKotlinAndroidProjectTests(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidIncrementalMultiModule",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            testConfigurationCacheOf(
                ":app:compileDebugAndroidTestKotlin",
                ":app:compileDebugUnitTestKotlin"
            )
        }
    }
}
