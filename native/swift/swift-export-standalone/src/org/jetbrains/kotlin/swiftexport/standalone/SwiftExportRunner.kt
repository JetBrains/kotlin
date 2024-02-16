/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.BRIDGE_MODULE_NAME
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.DEBUG_MODE_ENABLED
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.DEFAULT_BRIDGE_MODULE_NAME
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildFunctionBridges
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildSwiftModule
import org.jetbrains.kotlin.swiftexport.standalone.transformation.transformToSwift
import org.jetbrains.kotlin.swiftexport.standalone.writer.dumpResultToFiles
import org.jetbrains.kotlin.utils.KotlinNativePaths
import java.nio.file.Path

public data class SwiftExportConfig(
    val settings: Map<String, String> = emptyMap(),
    val logger: SwiftExportLogger = createDummyLogger(),
    val distribution: Distribution = Distribution(KotlinNativePaths.homePath.absolutePath)
) {
    public companion object {
        public const val DEBUG_MODE_ENABLED: String = "DEBUG_MODE_ENABLED"

        /**
         * How should the generated stubs refer to C bridging module?
         * ```swift
         * import $BRIDGE_MODULE_NAME
         * ...
         * ```
         */
        public const val BRIDGE_MODULE_NAME: String = "BRIDGE_MODULE_NAME"

        public const val DEFAULT_BRIDGE_MODULE_NAME: String = "KotlinBridges"
    }
}

public data class SwiftExportInput(
    val sourceRoot: Path, // todo: we do not support multi-modules currently. see KT-65220
    val libraries: List<Path> = emptyList(), // todo: not supported currently. see KT-65221
)

public data class SwiftExportOutput(
    val swiftApi: Path,
    val kotlinBridges: Path,
    val cHeaderBridges: Path,
)

/**
 * Trivial logging interface that should be implemented
 * by the environment to report messages from Swift export.
 */
public interface SwiftExportLogger {
    public enum class Severity {
        Info,
        Warning,
        Error,
    }

    public fun report(severity: Severity, message: String)
}

/**
 * Primitive implementation of [SwiftExportLogger] which should be sufficient for testing purposes.
 */
public fun createDummyLogger(): SwiftExportLogger = object : SwiftExportLogger {
    override fun report(severity: SwiftExportLogger.Severity, message: String) {
        println("$severity: $message")
    }
}

/**
 * A root function for running Swift Export from build tool
 */
public fun runSwiftExport(
    input: SwiftExportInput,
    config: SwiftExportConfig = SwiftExportConfig(),
    output: SwiftExportOutput,
) {
    val isDebugModeEnabled = config.settings.containsKey(DEBUG_MODE_ENABLED)
    val bridgeModuleName = config.settings.getOrElse(BRIDGE_MODULE_NAME) {
        config.logger.report(
            SwiftExportLogger.Severity.Warning,
            "Bridging header is not set. Using $DEFAULT_BRIDGE_MODULE_NAME instead"
        )
        DEFAULT_BRIDGE_MODULE_NAME
    }


    val module = buildSwiftModule(
        input,
        config.distribution,
        isDebugModeEnabled,
        bridgeModuleName
    )
        .transformToSwift()
    val bridgeRequests = module.buildFunctionBridges()
    module.dumpResultToFiles(bridgeRequests, output)
}
