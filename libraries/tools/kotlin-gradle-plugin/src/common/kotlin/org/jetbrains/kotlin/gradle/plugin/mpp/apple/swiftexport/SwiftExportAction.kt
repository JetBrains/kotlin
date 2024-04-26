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

    @Suppress("DEPRECATION")
    override fun execute() {
        runSwiftExport(
            input = InputModule.Source(
                name = parameters.swiftApiModuleName.get(),
                path = parameters.sourceRoot.getFile().toPath()
            ),
            config = SwiftExportConfig(
                settings = mapOf(
                    SwiftExportConfig.STABLE_DECLARATIONS_ORDER to parameters.debugMode.getOrElse(false).toString(),
                    SwiftExportConfig.BRIDGE_MODULE_NAME to parameters.bridgeModuleName.getOrElse(SwiftExportConfig.DEFAULT_BRIDGE_MODULE_NAME),
                ),
                logger = Companion,
                distribution = parameters.konanDistribution.get(),
                outputPath = parameters.swiftApiPath.getFile().toPath(), // just a placeholder
            ),
            output = SwiftExportFiles(
                swiftApi = parameters.swiftApiPath.getFile().toPath(),
                kotlinBridges = parameters.kotlinBridgePath.getFile().toPath(),
                cHeaderBridges = parameters.headerBridgePath.getFile().toPath(),
            )
        )
    }
}