/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.BRIDGE_MODULE_NAME
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.DEFAULT_BRIDGE_MODULE_NAME
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.RENDER_DOC_COMMENTS
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportConfig.Companion.STABLE_DECLARATIONS_ORDER
import org.jetbrains.kotlin.sir.providers.UnknownTypeStrategy as InternalUnknownTypeStrategy
import org.jetbrains.kotlin.sir.providers.UnsupportedTypeStrategy as InternalUnsupportedTypeStrategy
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildBridgeRequests
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildSwiftModule
import org.jetbrains.kotlin.swiftexport.standalone.writer.dumpResultToFiles
import org.jetbrains.kotlin.utils.KotlinNativePaths
import java.nio.file.Path

public data class SwiftExportConfig(
    val settings: Map<String, String> = emptyMap(),
    val logger: SwiftExportLogger = createDummyLogger(),
    val unsupportedTypeStrategy: UnsupportedTypeStrategy = UnsupportedTypeStrategy.Fail,
    val unknownTypeStrategy: UnknownTypeStrategy = UnknownTypeStrategy.Fail,
    val distribution: Distribution = Distribution(KotlinNativePaths.homePath.absolutePath)
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

/**
 * What should we do when encounter a not yet supported type?
 */
public enum class UnsupportedTypeStrategy {
    Fail,
    SpecialType;

    internal fun toInternalType(): InternalUnsupportedTypeStrategy = when (this) {
        Fail -> InternalUnsupportedTypeStrategy.Fail
        SpecialType -> InternalUnsupportedTypeStrategy.SpecialType
    }
}

/**
 * What should we do when encounter an unknown type (e.g., a type from unknown klib or incomplete declaration in IDE)?
 */
public enum class UnknownTypeStrategy {
    Fail,
    SpecialType;

    internal fun toInternalType(): InternalUnknownTypeStrategy = when (this) {
        Fail -> InternalUnknownTypeStrategy.Fail
        SpecialType -> InternalUnknownTypeStrategy.SpecialType
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
    val stableDeclarationsOrder = config.settings.containsKey(STABLE_DECLARATIONS_ORDER)
    val renderDocComments = config.settings[RENDER_DOC_COMMENTS] != "false"
    val bridgeModuleName = config.settings.getOrElse(BRIDGE_MODULE_NAME) {
        config.logger.report(
            SwiftExportLogger.Severity.Warning,
            "Bridging header is not set. Using $DEFAULT_BRIDGE_MODULE_NAME instead"
        )
        DEFAULT_BRIDGE_MODULE_NAME
    }
    val swiftModule = buildSwiftModule(input, config.distribution, bridgeModuleName)
    val bridgeRequests = buildBridgeRequests(swiftModule)
    swiftModule.dumpResultToFiles(
        bridgeRequests, output,
        stableDeclarationsOrder = stableDeclarationsOrder,
        renderDocComments = renderDocComments
    )
}
