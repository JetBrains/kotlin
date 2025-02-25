/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.impl.SirEnumGeneratorImpl
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule
import org.jetbrains.kotlin.sir.providers.utils.SilentUnsupportedDeclarationReporter
import org.jetbrains.kotlin.sir.providers.utils.SimpleUnsupportedDeclarationReporter
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter
import org.jetbrains.kotlin.swiftexport.standalone.builders.SwiftExportDependencies
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig
import org.jetbrains.kotlin.swiftexport.standalone.translation.TranslationResult
import org.jetbrains.kotlin.swiftexport.standalone.translation.translateModulePublicApi
import org.jetbrains.kotlin.swiftexport.standalone.utils.logConfigIssues
import org.jetbrains.kotlin.swiftexport.standalone.writer.dumpTextAtFile
import org.jetbrains.kotlin.swiftexport.standalone.writer.dumpTextAtPath
import org.jetbrains.sir.printer.SirAsSwiftSourcesPrinter
import java.io.Serializable
import java.nio.file.Path
import kotlin.io.path.div

public enum class UnsupportedDeclarationReporterKind {
    Silent, Inline;

    public fun toReporter(): UnsupportedDeclarationReporter = when (this) {
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

public class InputModule(
    public val name: String,
    public val path: Path,
    public val config: SwiftModuleConfig,
)

public sealed class SwiftExportModule(
    public val name: String,
    public val dependencies: List<Reference>,
) : Serializable {

    public class Reference(
        public val name: String,
    )

    public class SwiftOnly(
        public val swiftApi: Path,
        public val kind: Kind,
        name: String,
    ) : SwiftExportModule(name, emptyList()) {
        public enum class Kind {
            KotlinPackages,
            KotlinRuntimeSupport,
        }

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
    config: SwiftExportConfig,
): Result<Set<SwiftExportModule>> = runCatching {
    logConfigIssues(input, config.logger)
    val stdlibInputModule = config.stdlibInputModule
    val platformLibsInputModule = config.platformLibsInputModule
    val translatedModules = input.map { rootModule ->
        /**
         * This value represents dependencies of current module.
         * The actual dependency graph is unknown at this point - there is only an array of modules to translate. This particular value
         * will be used to initialize Analysis API session. It is an error to pass module as a dependency to itself - therefor there is
         * a need to remove the current translation module from the list of dependencies.
         */
        val dependencies = input - rootModule
        translateModulePublicApi(
            rootModule,
            SwiftExportDependencies(
                user = dependencies,
                stdlib = stdlibInputModule,
                platform = platformLibsInputModule
            ),
            config
        )
    }

    val packagesModule = writeKotlinPackagesModule(
        sirModule = translatedModules.createModuleForPackages(config),
        outputPath = config.outputPath.parent / config.moduleForPackagesName / "${config.moduleForPackagesName}.swift"
    )
    val runtimeSupportModule = writeRuntimeSupportModule(
        config = config,
        outputPath = config.outputPath.parent / config.runtimeSupportModuleName / "${config.runtimeSupportModuleName}.swift",
    )
    return@runCatching setOf(packagesModule, runtimeSupportModule) + translatedModules.map { it.writeModule(config) }
}

private fun Collection<TranslationResult>.createModuleForPackages(config: SwiftExportConfig): SirModule = buildModule {
    name = config.moduleForPackagesName
}.apply {
    val enumGenerator = SirEnumGeneratorImpl(this)
    flatMap { it.packages }
        .forEach { with(enumGenerator) { it.sirPackageEnum() } }
}

private fun writeKotlinPackagesModule(
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
        kind = SwiftExportModule.SwiftOnly.Kind.KotlinPackages,
    )
}

private fun writeRuntimeSupportModule(
    config: SwiftExportConfig,
    outputPath: Path,
): SwiftExportModule.SwiftOnly {

    val runtimeSupportContent = config.javaClass.getResource("/swift/KotlinRuntimeSupport.swift")?.readText()
        ?: error("Can't find runtime support module")
    dumpTextAtFile(sequenceOf(runtimeSupportContent), outputPath.toFile())

    return SwiftExportModule.SwiftOnly(
        swiftApi = outputPath,
        name = config.runtimeSupportModuleName,
        kind = SwiftExportModule.SwiftOnly.Kind.KotlinRuntimeSupport,
    )
}

private fun TranslationResult.writeModule(config: SwiftExportConfig): SwiftExportModule {
    val swiftSources = sequenceOf(
        SirAsSwiftSourcesPrinter.print(
            sirModule,
            config.stableDeclarationsOrder,
            config.renderDocComments,
        )
    ) + moduleConfig.unsupportedDeclarationReporter.messages.map { "// $it" }

    val outputFiles = SwiftExportFiles(
        swiftApi = (config.outputPath / sirModule.name / "${sirModule.name}.swift"),
        kotlinBridges = (config.outputPath / sirModule.name / "${sirModule.name}.kt"),
        cHeaderBridges = (config.outputPath / sirModule.name / "${sirModule.name}.h")
    )

    dumpTextAtPath(
        swiftSources,
        bridgeSources,
        outputFiles
    )

    return SwiftExportModule.BridgesToKotlin(
        name = sirModule.name,
        dependencies = sirModule.imports
            .filter { it.moduleName !in setOf(KotlinRuntimeModule.name, bridgesModuleName) + config.platformLibsInputModule.map { it.name } }
            .map { SwiftExportModule.Reference(it.moduleName) },
        bridgeName = bridgesModuleName,
        files = outputFiles
    )
}
