/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.executors

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.Xcode
import kotlin.math.min

internal val gson: Gson by lazy { GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()!! }

/**
 * Compares two strings assuming that both are representing numeric version strings.
 * Examples of numeric version strings: "12.4.1.2", "9", "0.5".
 */
private fun compareStringsAsVersions(version1: String, version2: String): Int {
    val splitVersion1 = version1.split('.').map { it.toInt() }
    val splitVersion2 = version2.split('.').map { it.toInt() }
    val minimalLength = min(splitVersion1.size, splitVersion2.size)
    for (index in 0 until minimalLength) {
        if (splitVersion1[index] < splitVersion2[index]) return -1
        if (splitVersion1[index] > splitVersion2[index]) return 1
    }
    return splitVersion1.size.compareTo(splitVersion2.size)
}

internal fun simulatorOsName(family: Family): String {
    return when (family) {
        Family.IOS -> "iOS"
        Family.WATCHOS -> "watchOS"
        Family.TVOS -> "tvOS"
        else -> error("Unexpected simulator OS: $family")
    }
}

/**
 * Returns parsed output of `xcrun simctl list runtimes -j`.
 */
private fun Xcode.getSimulatorRuntimeDescriptors(): List<SimulatorRuntimeDescriptor> =
        gson.fromJson(simulatorRuntimes, ListRuntimesReport::class.java).runtimes
private fun getSimulatorRuntimeDescriptors(json: String): List<SimulatorRuntimeDescriptor> =
        gson.fromJson(json, ListRuntimesReport::class.java).runtimes

private fun getLatestSimulatorRuntimeFor(
        descriptors: List<SimulatorRuntimeDescriptor>,
        family: Family,
        osMinVersion: String
): SimulatorRuntimeDescriptor? {
    val osName = simulatorOsName(family)
    return descriptors.firstOrNull {
        it.checkAvailability() && it.name.startsWith(osName) && compareStringsAsVersions(it.version, osMinVersion) >= 0
    }
}

private fun getSimulatorRuntimesFor(
        descriptors: List<SimulatorRuntimeDescriptor>,
        family: Family,
        osMinVersion: String
): List<SimulatorRuntimeDescriptor> {
    val osName = simulatorOsName(family)
    return descriptors.filter {
        it.checkAvailability() && it.name.startsWith(osName) && compareStringsAsVersions(it.version, osMinVersion) >= 0
    }
}

/**
 * Returns first available simulator runtime for [target] with at least [osMinVersion] OS version.
 * */
fun Xcode.getLatestSimulatorRuntimeFor(family: Family, osMinVersion: String): SimulatorRuntimeDescriptor? =
        getLatestSimulatorRuntimeFor(getSimulatorRuntimeDescriptors(), family, osMinVersion)

fun getLatestSimulatorRuntimeFor(json: String, family: Family, osMinVersion: String): SimulatorRuntimeDescriptor? =
        getLatestSimulatorRuntimeFor(getSimulatorRuntimeDescriptors(json), family, osMinVersion)

fun getSimulatorRuntimesFor(json: String, family: Family, osMinVersion: String): List<SimulatorRuntimeDescriptor> =
        getSimulatorRuntimesFor(getSimulatorRuntimeDescriptors(json), family, osMinVersion)

// Result of `xcrun simctl list runtimes -j`.
data class ListRuntimesReport(
        @Expose val runtimes: List<SimulatorRuntimeDescriptor>
)

data class SimulatorRuntimeDescriptor(
        @Expose val version: String,
        // bundlePath field may not exist in the old Xcode (prior to 10.3).
        @Expose val bundlePath: String? = null,
        @Expose val isAvailable: Boolean? = null,
        @Expose val availability: String? = null,
        @Expose val name: String,
        @Expose val identifier: String,
        @Expose val buildversion: String,
        @Expose val supportedDeviceTypes: List<DeviceType>
) {
    /**
     * Different Xcode/macOS combinations give different fields that checks
     * runtime availability. This method is an umbrella for these fields.
     */
    fun checkAvailability(): Boolean {
        if (isAvailable == true) return true
        if (availability?.contains("unavailable") == true) return false
        return false
    }
}

data class DeviceType(
        @Expose val bundlePath: String,
        @Expose val name: String,
        @Expose val identifier: String,
        @Expose val productFamily: String
)

/**
 * Returns map of simulator devices from the json input
 */
fun getSimulatorDevices(json: String): Map<String, List<SimulatorDeviceDescriptor>> =
        gson.fromJson(json, ListDevicesReport::class.java).devices

// Result of `xcrun simctl list devices -j`
data class ListDevicesReport(
        @Expose val devices: Map<String, List<SimulatorDeviceDescriptor>>
)

data class SimulatorDeviceDescriptor(
        @Expose val lastBootedAt: String?,
        @Expose val dataPath: String,
        @Expose val dataPathSize: Long,
        @Expose val logPath: String,
        @Expose val udid: String,
        @Expose val isAvailable: Boolean?,
        @Expose val deviceTypeIdentifier: String,
        @Expose val state: String,
        @Expose val name: String
)
