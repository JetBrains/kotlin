/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import org.gradle.workers.WorkAction
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.swiftexport.standalone.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.SerializationTools
import java.io.File
import java.time.Instant
import java.util.logging.Level
import java.util.logging.Logger

internal abstract class SwiftExportAction : WorkAction<SwiftExportWorkParameters> {

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

        val exportModules = parameters.swiftModules.get().map { module ->
            module.toInputModule(config())
        }.toSet()

        val modules = GradleSwiftExportModules(
            runSwiftExport(exportModules).getOrThrow().toPlainList(),
            Instant.now().toEpochMilli()
        )

        val json = SerializationTools.writeToJson(modules)
        parameters.swiftModulesFile.getFile().writeText(json)
    }

    private fun config(): SwiftExportConfig {
        return SwiftExportConfig(
            settings = mapOf(
                SwiftExportConfig.STABLE_DECLARATIONS_ORDER to parameters.stableDeclarationsOrder.getOrElse(true).toString(),
                SwiftExportConfig.BRIDGE_MODULE_NAME to parameters.bridgeModuleName.getOrElse(SwiftExportConfig.DEFAULT_BRIDGE_MODULE_NAME),
                SwiftExportConfig.RENDER_DOC_COMMENTS to parameters.renderDocComments.getOrElse(false).toString()
            ),
            logger = Companion,
            distribution = parameters.konanDistribution.get(),
            outputPath = parameters.outputPath.getFile().toPath()
        )
    }
}

internal fun Set<SwiftExportModule>.toPlainList(): List<GradleSwiftExportModule> {
    val modules = mutableSetOf<GradleSwiftExportModule>()

    for (module in this) {
        val kgpModule = module.toKGPModule()
        if (kgpModule !in modules) {
            modules.add(kgpModule)
        }
    }

    return modules.toList()
}

private fun SwiftExportedModule.toInputModule(config: SwiftExportConfig): InputModule {
    return InputModule(
        name = moduleName,
        path = artifact.toPath(),
        config = config
    )
}

private fun SwiftExportModule.toKGPModule(): GradleSwiftExportModule {
    return when (this) {
        is SwiftExportModule.BridgesToKotlin ->
            GradleSwiftExportModule.BridgesToKotlin(files.toKGPFiles(), bridgeName, name, dependencies.map { it.name })
        is SwiftExportModule.SwiftOnly ->
            GradleSwiftExportModule.SwiftOnly(swiftApi.toFile(), name, dependencies.map { it.name })
    }
}

private fun SwiftExportFiles.toKGPFiles(): GradleSwiftExportFiles {
    return GradleSwiftExportFiles(swiftApi.toFile(), kotlinBridges.toFile(), cHeaderBridges.toFile())
}
