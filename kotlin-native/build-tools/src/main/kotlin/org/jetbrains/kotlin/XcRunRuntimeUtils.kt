package org.jetbrains.kotlin

import com.google.gson.annotations.Expose
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.Xcode
import kotlin.math.min

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

/**
 * Returns parsed output of `xcrun simctl list runtimes -j`.
 */
private fun Xcode.getSimulatorRuntimeDescriptors(): List<SimulatorRuntimeDescriptor> = gson.fromJson(simulatorRuntimes, ListRuntimesReport::class.java).runtimes

/**
 * Returns first available simulator runtime for [target] with at least [osMinVersion] OS version.
 * */
fun Xcode.getLatestSimulatorRuntimeFor(target: KonanTarget, osMinVersion: String): SimulatorRuntimeDescriptor? {
    val osName = when (target) {
        KonanTarget.IOS_X64 -> "iOS"
        KonanTarget.WATCHOS_X64, KonanTarget.WATCHOS_X86 -> "watchOS"
        KonanTarget.TVOS_X64 -> "tvOS"
        else -> error("Unexpected simulator target: $target")
    }
    return getSimulatorRuntimeDescriptors().firstOrNull {
        it.checkAvailability() && it.name.startsWith(osName) && compareStringsAsVersions(it.version, osMinVersion) >= 0
    }
}

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
        @Expose val buildversion: String
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
