/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.internal.LogType
import org.jetbrains.kotlin.gradle.internal.processLogMessage
import org.jetbrains.kotlin.swiftexport.standalone.*

@DisableCachingByDefault(because = "Swift Export is experimental, so no caching for now")
abstract class SwiftExportTask : DefaultTask(), SwiftExportLogger {

    @get:Input
    abstract val bridgeModuleName: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceRoots: ConfigurableFileCollection

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
                sourceRoots = sourceRoots.asFileTree.files.map { it.toPath() }
            ),
            config = SwiftExportConfig(
                settings = mapOf(
                    SwiftExportConfig.DEBUG_MODE_ENABLED to "true",
                    SwiftExportConfig.BRIDGE_MODULE_NAME to bridgeModuleName.getOrElse(SwiftExportConfig.DEFAULT_BRIDGE_MODULE_NAME),
                ),
                this
            ),
            output = SwiftExportOutput(
                swiftApi = swiftApiPath.get().asFile.toPath(),
                kotlinBridges = kotlinBridgePath.get().asFile.toPath(),
                cHeaderBridges = headerBridgePath.get().asFile.toPath(),
            )
        )
    }

    override fun report(severity: SwiftExportLogger.Severity, message: String) {
        logger.processLogMessage(message, severity.logType())
    }
}

private fun SwiftExportLogger.Severity.logType(): LogType = when (this) {
    SwiftExportLogger.Severity.Info -> LogType.INFO
    SwiftExportLogger.Severity.Warning -> LogType.WARN
    SwiftExportLogger.Severity.Error -> LogType.ERROR
    else -> LogType.LOG
}