/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import org.jetbrains.kotlin.gradle.utils.appendLine
import java.io.File

/**
 * Generates a def-file for the given CocoaPods dependency.
 */
abstract class DefFileTask : DefaultTask() {

    @get:Nested
    abstract val pod: Property<CocoapodsDependency>

    @get:Input
    abstract val useLibraries: Property<Boolean>

    @get:OutputFile
    val outputFile: File
        get() = project.cocoapodsBuildDirs.defs.resolve("${pod.get().moduleName}.def")

    @TaskAction
    fun generate() {
        outputFile.parentFile.mkdirs()
        outputFile.writeText(buildString {
            appendLine("language = Objective-C")
            with(pod.get()) {
                when {
                    headers != null -> appendLine("headers = $headers")
                    useLibraries.get() -> logger.warn(
                        """
                        w: Pod '$moduleName' should have 'headers' property specified when using 'useLibraries()'.
                        Otherwise code from this pod won't be accessible from Kotlin.
                        """.trimIndent()
                    )
                    else -> {
                        appendLine("modules = $moduleName")

                        // Linker opt with framework name is added so produced cinterop klib would have this flag inside its manifest
                        // This way error will be more obvious when someone will try to depend on a library with this cinterop
                        appendLine("linkerOpts = -framework $moduleName")
                    }
                }
            }
        })
    }
}