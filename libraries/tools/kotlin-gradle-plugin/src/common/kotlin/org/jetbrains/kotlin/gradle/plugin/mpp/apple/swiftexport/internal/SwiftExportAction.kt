/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.SerializationTools
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.swiftexport.standalone.*
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig
import java.time.Instant
import java.util.logging.Level
import java.util.logging.Logger

internal abstract class SwiftExportAction : WorkAction<SwiftExportAction.SwiftExportWorkParameters> {
    internal interface SwiftExportWorkParameters : SwiftExportTaskParameters, WorkParameters {
        val konanDistribution: Property<Distribution>
    }

    private val swiftExportLogger = object : SwiftExportLogger {
        private val logger = Logger.getLogger(this::class.java.name)

        override fun report(severity: SwiftExportLogger.Severity, message: String) {
            logger.log(severity.logType(), message)
        }

        private fun SwiftExportLogger.Severity.logType(): Level = when (this) {
            SwiftExportLogger.Severity.Info -> Level.INFO
            SwiftExportLogger.Severity.Warning -> Level.WARNING
            SwiftExportLogger.Severity.Error -> Level.SEVERE
        }
    }

    override fun execute() {
        val exportSettings = parameters.swiftExportSettings
        val exportModules = parameters.swiftModules.zip(exportSettings) { modules, settings ->
            modules.map { module ->
                module.toInputModule(
                    createModuleConfig(
                        module.flattenPackage,
                        module.shouldBeFullyExported,
                        settings
                    )
                )
            }.toSet()
        }.get()

        val modules = GradleSwiftExportModules(
            runSwiftExport(exportModules, createSwiftExportConfig()).getOrThrow().toPlainList(),
            Instant.now().toEpochMilli()
        )

        val json = SerializationTools.writeToJson(modules)
        parameters.swiftModulesFile.getFile().writeText(json)
    }

    private fun createModuleConfig(
        flattenPackage: String?,
        shouldBeFullyExported: Boolean,
        settings: Map<String, String>,
    ): SwiftModuleConfig {
        return SwiftModuleConfig(
            bridgeModuleName = parameters.bridgeModuleName.getOrElse(SwiftModuleConfig.DEFAULT_BRIDGE_MODULE_NAME),
            rootPackage = flattenPackage,
            experimentalFeatures = settings,
            shouldBeFullyExported = shouldBeFullyExported,
        )
    }

    private fun createSwiftExportConfig(): SwiftExportConfig {
        return SwiftExportConfig(
            outputPath = parameters.outputPath.getFile().toPath(),
            stableDeclarationsOrder = parameters.stableDeclarationsOrder.getOrElse(true),
            distribution = parameters.konanDistribution.get(),
            renderDocComments = parameters.renderDocComments.getOrElse(false),
            logger = swiftExportLogger,
            konanTarget = parameters.konanTarget.get(),
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

private fun SwiftExportedModule.toInputModule(config: SwiftModuleConfig): InputModule {
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
