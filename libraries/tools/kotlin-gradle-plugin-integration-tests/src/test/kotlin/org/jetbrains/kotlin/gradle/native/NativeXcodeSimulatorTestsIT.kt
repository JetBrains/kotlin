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

@DisplayName("tests for the K/N XCode simulator test infrastructure")
@NativeGradlePluginTests
@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
class NativeXcodeSimulatorTestsIT : KGPBaseTest() {
    @DisplayName("A user-friendly error message is produced when the standalone mode is disabled and no simulator has booted")
    @GradleTest
    fun checkNoSimulatorErrorMessage(gradleVersion: GradleVersion) {
        XCTestHelpers().use {
            val unbootedSimulator = it.createSimulator()
            project("native-test-ios-https-request", gradleVersion) {
                buildGradleKts.append(
                    """
                tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest> {
                    device.set("${unbootedSimulator.udid}")
                    standalone.set(false)
                }
                """.trimIndent()
                )
                buildAndFail("check") {
                    assertOutputContains("The problem can be that you have not booted the required device or have configured the task to a different simulator. Please check the task output and its device configuration.")
                }
            }
        }
    }

    @DisplayName("iOS simulator test with an https request fails with default settings")
    @GradleTest
    fun checkSimulatorTestFailsInStandaloneMode(gradleVersion: GradleVersion) {
        project("native-test-ios-https-request", gradleVersion) {
            buildAndFail("check") {
                // that's an Apple's issue that we expect to fail the build
                assertOutputContains("The certificate for this server is invalid. You might be connecting to a server that is pretending to be ")
            }
        }
    }

    @DisplayName("iOS simulator test with an https request doesn't fail with the standalone mode disabled")
    @GradleTest
    fun checkSimulatorTestDoesNotFailInNonStandaloneMode(gradleVersion: GradleVersion) {
        XCTestHelpers().use {
            val simulator = it.createSimulator().apply {
                boot()
            }

            project("native-test-ios-https-request", gradleVersion) {
                buildGradleKts.append(
                    """
                tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest> {
                    device.set("${simulator.udid}")
                    standalone.set(false)
                }
                """.trimIndent()
                )
                build("check") {
                    when (HostManager.host) {
                        KonanTarget.MACOS_ARM64 -> assertTasksExecuted(":iosSimulatorArm64Test")
                        KonanTarget.MACOS_X64 -> assertTasksExecuted(":iosX64Test")
                        else -> error("Unexpected host")
                    }
                }
            }
        }
    }
}