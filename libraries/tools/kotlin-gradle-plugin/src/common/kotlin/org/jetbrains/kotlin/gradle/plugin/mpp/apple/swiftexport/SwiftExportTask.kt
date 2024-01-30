/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.sir.runner.SwiftExportConfig
import org.jetbrains.kotlin.sir.runner.SwiftExportInput
import org.jetbrains.kotlin.sir.runner.SwiftExportOutput
import org.jetbrains.kotlin.sir.runner.runSwiftExport
import java.nio.file.Paths


abstract class SwiftExportTask : DefaultTask() {

    @get:Input
    abstract val sourcesRoots: ListProperty<String>

    @get:OutputFile
    abstract val swiftApiPath: RegularFileProperty

    @get:OutputFile
    abstract val headerBridgePath: RegularFileProperty

    @get:OutputFile
    abstract val kotlinBridgePath: RegularFileProperty

    @TaskAction
    fun run() {
        runSwiftExport(
            input = SwiftExportInput(
                sourceRoots = sourcesRoots.get().map { Paths.get(it) },
            ),
            config = SwiftExportConfig(
                settings = emptyMap(),
            ),
            output = SwiftExportOutput(
                swiftApi = swiftApiPath.get().asFile.toPath(),
                kotlinBridges = kotlinBridgePath.get().asFile.toPath(),
                cHeaderBridges = headerBridgePath.get().asFile.toPath(),
            )
        )
    }

}