/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import org.gradle.workers.WorkAction
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.swiftexport.standalone.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.SerializationTools
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
            input = InputModule.Binary(
                name = parameters.swiftApiModuleName.get(),
                path = parameters.kotlinLibraryFile.getFile().toPath()
            ),
            config = SwiftExportConfig(
                settings = mapOf(
                    SwiftExportConfig.STABLE_DECLARATIONS_ORDER to parameters.stableDeclarationsOrder.getOrElse(true).toString(),
                    SwiftExportConfig.BRIDGE_MODULE_NAME to parameters.bridgeModuleName.getOrElse(SwiftExportConfig.DEFAULT_BRIDGE_MODULE_NAME),
                    SwiftExportConfig.RENDER_DOC_COMMENTS to parameters.renderDocComments.getOrElse(false).toString(),
                ),
                logger = Companion,
                distribution = parameters.konanDistribution.get(),
                outputPath = parameters.outputPath.getFile().toPath()
            )
        ).apply {
            val modules = getOrThrow()
            val path = parameters.swiftModulesFile.getFile().canonicalPath

            SerializationTools.writeToJson(modules, path)
        }
    }
}