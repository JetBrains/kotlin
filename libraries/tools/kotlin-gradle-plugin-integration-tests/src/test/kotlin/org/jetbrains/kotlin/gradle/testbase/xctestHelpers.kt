/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.gradle.util.assertProcessRunResult
import org.jetbrains.kotlin.gradle.util.runProcess
import java.io.Closeable
import java.io.File
import java.util.*

internal class XCTestHelpers : Closeable {
    @Serializable
    data class Device(val name: String, val udid: String)

    @Serializable
    data class Simulators(val devices: Map<String, List<Device>>)

    private val deviceIdentifier = "com.apple.CoreSimulator.SimDeviceType.iPhone-12-Pro-Max"
    private val uuid = UUID.randomUUID()
    private val testSimulatorName = "NativeXcodeSimulatorTestsIT_${uuid}_simulator"

    fun createSimulator(): Device {
        return Device(
            testSimulatorName,
            processOutput(
                listOf("/usr/bin/xcrun", "simctl", "create", testSimulatorName, deviceIdentifier)
            ).dropLast(1)
        )
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private fun simulators(): Simulators {
        return json.decodeFromString<Simulators>(
            processOutput(
                listOf("/usr/bin/xcrun", "simctl", "list", "devices", "-j")
            )
        )
    }

    override fun close() {
        simulators().devices.values.toList().flatten().filter {
            it.name == testSimulatorName
        }.forEach {
            processOutput(
                listOf("/usr/bin/xcrun", "simctl", "delete", it.udid)
            )
        }
    }
}

internal fun XCTestHelpers.Device.boot() {
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