/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.bridge.SirTypeNamer
import org.jetbrains.kotlin.sir.bridge.createBridgeGenerator
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.SirModuleProvider
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.impl.SirEnumGeneratorImpl
import org.jetbrains.kotlin.sir.providers.impl.SirOneToOneModuleProvider
import org.jetbrains.kotlin.sir.providers.impl.SirSingleModuleProvider
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.*
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.isValidSwiftIdentifier
import org.jetbrains.kotlin.sir.util.swiftName
import org.jetbrains.kotlin.swiftexport.standalone.builders.TypeMapping
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildBridgeRequests
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildTypeMappings
import org.jetbrains.kotlin.swiftexport.standalone.builders.createModuleWithScopeProviderFromBinary
import org.jetbrains.kotlin.swiftexport.standalone.builders.buildRuntimeTypeMappings
import org.jetbrains.kotlin.swiftexport.standalone.builders.initializeSirModule
import org.jetbrains.kotlin.swiftexport.standalone.writer.*
import org.jetbrains.kotlin.swiftexport.standalone.writer.generateBridgeSources
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.jetbrains.sir.printer.SirAsSwiftSourcesPrinter
import java.io.Serializable
import java.nio.file.Path
import kotlin.io.path.div

public data class SwiftExportConfig(
    val settings: Map<String, String> = emptyMap(),
    val outputPath: Path,
    val logger: SwiftExportLogger = createDummyLogger(),
    val distribution: Distribution = Distribution(KotlinNativePaths.homePath.absolutePath),
    val errorTypeStrategy: ErrorTypeStrategy = ErrorTypeStrategy.Fail,
    val unsupportedTypeStrategy: ErrorTypeStrategy = ErrorTypeStrategy.SpecialType,
    val multipleModulesHandlingStrategy: MultipleModulesHandlingStrategy = MultipleModulesHandlingStrategy.OneToOneModuleMapping,
    val unsupportedDeclarationReporterKind: UnsupportedDeclarationReporterKind = UnsupportedDeclarationReporterKind.Silent,
    val moduleForPackagesName: String = "ExportedKotlinPackages",
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

    internal val stableDeclarationsOrder: Boolean = settings.containsKey(STABLE_DECLARATIONS_ORDER)
    internal val renderDocComments: Boolean = settings[RENDER_DOC_COMMENTS] != "false"
    internal val unsupportedDeclarationReporter: UnsupportedDeclarationReporter = unsupportedDeclarationReporterKind.toReporter()

    internal val bridgeGenerator = createBridgeGenerator(StandaloneSirTypeNamer)
    internal val bridgeModuleNamePrefix: String = settings.getOrElse(BRIDGE_MODULE_NAME) {
        logger.report(
            SwiftExportLogger.Severity.Warning,
            "Bridging header is not set. Using $DEFAULT_BRIDGE_MODULE_NAME instead"
        )
        DEFAULT_BRIDGE_MODULE_NAME
    }
    internal val targetPackageFqName = settings[ROOT_PACKAGE]?.let { packageName ->
        packageName.takeIf { FqNameUnsafe.isValid(it) }?.let { FqName(it) }
            ?.takeIf { it.pathSegments().all { it.toString().isValidSwiftIdentifier() } }
            ?: null.also {
                logger.report(
                    SwiftExportLogger.Severity.Warning,
                    "'$packageName' is not a valid name for ${ROOT_PACKAGE} and will be ignored"
                )
            }
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
    )

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
    val typeMappings: Path,
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
    val translatedModules = input
        .map { rootModule ->
            /**
             * This value represents dependencies of current module.
             * The actual dependency graph is unknown at this point - there is only an array of modules to translate. This particular value
             * will be used to initialize Analysis API session. It is an error to pass module as a dependency to itself - therefor there is
             * a need to remove the current translation module from the list of dependencies.
             */
            val dependencies = input - rootModule
            translateModule(rootModule, dependencies)
        }

    val packagesModule = writeSwiftModule(
        sirModule = translatedModules.createModuleForPackages(),
        outputPath = input.first().config.let { // we don't have "general" config, so we have to calculate this from nearest module KT-70205
            it.outputPath.parent / it.moduleForPackagesName / "${it.moduleForPackagesName}.swift"
        }
    )

    return@runCatching setOf(packagesModule) + translatedModules.map(TranslationResult::writeModule)
}

private fun translateModule(module: InputModule, dependencies: Set<InputModule>): TranslationResult {
    val moduleProvider: SirModuleProvider = when (module.config.multipleModulesHandlingStrategy) {
        MultipleModulesHandlingStrategy.OneToOneModuleMapping -> SirOneToOneModuleProvider()
        MultipleModulesHandlingStrategy.IntoSingleModule -> SirSingleModuleProvider(swiftModuleName = module.name)
    }
    val buildResult = createModuleWithScopeProviderFromBinary(module, dependencies)
        .initializeSirModule(module.config, moduleProvider)

    // KT-68253: bridge generation could be better
    val bridgeRequests = buildBridgeRequests(module.config.bridgeGenerator, buildResult.module)
    if (bridgeRequests.isNotEmpty()) {
        buildResult.module.updateImport(
            SirImport(
                moduleName = module.bridgesModuleName,
                mode = org.jetbrains.kotlin.sir.SirImport.Mode.ImplementationOnly
            )
        )
    }

    val bridges = generateBridgeSources(module.config.bridgeGenerator, bridgeRequests, true)

    val typeMappings = (buildRuntimeTypeMappings() + buildTypeMappings(buildResult.module)).toList()

    return TranslationResult(
        packages = buildResult.packages,
        sirModule = buildResult.module,
        bridgeSources = bridges,
        config = module.config,
        bridgesModuleName = module.bridgesModuleName,
        typeMappings = typeMappings,
    )
}

internal val InputModule.bridgesModuleName: String
    get() = "${config.bridgeModuleNamePrefix}_${name}"

private class TranslationResult(
    val sirModule: SirModule,
    val packages: Set<FqName>,
    val bridgeSources: BridgeSources,
    val config: SwiftExportConfig,
    val bridgesModuleName: String,
    val typeMappings: List<TypeMapping>,
)

private fun Collection<TranslationResult>.createModuleForPackages(): SirModule = buildModule {
    name = first().config.moduleForPackagesName
}.apply {
    val enumGenerator = SirEnumGeneratorImpl(this)
    flatMap { it.packages }
        .forEach { with(enumGenerator) { it.sirPackageEnum() } }
}

private fun writeSwiftModule(
    sirModule: SirModule,
    outputPath: Path,
): SwiftExportModule.SwiftOnly {
    val swiftSources = sequenceOf(
        SirAsSwiftSourcesPrinter.print(
            sirModule,
            stableDeclarationsOrder = true,
            renderDocComments = false,
        )
    )

    dumpTextAtFile(swiftSources, outputPath.toFile())

    return SwiftExportModule.SwiftOnly(
        name = sirModule.name,
        swiftApi = outputPath,
    )
}

private fun TranslationResult.writeModule(): SwiftExportModule {
    val swiftSources = sequenceOf(
        SirAsSwiftSourcesPrinter.print(
            sirModule,
            config.stableDeclarationsOrder,
            config.renderDocComments,
        )
    ) + config.unsupportedDeclarationReporter.messages.map { "// $it" }

    val outputFiles = SwiftExportFiles(
        swiftApi = (config.outputPath / sirModule.name / "${sirModule.name}.swift"),
        kotlinBridges = (config.outputPath / sirModule.name / "${sirModule.name}.kt"),
        cHeaderBridges = (config.outputPath / sirModule.name / "${sirModule.name}.h"),
        typeMappings = (config.outputPath / sirModule.name / "${sirModule.name}.type-mappings.txt"),
    )

    dumpTextAtPath(
        swiftSources,
        bridgeSources,
        typeMappings,
        outputFiles
    )

    return SwiftExportModule.BridgesToKotlin(
        name = sirModule.name,
        dependencies = sirModule.imports
            .filter { it.moduleName !in setOf(KotlinRuntimeModule.name, bridgesModuleName) }
            .map { SwiftExportModule.Reference(it.moduleName) },
        bridgeName = bridgesModuleName,
        files = outputFiles
    )
}

private object StandaloneSirTypeNamer : SirTypeNamer {
    override fun swiftFqName(type: SirType): String = type.swiftName
    override fun kotlinFqName(type: SirType): String {
        require(type is SirNominalType)

        return when(val declaration = type.type) {
            KotlinRuntimeModule.kotlinBase -> "kotlin.Any"
            SirSwiftModule.string -> "kotlin.String"
            else -> ((declaration.origin as KotlinSource).symbol as KaClassLikeSymbol).classId!!.asFqNameString()
        }
    }
}
