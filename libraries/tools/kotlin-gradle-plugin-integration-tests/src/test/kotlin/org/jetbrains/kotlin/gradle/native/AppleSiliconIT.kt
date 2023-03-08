/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.fail

class AppleSiliconIT : BaseGradleIT() {
    override val defaultGradleVersion: GradleVersionRequired = GradleVersionRequired.FOR_MPP_SUPPORT

    private val host = HostManager.host

    @BeforeTest
    fun assumeMacosHost() {
        assumeTrue("Test requires MacOS host", HostManager.hostIsMac)
    }

    @Test
    fun `test compilation`() {
        with(Project("appleSilicon")) {
            build("assemble") {
                assertSuccessful()
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

    @Test
    fun `test execution`() {
        with(Project("appleSilicon")) {
            build("check") {
                assertSuccessful()
                assertTasksExecuted(":jvmTest")
                assertContains("Executed Code from: commonMain/jvmMain")

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
                        assertContains(*armTestOutputs.toTypedArray())
                        assertNotContains(*x64TestOutputs.toTypedArray())
                    }

                    KonanTarget.MACOS_X64 -> {
                        assertContains(*x64TestOutputs.toTypedArray())
                        assertNotContains(*armTestOutputs.toTypedArray())
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
                        assertTasksNotExecuted(x64Tests)
                    }

                    KonanTarget.MACOS_X64 -> {
                        assertTasksExecuted(x64Tests)
                        assertTasksNotExecuted(armTests)
                    }

                    else -> fail("Unexpected host $host")
                }
            }
        }
    }
}
