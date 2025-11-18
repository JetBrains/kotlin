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
 * Runs xcodebuild to compile the app.
 */
abstract class BuildXcodeTask : AbstractXcodeTask() {
    @get:InputDirectory
    abstract val projectPath: DirectoryProperty

    @get:Input
    abstract val scheme: Property<String>

    @get:Input
    abstract val configuration: Property<String>

    @get:Input
    abstract val destination: Property<String>

    @TaskAction
    fun build() {
        logger.lifecycle("Building Xcode project for destination: ${destination.get()}")

        runCommand(
            "xcodebuild",
            "-project", projectPath.get().asFile.absolutePath,
            "-scheme", scheme.get(),
            "-configuration", configuration.get(),
            "-destination", destination.get(),
            "-derivedDataPath", project.layout.buildDirectory.dir("xcodeDerivedData").getFile().absolutePath,
            "clean", "build"
        )
    }
}