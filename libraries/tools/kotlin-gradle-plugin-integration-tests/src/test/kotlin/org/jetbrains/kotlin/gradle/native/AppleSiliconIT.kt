/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.test.fail

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for Apple Silicon builds")
@NativeGradlePluginTests
class AppleSiliconIT : KGPBaseTest() {

    private val host = HostManager.host

    @DisplayName("Tests compilation")
    @GradleTest
    fun shouldCompiledSuccessfully(gradleVersion: GradleVersion) {
        nativeProject("appleSilicon", gradleVersion) {
            build("assemble") {
                assertTasksExecuted(
                    ":compileKotlinIosArm64",
                    ":compileKotlinIosSimulatorArm64",
                    ":compileKotlinIosX64",
                    ":compileKotlinMacosArm64",
                    ":compileKotlinMacosX64",
                    ":compileKotlinTvosArm64",
                    ":compileKotlinTvosSimulatorArm64",
                    ":compileKotlinTvosX64",
                    ":compileKotlinWatchosArm32",
                    ":compileKotlinWatchosArm64",
                    ":compileKotlinWatchosSimulatorArm64",
                    ":compileKotlinWatchosX64",
                    ":compileKotlinJvm",
                    ":jvmJar",
                    ":linkDebugExecutableMacosArm64",
                    ":linkDebugExecutableMacosX64",
                    ":linkReleaseExecutableMacosArm64",
                    ":linkReleaseExecutableMacosX64",
                )
            }
        }
    }

    @DisplayName("Tests execution")
    @GradleTest
    fun shouldExecuteTestsSuccessfully(gradleVersion: GradleVersion) {
        nativeProject("appleSilicon", gradleVersion) {
            build("check") {
                assertTasksExecuted(":jvmTest")
                assertOutputContains("Executed Code from: commonMain/jvmMain")

                val armTestOutputs = listOf(
                    "commonMain/iosMain/iosSimulatorArm64Main",
                    "commonMain/macosMain/macosArm64Main",
                    "commonMain/tvosMain/tvosSimulatorArm64Main",
                    "commonMain/watchosMain/watchosSimulatorArm64Main"
                ).map { "Executed Code from: $it" }

                val x64TestOutputs = listOf(
                    "commonMain/iosMain/iosX64Main",
                    "commonMain/macosMain/macosX64Main",
                    "commonMain/tvosMain/tvosX64Main",
                    "commonMain/watchosMain/watchosX64Main"
                ).map { "Executed Code from: $it" }

                when (host) {
                    KonanTarget.MACOS_ARM64 -> {
                        for (s in armTestOutputs) {
                            assertOutputContains(s)
                        }
                        for (s in x64TestOutputs) {
                            assertOutputDoesNotContain(s)
                        }

                    }

                    KonanTarget.MACOS_X64 -> {
                        for (s in x64TestOutputs) {
                            assertOutputContains(s)
                        }
                        for (s in armTestOutputs) {
                            assertOutputDoesNotContain(s)
                        }
                    }

                    else -> fail("Unexpected host $host")
                }

                val armTests = listOf(
                    ":iosSimulatorArm64Test",
                    ":macosArm64Test",
                    ":tvosSimulatorArm64Test",
                    ":watchosSimulatorArm64Test",
                )

                val x64Tests = listOf(
                    ":iosX64Test",
                    ":macosX64Test",
                    ":tvosX64Test",
                    ":watchosX64Test",
                )

                when (host) {
                    KonanTarget.MACOS_ARM64 -> {
                        assertTasksExecuted(armTests)
                        assertTasksSkipped(*x64Tests.toTypedArray())
                    }

                    KonanTarget.MACOS_X64 -> {
                        assertTasksExecuted(x64Tests)
                        assertTasksSkipped(*armTests.toTypedArray())
                    }

                    else -> fail("Unexpected host $host")
                }
            }
        }
    }
}