/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.gradle.utils.XcodeUtils
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.io.File
import kotlin.test.assertEquals

@DisplayName("tests for the K/N XCode simulator test infrastructure")
@NativeGradlePluginTests
@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
class NativeXcodeSimulatorTestsIT : KGPBaseTest() {
    private val defaultIosSimulator by lazy {
        XcodeUtils.getDefaultTestDeviceId(KonanTarget.IOS_SIMULATOR_ARM64) ?: error("No simulator found for iOS ARM64")
    }

    @DisplayName("A user-friendly error message is produced when the standalone mode is disabled and no simulator has booted")
    @GradleTest
    fun checkNoSimulatorErrorMessage(gradleVersion: GradleVersion) {
        shutDownSimulators() // based on the tests order there no simulator should be booted, but anyway to be sure
        // Note the test still may fail if you have booted the required simulator manually before running the test
        // We don't shut down such simulators in the tests
        project("native-test-ios-https-request", gradleVersion) {
            buildGradleKts.append(
                """
                tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest> {
                    device.set("$defaultIosSimulator")
                    standalone.set(false)
                }
                """.trimIndent()
            )
            buildAndFail("check") {
                assertOutputContains("The problem can be that you have not booted the required device or have configured the task to a different simulator. Please check the task output and its device configuration.")
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
        project("native-test-ios-https-request", gradleVersion) {
            bootXcodeSimulator(defaultIosSimulator)
            buildGradleKts.append(
                """
                tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest> {
                    device.set("$defaultIosSimulator")
                    standalone.set(false)
                }
                """.trimIndent()
            )
            build("check")
        }
    }

    @AfterAll
    fun shutDownSimulators() {
        synchronized(bootedXcodeSimulators) {
            for (device in bootedXcodeSimulators) {
                val command = listOf("/usr/bin/xcrun", "simctl", "shutdown", device)
                val processResult = runProcess(command, File("."))
                assertEquals(0, processResult.exitCode)
                bootedXcodeSimulators.remove(device)
            }
            bootedXcodeSimulators.clear()
        }
    }

    private val bootedXcodeSimulators: MutableSet<String> = HashSet()

    private fun bootXcodeSimulator(device: String) {
        synchronized(bootedXcodeSimulators) {
            if (device !in bootedXcodeSimulators) {
                val command = listOf("/usr/bin/xcrun", "simctl", "boot", device)
                val processResult = runProcess(command, File("."))
                assert(processResult.exitCode == 0 || "current state: Booted" in processResult.output) {
                    "Failed to boot a simulator $device and it wasn't started before, exit code = ${processResult.exitCode}"
                }
                if (processResult.exitCode == 0) {
                    // if the simulator was booted not by us, we should not shut it down
                    bootedXcodeSimulators.add(device)
                }
            }
        }
    }
}