/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Boots a simulator if it is currently shutdown.
 */
abstract class BootSimulatorTask : AbstractXcodeTask() {
    @get:Input
    abstract val simulatorName: Property<String>

    @TaskAction
    fun boot() {
        // We resolve ID by name for simplicity in this step,
        // or we could pass the ID from the creation task if we wired outputs.
        val name = simulatorName.get()

        // Naive boot command. 'xcrun simctl boot' fails if already booted,
        // so we ignore exit code or check status first.
        try {
            logger.lifecycle("Booting simulator '$name'...")
            execOperations.exec {
                it.commandLine("xcrun", "simctl", "boot", name)
                it.isIgnoreExitValue = true
            }

            // Wait specifically for the service to be ready
            execOperations.exec {
                it.commandLine("xcrun", "simctl", "bootstatus", name)
            }
        } catch (e: Exception) {
            logger.warn("Attempt to boot '$name' finished. It might already be booted.")
        }
    }
}