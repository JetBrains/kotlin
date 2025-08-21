package com.github.jetbrains.myapplication

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice
import platform.UIKit.UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
import platform.UIKit.UIImagePickerController

actual class Platform actual constructor() {
    actual val platform: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    /**
     * Retrieves device information based on the specified information type.
     *
     * This function provides access to various iOS device properties and system information.
     * It can return different types of device data depending on the requested information type.
     *
     * @param infoType The type of information to retrieve. Supported values:
     *   - "model": Returns the device model name (e.g., "iPhone", "iPad")
     *   - "name": Returns the user-assigned device name
     *   - "version": Returns the system version only
     *   - "bundle": Returns the app bundle identifier
     *   - "battery": Returns battery level as a percentage string
     * @param includePrefix Whether to include a descriptive prefix in the returned string.
     *   When true, results are prefixed with labels like "Model: " or "Name: "
     *
     * @return A string containing the requested device information, or "Unknown" if the
     *   information type is not supported or cannot be retrieved.
     *
     * @throws IllegalArgumentException if infoType is null or empty
     *
     * @sample
     * ```kotlin
     * val platform = Platform()
     * val deviceModel = platform.getDeviceInfo("model", false) // Returns "iPhone"
     * val deviceName = platform.getDeviceInfo("name", true)    // Returns "Name: John's iPhone"
     * ```
     */
    fun getDeviceInfo(infoType: String, includePrefix: Boolean = false): String {
        if (infoType.isBlank()) {
            throw IllegalArgumentException("Info type cannot be null or empty")
        }

        val device = UIDevice.currentDevice
        val prefix = if (includePrefix) "${infoType.replaceFirstChar { it.uppercase() }}: " else ""

        return when (infoType.lowercase()) {
            "model" -> "${prefix}${device.model}"
            "name" -> "${prefix}${device.name}"
            "version" -> "${prefix}${device.systemVersion}"
            "bundle" -> "${prefix}${NSBundle.mainBundle.bundleIdentifier ?: "Unknown"}"
            "battery" -> {
                device.batteryMonitoringEnabled = true
                val batteryLevel = (device.batteryLevel * 100).toInt()
                "${prefix}${batteryLevel}%"
            }
            else -> "Unknown"
        }
    }

    /**
     * Checks if the current iOS device supports a specific feature.
     *
     * @param feature The feature name to check. Supported features: "camera", "multitasking", "telephony"
     * @return true if the device supports the specified feature, false otherwise
     */
    fun isFeatureSupported(feature: String): Boolean {
        val device = UIDevice.currentDevice

        return when (feature.lowercase()) {
            "camera" -> UIImagePickerController.isSourceTypeAvailable(
                UIImagePickerControllerSourceTypeCamera
            )
            "multitasking" -> device.multitaskingSupported
            "telephony" -> device.model.contains("iPhone", ignoreCase = true)
            else -> false
        }
    }
}