/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Power-Assert tests")
class PowerAssertIT : KGPBaseTest() {

    @OtherGradlePluginTests
    @DisplayName("power-assert works")
    @GradleTest
    fun testPowerAssertSimple(gradleVersion: GradleVersion) {
        project(
            "powerAssertSimple",
            gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            build("check")
        }
    }

    @OtherGradlePluginTests
    @DisplayName("power-assert only applied to configured source sets")
    @GradleTest
    fun testPowerAssertSourceSets(gradleVersion: GradleVersion) {
        project("powerAssertSourceSets", gradleVersion) {
            build("check")
        }
    }

    @OtherGradlePluginTests
    @DisplayName("power-assert applied to all compilations")
    @GradleTest
    fun testPowerAssertCompilationFilter_ALL(gradleVersion: GradleVersion) {
        project("powerAssertCompilationFilter-all", gradleVersion) {
            build("check")
        }
    }

    @OtherGradlePluginTests
    @DisplayName("power-assert applied to test compilations")
    @GradleTest
    fun testPowerAssertCompilationFilter_TEST(gradleVersion: GradleVersion) {
        project("powerAssertCompilationFilter-test", gradleVersion) {
            build("check")
        }
    }

    @OtherGradlePluginTests
    @DisplayName("power-assert applied to no compilations")
    @GradleTest
    fun testPowerAssertCompilationFilter_NONE(gradleVersion: GradleVersion) {
        project("powerAssertCompilationFilter-none", gradleVersion) {
            build("check")
        }
    }
}
