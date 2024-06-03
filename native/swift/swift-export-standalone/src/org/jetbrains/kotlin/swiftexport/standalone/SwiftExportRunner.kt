/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.sir.SirImport
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.bridge.SirTypeNamer
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.utils.updateImports
import org.jetbrains.kotlin.sir.bridge.createBridgeGenerator
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.SilentUnsupportedDeclarationReporter
import org.jetbrains.kotlin.sir.providers.utils.SimpleUnsupportedDeclarationReporter
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter
import org.jetbrains.kotlin.sir.util.swiftName
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.BRIDGE_MODULE_NAME
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.DEFAULT_BRIDGE_MODULE_NAME
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.RENDER_DOC_COMMENTS
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.STABLE_DECLARATIONS_ORDER
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildBridgeRequests
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildSwiftModule
import org.jetbrains.kotlin.swiftexport.standalone.writer.dumpResultToFiles
import org.jetbrains.kotlin.utils.KotlinNativePaths
import java.io.Serializable
import java.nio.file.Path
import kotlin.io.path.div

public data class SwiftExportConfig(
    val settings: Map<String, String> = emptyMap(),
    val outputPath: Path,
    val logger: SwiftExportLogger = createDummyLogger(),
    val distribution: Distribution = Distribution(KotlinNativePaths.homePath.absolutePath),
    val errorTypeStrategy: ErrorTypeStrategy = ErrorTypeStrategy.Fail,
    val unsupportedTypeStrategy: ErrorTypeStrategy = ErrorTypeStrategy.Fail,
    val unsupportedDeclarationReporterKind: UnsupportedDeclarationReporterKind = UnsupportedDeclarationReporterKind.Silent,
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

        public const val ROOT_PACKAGE: String = "packageRoot"
    }
}

public enum class UnsupportedDeclarationReporterKind {
    Silent, Inline;

    internal fun toReporter(): UnsupportedDeclarationReporter = when (this) {
        Silent -> SilentUnsupportedDeclarationReporter
        Inline -> SimpleUnsupportedDeclarationReporter()
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

    public class Binary(
        override val name: String,
        override val path: Path,
    ) : InputModule
}

public data class SwiftExportModule(
    val name: String,
    val files: SwiftExportFiles,
    val dependencies: List<SwiftExportModule>,
) : Serializable

public data class SwiftExportFiles(
    val swiftApi: Path,
    val kotlinBridges: Path,
    val cHeaderBridges: Path,
) : Serializable

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
    input: InputModule.Binary,
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
    val unsupportedDeclarationReporter = config.unsupportedDeclarationReporterKind.toReporter()
    val buildResult = buildSwiftModule(input, config, unsupportedDeclarationReporter)
    val bridgeGenerator = createBridgeGenerator(object : SirTypeNamer {
        override fun swiftFqName(type: SirType): String = type.swiftName
        override fun kotlinFqName(type: SirType): String {
            require(type is SirNominalType)
            return ((type.type.origin as KotlinSource).symbol as KaClassLikeSymbol).classId!!.asFqNameString()
        }
    })
    val bridgeRequests = buildBridgeRequests(bridgeGenerator, buildResult.mainModule)
    if (bridgeRequests.isNotEmpty()) {
        buildResult.mainModule.updateImports(listOf(SirImport(bridgeModuleName)))
    }
    val additionalSwiftLinesProvider = if (unsupportedDeclarationReporter is SimpleUnsupportedDeclarationReporter) {
        // Lazily call after SIR printer to make sure that all declarations are collected.
        { unsupportedDeclarationReporter.messages.map { "// $it" } }
    } else {
        { emptyList() }
    }
    dumpResultToFiles(
        sirModules = listOf(buildResult.mainModule, buildResult.moduleForPackageEnums),
        bridgeGenerator = bridgeGenerator,
        requests = bridgeRequests,
        output = output,
        stableDeclarationsOrder = stableDeclarationsOrder,
        renderDocComments = renderDocComments,
        additionalSwiftLinesProvider = additionalSwiftLinesProvider,
    )
}

/**
 * A root function for running Swift Export from build tool
 */
@Suppress("DEPRECATION")
public fun runSwiftExport(
    input: InputModule.Binary,
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
