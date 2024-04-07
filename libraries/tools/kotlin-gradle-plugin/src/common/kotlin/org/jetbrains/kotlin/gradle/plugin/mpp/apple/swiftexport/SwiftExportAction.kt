/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.workers.WorkAction
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.swiftexport.standalone.*
import java.util.logging.Level
import java.util.logging.Logger

internal abstract class SwiftExportAction : WorkAction<SwiftExportParameters> {

    companion object : SwiftExportLogger {
        private val logger = Logger.getLogger(this::class.java.name)

        override fun report(severity: SwiftExportLogger.Severity, message: String) {
            logger.log(severity.logType(), message)
        }

        private fun SwiftExportLogger.Severity.logType(): Level = when (this) {
            SwiftExportLogger.Severity.Info -> Level.INFO
            SwiftExportLogger.Severity.Warning -> Level.WARNING
            SwiftExportLogger.Severity.Error -> Level.SEVERE
            else -> Level.INFO
        }
    }

    override fun execute() {
        runSwiftExport(
            input = SwiftExportInput(
                moduleName = parameters.swiftApiModuleName.get(),
                sourceRoot = parameters.sourceRoot.getFile().toPath()
            ),
            config = SwiftExportConfig(
                settings = mapOf(
                    SwiftExportConfig.DEBUG_MODE_ENABLED to parameters.debugMode.getOrElse(false).toString(),
                    SwiftExportConfig.BRIDGE_MODULE_NAME to parameters.bridgeModuleName.getOrElse(SwiftExportConfig.DEFAULT_BRIDGE_MODULE_NAME),
                ),
                Companion,
                distribution = parameters.konanDistribution.get(),
            ),
            output = SwiftExportOutput(
                swiftApi = parameters.swiftApiPath.getFile().toPath(),
                kotlinBridges = parameters.kotlinBridgePath.getFile().toPath(),
                cHeaderBridges = parameters.headerBridgePath.getFile().toPath(),
            )
        )
    }
}