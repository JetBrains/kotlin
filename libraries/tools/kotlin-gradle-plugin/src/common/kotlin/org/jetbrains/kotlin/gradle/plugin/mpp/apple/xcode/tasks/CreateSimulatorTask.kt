/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.tasks

import com.google.gson.Gson
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * Ensures a simulator exists with the specified configuration.
 * If it exists, it does nothing. If not, it creates it.
 */
abstract class CreateSimulatorTask : AbstractXcodeTask() {
    @get:Input
    abstract val deviceName: Property<String>

    @get:Input
    abstract val deviceType: Property<String>

    @get:Input
    abstract val osVersion: Property<String>

    @get:Internal
    val simulatorId: Property<String> = project.objects.property(String::class.java)

    @TaskAction
    fun create() {
        val name = deviceName.get()

        // 1. Check if exists
        val listJson = runCommand("xcrun", "simctl", "list", "devices", "--json")

        // Use Gson to parse the JSON output
        val json = Gson().fromJson(listJson, Map::class.java)
        val devices = json["devices"] as? Map<*, *> ?: emptyMap<Any, Any>()

        var existingId: String? = null

        // Parse the nested JSON structure: Runtimes -> Devices list
        devices.forEach { (_, deviceList) ->
            (deviceList as List<*>).forEach { device ->
                val d = device as Map<*, *>
                if (d["name"] == name) {
                    existingId = d["udid"] as String
                }
            }
        }

        if (existingId != null) {
            logger.lifecycle("Simulator '$name' already exists with ID: $existingId")
            simulatorId.set(existingId)
        } else {
            logger.lifecycle("Creating simulator '$name'...")
            // Construct runtime ID (e.g., com.apple.CoreSimulator.SimRuntime.iOS-17-2)
            // Note: This is a simplified mapping. Real implementation might need exact lookup.
            val runtimeId = "com.apple.CoreSimulator.SimRuntime.${osVersion.get().replace(" ", "-").replace(".", "-")}"
            val typeId = "com.apple.CoreSimulator.SimDeviceType.${deviceType.get().replace(" ", "-")}"

            val newId = runCommand("xcrun", "simctl", "create", name, typeId, runtimeId)
            logger.lifecycle("Created simulator with ID: $newId")
            simulatorId.set(newId)
        }
    }
}