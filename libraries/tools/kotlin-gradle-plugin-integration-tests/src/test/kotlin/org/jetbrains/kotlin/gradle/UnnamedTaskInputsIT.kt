/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Tasks don't have unnamed inputs and outputs")
class UnnamedTaskInputsIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(buildCacheEnabled = true)

    private val localBuildCacheDir get() = workingDir.resolve("custom-jdk-build-cache-2")

    @JvmGradlePluginTests
    @DisplayName("JVM")
    @GradleTest
    fun inputsJvm(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)

            build("assemble") {
                assertNoUnnamedInputsOutputs()
            }
        }
    }

    @JsGradlePluginTests
    @DisplayName("JS")
    @GradleTest
    fun inputsJs(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)

            // For some reason Gradle 6.* fails with message about using deprecated API which will fail in 7.0
            // But for Gradle 7.* everything works, so seems false positive
            if (gradleVersion.baseVersion.version.substringBefore(".").toInt() >= 7) {
                build("assemble") {
                    assertNoUnnamedInputsOutputs()
                }
            }
        }
    }

    @MppGradlePluginTests
    @DisplayName("MPP")
    @GradleTest
    fun inputsMpp(gradleVersion: GradleVersion) {
        project("hierarchical-mpp-multi-modules", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)

            build("assemble") {
                assertNoUnnamedInputsOutputs()
            }
        }
    }

    @OtherGradlePluginTests
    @DisplayName("Kapt")
    @GradleTest
    fun inputsKapt(gradleVersion: GradleVersion) {
        project("kapt2/simple", gradleVersion) {
            enableLocalBuildCache(localBuildCacheDir)

            build("assemble") {
                assertNoUnnamedInputsOutputs()
                assertNoBuildWarnings(expectedK2KaptWarnings)
            }
        }
    }

    private fun BuildResult.assertNoUnnamedInputsOutputs() {
        // Check that all inputs/outputs added at runtime have proper names
        // (the unnamed ones are listed as $1, $2 etc.):
        assertOutputDoesNotContain("Appending inputPropertyHash for '\\$\\d+'".toRegex())
        assertOutputDoesNotContain("Appending outputPropertyName to build cache key: \\$\\d+".toRegex())
    }
}