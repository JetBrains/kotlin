/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.BRIDGE_MODULE_NAME
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.DEBUG_MODE_ENABLED
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.DEFAULT_BRIDGE_MODULE_NAME
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.SKIP_DOC_RENDERING
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.SORT_DECLARATIONS
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildSwiftModule
import org.jetbrains.kotlin.swiftexport.standalone.builders.collectBridgeRequests
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

        public const val SKIP_DOC_RENDERING: String = "SKIP_DOC_RENDERING"
        public const val SORT_DECLARATIONS: String = "SORT_DECLARATIONS"
    }
}

public sealed interface InputModule {
    public val name: String
    public val path: Path
    public val dependencies: List<InputModule>

    public data class SourceModule(
        override val name: String = "main",
        override val path: Path,
        override val dependencies: List<InputModule> = emptyList(), // todo: not supported currently. see KT-65221
    ) : InputModule

    public data class BinaryModule(
        override val name: String = "main",
        override val path: Path,
        override val dependencies: List<InputModule> = emptyList(),
    ) : InputModule
}

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
    input: InputModule,
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

    val skipDocComments = config.settings.containsKey(SKIP_DOC_RENDERING)
    val sortDeclarations = config.settings.containsKey(SORT_DECLARATIONS)

    val module = buildSwiftModule(
        input,
        config.distribution,
        isDebugModeEnabled,
        bridgeModuleName
    )
    val bridgeRequests = collectBridgeRequests(module)
    module.dumpResultToFiles(bridgeRequests, sortDeclarations = sortDeclarations, skipDocComments = skipDocComments, output = output)
}
