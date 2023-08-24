/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.assertProcessRunResult
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.io.File
import java.util.UUID

@DisplayName("tests for the K/N XCode simulator test infrastructure")
@NativeGradlePluginTests
@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
class NativeXcodeSimulatorTestsIT : KGPBaseTest() {
    @DisplayName("A user-friendly error message is produced when the standalone mode is disabled and no simulator has booted")
    @GradleTest
    fun checkNoSimulatorErrorMessage(gradleVersion: GradleVersion) {
        val unbootedSimulator = createSimulator()
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
            val simulator = createSimulator()
            simulator.boot()
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

    @Serializable
    private data class Device(val name: String, val udid: String)
    @Serializable
    private data class Simulators(val devices: Map<String, List<Device>>)

    private val deviceIdentifier = "com.apple.CoreSimulator.SimDeviceType.iPhone-12-Pro-Max"
    private val uuid = UUID.randomUUID()
    private val testSimulatorName = "NativeXcodeSimulatorTestsIT_${uuid}_simulator"

    @AfterAll
    fun removeSimulatorsCreatedForTests() {
        simulators().devices.values.toList().flatMap { it }.filter {
            it.name == testSimulatorName
        }.forEach {
            processOutput(
                listOf("/usr/bin/xcrun", "simctl", "delete", it.udid)
            )
        }
    }

    private fun simulators(): Simulators {
        return Json {
            ignoreUnknownKeys = true
        }.decodeFromString<Simulators>(
            processOutput(
                listOf("/usr/bin/xcrun", "simctl", "list", "devices", "-j")
            )
        )
    }

    private fun createSimulator(): Device {
        return Device(
            testSimulatorName,
            processOutput(
                listOf("/usr/bin/xcrun", "simctl", "create", testSimulatorName, deviceIdentifier)
            ).dropLast(1)
        )
    }

    private fun Device.boot() {
        processOutput(
            listOf("/usr/bin/xcrun", "simctl", "bootstatus", udid, "-bd")
        )
    }

    private fun processOutput(arguments: List<String>): String {
        val result = runProcess(
            arguments, File("."),
            redirectErrorStream = false,
        )
        assertProcessRunResult(
            result
        ) {
            assert(isSuccessful)
        }
        return result.output
    }
}