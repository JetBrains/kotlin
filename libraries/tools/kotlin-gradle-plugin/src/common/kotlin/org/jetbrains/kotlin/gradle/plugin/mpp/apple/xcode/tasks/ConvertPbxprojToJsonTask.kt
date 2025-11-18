/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import javax.inject.Inject

/**
 * Producer Task: Converts the `.pbxproj` file to JSON using `plutil`.
 * This task is fully cacheable.
 */
@CacheableTask
internal abstract class ConvertPbxprojToJsonTask : DefaultTask() {
    init {
        onlyIf("Task can only run on macOS") { HostManager.hostIsMac }
    }

    companion object {
        const val TASK_NAME = "convertPbxprojToJson"
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val pbxprojFile: RegularFileProperty

    @get:OutputFile
    abstract val jsonFile: RegularFileProperty

    @TaskAction
    fun run() {
        try {
            execOperations.exec { spec ->
                spec.commandLine(
                    "plutil",
                    "-convert", "json",
                    "-o", jsonFile.get().asFile.absolutePath,
                    pbxprojFile.get().asFile.absolutePath
                )
            }
        } catch (exception: Exception) {
            logger.error(
                "Failed to execute 'plutil' on '${pbxprojFile.get().asFile.path}'. The file might be malformed or 'plutil' is not in PATH.",
                exception
            )
            jsonFile.get().asFile.writeText("{}") // Write empty JSON on failure
        }
    }
}