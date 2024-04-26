/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.sir.SirImport
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.utils.updateImports
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.BRIDGE_MODULE_NAME
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.DEFAULT_BRIDGE_MODULE_NAME
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.RENDER_DOC_COMMENTS
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.STABLE_DECLARATIONS_ORDER
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildBridgeRequests
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildSwiftModule
import org.jetbrains.kotlin.swiftexport.standalone.writer.dumpResultToFiles
import org.jetbrains.kotlin.utils.KotlinNativePaths
import java.nio.file.Path
import kotlin.io.path.div

public data class SwiftExportConfig(
    val settings: Map<String, String> = emptyMap(),
    val outputPath: Path,
    val logger: SwiftExportLogger = createDummyLogger(),
    val distribution: Distribution = Distribution(KotlinNativePaths.homePath.absolutePath),
    val errorTypeStrategy: ErrorTypeStrategy = ErrorTypeStrategy.Fail,
    val unsupportedTypeStrategy: ErrorTypeStrategy = ErrorTypeStrategy.Fail,
) {
    public companion object {
        /**
         * How should the generated stubs refer to C bridging module?
         * ```swift
         * import $BRIDGE_MODULE_NAME
         * ...
         * ```
         */
        public const val BRIDGE_MODULE_NAME: String = "BRIDGE_MODULE_NAME"

        public const val DEFAULT_BRIDGE_MODULE_NAME: String = "KotlinBridges"

        public const val STABLE_DECLARATIONS_ORDER: String = "STABLE_DECLARATIONS_ORDER"

        public const val RENDER_DOC_COMMENTS: String = "RENDER_DOC_COMMENTS"

        public const val ROOT_PACKAGE: String = "rootPackage"
    }
}

public enum class ErrorTypeStrategy {
    Fail,
    SpecialType;

    internal fun toInternalType(): SirTypeProvider.ErrorTypeStrategy = when (this) {
        Fail -> SirTypeProvider.ErrorTypeStrategy.Fail
        SpecialType -> SirTypeProvider.ErrorTypeStrategy.ErrorType
    }
}

public sealed interface InputModule {
    public val name: String
    public val path: Path

    public class Source(
        override val name: String,
        override val path: Path,
    ): InputModule

    public class Binary(
        override val name: String,
        override val path: Path,
    ) : InputModule
}

public data class SwiftExportModule(
    val name: String,
    val files: SwiftExportFiles,
    val dependencies: List<SwiftExportModule>,
)

public data class SwiftExportFiles(
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

@Deprecated(message = "This method will be removed in a future version")
public fun runSwiftExport(
    input: InputModule,
    config: SwiftExportConfig,
    output: SwiftExportFiles,
) {
    val stableDeclarationsOrder = config.settings.containsKey(STABLE_DECLARATIONS_ORDER)
    val renderDocComments = config.settings[RENDER_DOC_COMMENTS] != "false"
    val bridgeModuleName = config.settings.getOrElse(BRIDGE_MODULE_NAME) {
        config.logger.report(
            SwiftExportLogger.Severity.Warning,
            "Bridging header is not set. Using $DEFAULT_BRIDGE_MODULE_NAME instead"
        )
        DEFAULT_BRIDGE_MODULE_NAME
    }
    val swiftModule = buildSwiftModule(input, config)
    val bridgeRequests = buildBridgeRequests(swiftModule)
    if (bridgeRequests.isNotEmpty()) {
        swiftModule.updateImports(listOf(SirImport(bridgeModuleName)))
    }
    swiftModule.dumpResultToFiles(
        bridgeRequests, output,
        stableDeclarationsOrder = stableDeclarationsOrder,
        renderDocComments = renderDocComments
    )
}

/**
 * A root function for running Swift Export from build tool
 */
@Suppress("DEPRECATION")
public fun runSwiftExport(
    input: InputModule,
    config: SwiftExportConfig,
): Result<List<SwiftExportModule>> {
    val output = SwiftExportFiles(
        swiftApi = config.outputPath / "${input.name}.swift",
        kotlinBridges = config.outputPath / "${input.name}.kt",
        cHeaderBridges = config.outputPath / "${input.name}.h"
    )

    return runCatching {
        runSwiftExport(
            input,
            config,
            output
        )
        listOf(
            SwiftExportModule(
                name = input.name,
                files = output,
                dependencies = emptyList(),
            )
        )
    }
}
