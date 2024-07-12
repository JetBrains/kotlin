/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.sir.SirImport
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.bridge.SirTypeNamer
import org.jetbrains.kotlin.sir.bridge.createBridgeGenerator
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.*
import org.jetbrains.kotlin.sir.util.SirSwiftModule
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
    val multipleModulesHandlingStrategy: MultipleModulesHandlingStrategy = MultipleModulesHandlingStrategy.OneToOneModuleMapping,
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

public enum class MultipleModulesHandlingStrategy {
    OneToOneModuleMapping, IntoSingleModule;
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

public data class InputModule(
    public val name: String,
    public val path: Path,
    public val config: SwiftExportConfig,
)

public sealed class SwiftExportModule(
    public val name: String,
    public val dependencies: List<Reference>
) : Serializable {

    public class Reference(
        public val name: String
    ) {
        public lateinit var module: SwiftExportModule
    }

    // used by packages module only
    public class SwiftOnly(
        public val swiftApi: Path,
        name: String,
    ) : SwiftExportModule(name, emptyList()) {
        override fun equals(other: Any?): Boolean =
            other is SwiftOnly && swiftApi == other.swiftApi && name == other.name

        override fun hashCode(): Int {
            var result = swiftApi.hashCode()
            result = 31 * result + name.hashCode()
            return result
        }
    }

    public class BridgesToKotlin(
        public val files: SwiftExportFiles,
        public val bridgeName: String,
        name: String,
        dependencies: List<Reference>,
    ) : SwiftExportModule(name, dependencies)
}

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

/**
 * A root function for running Swift Export from build tool
 */
public fun runSwiftExport(
    input: Set<InputModule>,
): Result<Set<SwiftExportModule>> = runCatching {
    // ATTENTION:
    // 1. Each call to `actualRunSwiftExport` will end with a write operation of contents of this module onto file-system.
    // 2. Each call to `actualRunSwiftExport` will modify this module AND there is no synchronization mechanism inplace.
    val moduleForPackages = buildModule {
        name = "ExportedKotlinPackages"
    }

    val swiftExportOutputs = input.flatMap { rootModule ->
        /**
         * This value represents dependencies of current module.
         * The actual dependency graph is unknown at this point - there is only an array of modules to translate. This particular value
         * will be used to initialize Analysis API session. It is an error to pass module as a dependency to itself - therefor there is
         * a need to remove the current translation module from the list of dependencies.
         */
        val dependencies = input - rootModule
        actualRunSwiftExport(
            input = rootModule,
            dependencies = dependencies,
            moduleForPackages = moduleForPackages,
        ).getOrThrow()
    }.toSet()

    swiftExportOutputs.forEach { realModule ->
        realModule.dependencies.forEach { dep ->
            dep.module = swiftExportOutputs.first { it.name == dep.name }
        }
    }

    return@runCatching swiftExportOutputs
}


private fun actualRunSwiftExport(
    input: InputModule,
    dependencies: Set<InputModule>,
    moduleForPackages: SirModule,
): Result<List<SwiftExportModule>> = runCatching {
    val config = input.config
    val stableDeclarationsOrder = config.settings.containsKey(STABLE_DECLARATIONS_ORDER)
    val renderDocComments = config.settings[RENDER_DOC_COMMENTS] != "false"
    val bridgeModuleNamePrefix = config.settings.getOrElse(BRIDGE_MODULE_NAME) {
        config.logger.report(
            SwiftExportLogger.Severity.Warning,
            "Bridging header is not set. Using $DEFAULT_BRIDGE_MODULE_NAME instead"
        )
        DEFAULT_BRIDGE_MODULE_NAME
    }
    val unsupportedDeclarationReporter = config.unsupportedDeclarationReporterKind.toReporter()
    val buildResult = buildSwiftModule(
        input,
        dependencies,
        moduleForPackages,
        config,
        unsupportedDeclarationReporter,
    )
    val bridgeGenerator = createBridgeGenerator(object : SirTypeNamer {
        override fun swiftFqName(type: SirType): String = type.swiftName
        override fun kotlinFqName(type: SirType): String {
            require(type is SirNominalType)

            return when(val declaration = type.type) {
                KotlinRuntimeModule.kotlinBase -> "kotlin.Any"
                SirSwiftModule.string -> "kotlin.String"
                else -> ((declaration.origin as KotlinSource).symbol as KaClassLikeSymbol).classId!!.asFqNameString()
            }
        }
    })

    val additionalSwiftLinesProvider = if (unsupportedDeclarationReporter is SimpleUnsupportedDeclarationReporter) {
        // Lazily call after SIR printer to make sure that all declarations are collected.
        { unsupportedDeclarationReporter.messages.map { "// $it" } }
    } else {
        { emptyList() }
    }
    val bridgesModuleName = "${bridgeModuleNamePrefix}_${buildResult.mainModule.name}"
    listOf(buildResult.mainModule, buildResult.moduleForPackageEnums).forEach {
        val bridgeRequests = buildBridgeRequests(bridgeGenerator, it)
        if (bridgeRequests.isNotEmpty()) {
            it.updateImport(
                SirImport(
                    moduleName = bridgesModuleName,
                    mode = SirImport.Mode.ImplementationOnly
                )
            )
        }
        it.dumpResultToFiles(
            output = it.createOutputFiles(
                // because packageModule actually shared between modules, we need to write it into parent directory.
                // this way the write location of packageModule is shared between runs of `runSwiftExport`.
                // SHOULD BE MOVED during KT-68864
                if (moduleForPackages == it) config.outputPath.parent
                else config.outputPath
            ),
            bridgeGenerator = bridgeGenerator,
            requests = bridgeRequests,
            stableDeclarationsOrder = stableDeclarationsOrder,
            renderDocComments = renderDocComments,
            additionalSwiftLinesProvider = additionalSwiftLinesProvider,
        )
    }

    val resultingDependencies = buildList {
        if (config.multipleModulesHandlingStrategy == MultipleModulesHandlingStrategy.OneToOneModuleMapping) {
            addReference(buildResult.moduleForPackageEnums.name)
        }
        buildResult.mainModule.imports
            .filter { it.moduleName !in setOf(buildResult.moduleForPackageEnums.name, KotlinRuntimeModule.name, bridgesModuleName) }
            .forEach { addReference(it.moduleName) }
    }

    return@runCatching buildList {
        add(
            SwiftExportModule.BridgesToKotlin(
                name = buildResult.mainModule.name,
                dependencies = resultingDependencies,
                bridgeName = bridgesModuleName,
                files = buildResult.mainModule.createOutputFiles(config.outputPath)
            )
        )
        if (config.multipleModulesHandlingStrategy == MultipleModulesHandlingStrategy.OneToOneModuleMapping) {
            add(
                SwiftExportModule.SwiftOnly(
                    name = moduleForPackages.name,
                    swiftApi = buildResult.moduleForPackageEnums.createOutputFiles(config.outputPath.parent).swiftApi
                )
            )
        }
    }
}

private fun SirModule.createOutputFiles(outputPath: Path): SwiftExportFiles = SwiftExportFiles(
    swiftApi = (outputPath / name / "$name.swift"),
    kotlinBridges = (outputPath / name / "$name.kt"),
    cHeaderBridges = (outputPath / name / "$name.h")
)

private fun MutableList<SwiftExportModule.Reference>.addReference(destinationModuleName: String): Boolean =
    add(SwiftExportModule.Reference(destinationModuleName))
