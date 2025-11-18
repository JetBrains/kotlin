/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.addExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.XcodeExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.tasks.BootSimulatorTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.tasks.BuildXcodeTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.tasks.CreateSimulatorTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.tasks.ListXcodeInfoTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.tasks.RunXcodeTask
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask

internal object XcodeDSLConstants {
    const val XCODE_DSL_EXTENSION_NAME = "xcode"
    const val TASK_GROUP = "Xcode"
}

internal val XcodeDSLSetupAction = KotlinProjectSetupCoroutine {
    val xcodeExtension = objects.newInstance(XcodeExtension::class.java)

    multiplatformExtension.addExtension(
        XcodeDSLConstants.XCODE_DSL_EXTENSION_NAME,
        xcodeExtension
    )

    val targets = multiplatformExtension
        .awaitTargets()
        .filterIsInstance<KotlinNativeTarget>()
        .filter { it.konanTarget.family.isAppleFamily }

    if (targets.isEmpty()) return@KotlinProjectSetupCoroutine

    val hasBinaries = targets.flatMap { it.binaries }.filterIsInstance<Framework>().isNotEmpty()
    if (!hasBinaries) return@KotlinProjectSetupCoroutine

    registerXcodePipeline(xcodeExtension)
}

private fun Project.registerXcodePipeline(
    extension: XcodeExtension,
) {
    // 1. Register Global Lifecycle Tasks
    locateOrRegisterTask<ListXcodeInfoTask>("listXcodeDestinations")

    // 2. Process Simulators dynamically
    extension.simulators.all { simulator ->
        val capitalizedName = simulator.name.replaceFirstChar { it.uppercase() }

        // Task: Create Simulator (Idempotent)
        val createTask = locateOrRegisterTask<CreateSimulatorTask>("createSimulator$capitalizedName") { task ->
            task.group = XcodeDSLConstants.TASK_GROUP
            task.description = "Creates the '${simulator.name}' simulator if missing."
            task.deviceName.set(simulator.name)
            task.deviceType.set(simulator.deviceType)
            task.osVersion.set(simulator.osVersion)
        }

        // Task: Boot Simulator
        val bootTask = locateOrRegisterTask<BootSimulatorTask>("bootSimulator$capitalizedName") { task ->
            task.group = XcodeDSLConstants.TASK_GROUP
            task.description = "Boots the '${simulator.name}' simulator."
            task.simulatorName.set(simulator.name)
            task.dependsOn(createTask)
        }

        // Task: Build App for this Simulator
        val buildTask = locateOrRegisterTask<BuildXcodeTask>("buildXcode$capitalizedName") { task ->
            task.group = XcodeDSLConstants.TASK_GROUP
            task.description = "Builds the iOS app for ${simulator.name}."

            task.projectPath.set(extension.projectPath)
            task.scheme.set(extension.scheme)
            task.configuration.set(extension.configuration)
            // Construct destination string for xcodebuild
            task.destination.set(project.provider {
                "platform=iOS Simulator,name=${simulator.name},OS=${simulator.osVersion.get()}"
            })

            // Ensure simulator exists before building (optional, but good for 'run')
            task.dependsOn(createTask)
        }

        // Task: Run App on Simulator
        locateOrRegisterTask<RunXcodeTask>("runXcode$capitalizedName") { task ->
            task.group = XcodeDSLConstants.TASK_GROUP
            task.description = "Installs and runs the app on ${simulator.name}."

            task.deviceIdOrName.set(simulator.name)
            // Note: In a real plugin, bundleId should be parsed from Info.plist
            task.bundleId.set(project.provider { "com.example.${extension.scheme.get()}" })
            task.scheme.set(extension.scheme)
            task.buildDir.set(project.layout.buildDirectory)

            task.dependsOn(bootTask, buildTask)
        }
    }

    // 3. Process Physical Devices (Simplified flow - no creation/booting)
    extension.devices.all { device ->
        val capitalizedName = device.name.replaceFirstChar { it.uppercase() }

        locateOrRegisterTask<BuildXcodeTask>("buildXcode$capitalizedName") { task ->
            task.group = XcodeDSLConstants.TASK_GROUP
            task.projectPath.set(extension.projectPath)
            task.scheme.set(extension.scheme)
            task.configuration.set(extension.configuration)
            task.destination.set(device.udid.map { "platform=iOS,id=$it" })
        }
    }
}