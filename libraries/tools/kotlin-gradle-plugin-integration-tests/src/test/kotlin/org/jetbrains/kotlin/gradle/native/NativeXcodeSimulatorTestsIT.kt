/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.gradle.utils.Xcode
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File
import kotlin.test.assertEquals

@DisplayName("tests for the K/N XCode simulator test infrastructure")
@NativeGradlePluginTests
@EnabledOnOs(OS.MAC)
class NativeXcodeSimulatorTestsIT : KGPBaseTest() {
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

    @DisplayName("iOS simulator test with an https request fails with the standalone mode disabled")
    @GradleTest
    fun checkSimulatorTestFailsInNonStandaloneMode(gradleVersion: GradleVersion) {
        project("native-test-ios-https-request", gradleVersion) {
            val xcode = Xcode ?: error("XCode is expected to be defined")
            val device = xcode.getDefaultTestDeviceId(KonanTarget.IOS_SIMULATOR_ARM64) ?: error("No simulator found for iOS ARM64")
            bootXcodeSimulator(device)
            buildGradleKts.append(
                """
                tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest> {
                    device.set("$device")
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