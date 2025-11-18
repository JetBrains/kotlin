/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.utils.getFile

/**
 * Installs the built .app onto the simulator.
 */
abstract class RunXcodeTask : AbstractXcodeTask() {
    @get:Input
    abstract val deviceIdOrName: Property<String>

    @get:Input
    abstract val bundleId: Property<String>

    @get:InputDirectory
    abstract val buildDir: DirectoryProperty

    @get:Input
    abstract val scheme: Property<String>

    @TaskAction
    fun run() {
        // 1. Find the .app path.
        // In a real plugin, we would parse the build settings or use a fixed output path.
        // Assuming standard DerivedData structure for this example:
        val appPath = buildDir.dir("xcodeDerivedData/Build/Products/Debug-iphonesimulator/${scheme.get()}.app").getFile().absolutePath

        logger.lifecycle("Installing app: $appPath")
        runCommand("xcrun", "simctl", "install", deviceIdOrName.get(), appPath)

        logger.lifecycle("Launching app: ${bundleId.get()}")
        runCommand("xcrun", "simctl", "launch", "--console-pty", deviceIdOrName.get(), bundleId.get())
    }
}