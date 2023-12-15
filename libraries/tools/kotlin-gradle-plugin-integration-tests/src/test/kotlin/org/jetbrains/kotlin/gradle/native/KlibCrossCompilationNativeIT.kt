/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@DisplayName("Tests for checking klib cross-compilation in KGP")
@NativeGradlePluginTests
class KlibCrossCompilationNativeIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata("klibCrossCompilationDefaultSettings")
    @OsCondition(supportedOn = [OS.LINUX, OS.WINDOWS], enabledOnCI = [OS.LINUX, OS.WINDOWS])
    fun compileIosTargetOnNonDarwinHostWithDefaultSettings(gradleVersion: GradleVersion) {
        nativeProject("klibCrossCompilationDefaultSettings", gradleVersion) {
            build(":compileKotlinIosArm64") {
                KotlinTestUtils.assertEqualsToFile(projectPath.resolve("diagnostics.txt"), extractProjectsAndTheirDiagnostics())
                assertTasksSkipped(":compileKotlinIosArm64")
            }
        }
    }

    @GradleTest
    @TestMetadata("klibCrossCompilationWithGradlePropertyEnabled")
    @Disabled("For now, fails with an error from klib resolver about mismatched stdlib targets; needs KT-66967")
    @OsCondition(supportedOn = [OS.LINUX, OS.WINDOWS], enabledOnCI = [OS.LINUX, OS.WINDOWS])
    fun compileIosTargetOnNonDarwinHostWithGradlePropertyEnabled(gradleVersion: GradleVersion) {
        nativeProject("klibCrossCompilationWithGradlePropertyEnabled", gradleVersion) {
            build(":compileKotlinIosArm64") {
                KotlinTestUtils.assertEqualsToFile(
                    projectPath.resolve("diagnostics-compileKotlinIosArm64.txt"), extractProjectsAndTheirDiagnostics()
                )
                assertTasksExecuted(":compileKotlinIosArm64")
            }

            build(":linkIosArm64") {
                KotlinTestUtils.assertEqualsToFile(
                    projectPath.resolve("diagnostics-linkIosArm64.txt"), extractProjectsAndTheirDiagnostics()
                )
                assertTasksSkipped(":linkIosArm64")
            }
        }
    }
}
