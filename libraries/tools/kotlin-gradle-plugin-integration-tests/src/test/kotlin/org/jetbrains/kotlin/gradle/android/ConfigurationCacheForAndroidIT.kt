/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.AbstractConfigurationCacheIT
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.TestVersions.AGP.AGP_42
import org.junit.jupiter.api.DisplayName

@DisplayName("Configuration cache in Android project")
@AndroidGradlePluginTests
class ConfigurationCacheForAndroidIT : AbstractConfigurationCacheIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(
        androidVersion = AGP_42,
        // Workaround for a deprecation warning from AGP
        // Relying on FileTrees for ignoring empty directories when using @SkipWhenEmpty has been deprecated.
        warningMode = WarningMode.None,
    )

    @DisplayName("works in android plus kapt project")
    @GradleTest
    fun testAndroidKaptProject(gradleVersion: GradleVersion) {
        project("kapt2/android-dagger", gradleVersion) {
            gradleProperties.append("\nkapt.incremental.apt=false")

            testConfigurationCacheOf(
                ":app:compileDebugKotlin",
                ":app:kaptDebugKotlin",
                ":app:kaptGenerateStubsDebugKotlin"
            )
        }
    }

    @DisplayName("works in android project")
    @GradleTest
    fun testKotlinAndroidProject(gradleVersion: GradleVersion) {
        project("AndroidProject", gradleVersion) {
            testConfigurationCacheOf(
                ":Lib:compileFlavor1DebugKotlin",
                ":Android:compileFlavor1DebugKotlin"
            )
        }
    }

    @DisplayName("works with android tests")
    @GradleTest
    fun testKotlinAndroidProjectTests(gradleVersion: GradleVersion) {
        project("AndroidIncrementalMultiModule", gradleVersion) {
            testConfigurationCacheOf(
                ":app:compileDebugAndroidTestKotlin",
                ":app:compileDebugUnitTestKotlin"
            )
        }
    }
}
