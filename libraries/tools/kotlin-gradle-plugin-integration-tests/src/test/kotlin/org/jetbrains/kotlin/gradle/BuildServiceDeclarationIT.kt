/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Build services usages in tasks are declared with `usesService`")
class BuildServiceDeclarationIT : KGPBaseTest() {

    @DisplayName("Build services are registered for Kotlin/JVM projects")
    @GradleTest
    @JvmGradlePluginTests
    fun testJvmProject(gradleVersion: GradleVersion) {
        project(
            "kotlinJavaProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Fail)
        ) {
            enableStableConfigurationCachePreview()
            build("build") {
                assertOutputDoesNotContainBuildServiceDeclarationWarnings()
            }
        }
    }

    @DisplayName("Build services are registered for Kotlin/JS browser projects")
    @GradleTest
    @JsGradlePluginTests
    fun testJsBrowserProject(gradleVersion: GradleVersion) {
        project(
            "kotlin-js-browser-project",
            gradleVersion,
            buildOptions = defaultBuildOptions.suppressDeprecationWarningsOn(
                "We currently have other warnings when `STABLE_CONFIGURATION_CACHE` is enabled unrelated to build services declaration, " +
                        "so we check for this kind of warnings in the build output (see KT-55563 and KT-55740)"
            ) { gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_0) }
        ) {
            enableStableConfigurationCachePreview()
            build("build") {
                assertOutputDoesNotContainBuildServiceDeclarationWarnings()
            }
        }
    }

    @DisplayName("Build services are registered for Kotlin/JS nodejs projects")
    @GradleTest
    @JsGradlePluginTests
    fun testJsNodeJsProject(gradleVersion: GradleVersion) {
        project(
            "kotlin-js-nodejs-project",
            gradleVersion,
            buildOptions = defaultBuildOptions.suppressDeprecationWarningsOn(
                "We currently have other warnings when `STABLE_CONFIGURATION_CACHE` is enabled unrelated to build services declaration, " +
                        "so we check for this kind of warnings in the build output (see KT-55563 and KT-55740)"
            ) { gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_0) }
        ) {
            enableStableConfigurationCachePreview()
            build("build") {
                assertOutputDoesNotContainBuildServiceDeclarationWarnings()
            }
        }
    }

    @DisplayName("Build services are registered for Kotlin/MPP projects")
    @GradleTest
    @MppGradlePluginTests
    fun testMppProject(gradleVersion: GradleVersion) {
        project(
            "new-mpp-lib-with-tests",
            gradleVersion,
            buildOptions = defaultBuildOptions.suppressDeprecationWarningsOn(
                "We currently have other warnings when `STABLE_CONFIGURATION_CACHE` is enabled unrelated to build services declaration, " +
                        "so we check for this kind of warnings in the build output (see KT-55563 and KT-55740)"
            ) { gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_0) }
        ) {
            enableStableConfigurationCachePreview()
            build("build") {
                assertOutputDoesNotContainBuildServiceDeclarationWarnings()
            }
        }
    }

    @DisplayName("Build services are registered for Kapt projects")
    @GradleTest
    @OtherGradlePluginTests
    fun testKaptProject(gradleVersion: GradleVersion) {
        project(
            "kapt2/simple",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(warningMode = WarningMode.Fail)
        ) {
            enableStableConfigurationCachePreview()
            build("build") {
                assertOutputDoesNotContainBuildServiceDeclarationWarnings()
            }
        }
    }

    private fun BuildResult.assertOutputDoesNotContainBuildServiceDeclarationWarnings() {
        assertOutputDoesNotContain("without the corresponding declaration via 'Task#usesService'")
    }
}