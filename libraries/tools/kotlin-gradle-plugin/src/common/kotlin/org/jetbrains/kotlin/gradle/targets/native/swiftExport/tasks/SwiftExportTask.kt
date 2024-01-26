/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.swiftExport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.sir.runner.SwiftExportInput
import org.jetbrains.kotlin.sir.runner.SwiftExportOutput
import org.jetbrains.kotlin.sir.runner.runSwiftExport
import javax.inject.Inject

internal val KotlinSwiftExportSetupAction = KotlinProjectSetupCoroutine {
    locateOrRegisterSwiftExportTask()
}

@DisableCachingByDefault
abstract class SwiftExportTask @Inject constructor(
    layout: ProjectLayout
) : DefaultTask() {

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val moduleRoot: DirectoryProperty

    @get:OutputDirectory
    abstract val swiftOutput: DirectoryProperty

    @TaskAction
    open fun swiftExport() {
        logger.info("🧪 Run Swift Export")

        val output = SwiftExportOutput(
            swiftApi = swiftOutput.get().asFile.resolve("result.swift").toPath(),
            kotlinBridges = swiftOutput.get().asFile.resolve("result.kt").toPath(),
            cHeaderBridges = swiftOutput.get().asFile.resolve("result.c").toPath()
        )

        runSwiftExport(
            input = SwiftExportInput(
                sourceRoot = moduleRoot.get().asFile.toPath(),
            ),
            output = output
        )
    }
}

internal fun Project.locateOrRegisterSwiftExportTask(): TaskProvider<SwiftExportTask> {
    return locateOrRegisterTask("swiftExport") { task ->
        //FIXME: For test purpose only. Create a directory 'swiftexportInput' in your module and put '.kt' file in it
        val resourcesDir = layout.projectDirectory.dir("swiftexportInput")
        val swiftOutput = layout.buildDirectory.dir("swiftexport")

        task.description = "Swift Export prototype task"
        task.group = "swift export"
        task.moduleRoot.set(resourcesDir)
        task.swiftOutput.set(swiftOutput)
    }
}