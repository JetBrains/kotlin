/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

import org.jetbrains.kotlin.analysis.api.klib.reader.createKaModulesForStandaloneAnalysis
import org.jetbrains.kotlin.library.metadata.KlibInputModule
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.providers.SirTypeProvider
import org.jetbrains.kotlin.sir.providers.impl.SirEnumGeneratorImpl
import org.jetbrains.kotlin.sir.providers.utils.SilentUnsupportedDeclarationReporter
import org.jetbrains.kotlin.sir.providers.utils.SimpleUnsupportedDeclarationReporter
import org.jetbrains.kotlin.sir.providers.utils.UnsupportedDeclarationReporter
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig
import org.jetbrains.kotlin.swiftexport.standalone.translation.TranslationResult
import org.jetbrains.kotlin.swiftexport.standalone.translation.translateCrossReferencingModulesTransitively
import org.jetbrains.kotlin.swiftexport.standalone.translation.translateModulePublicApi
import org.jetbrains.kotlin.swiftexport.standalone.utils.patchConfigAndLogIssues
import org.jetbrains.kotlin.swiftexport.standalone.writer.BridgeSources
import org.jetbrains.kotlin.swiftexport.standalone.writer.dumpTextAtFile
import org.jetbrains.kotlin.swiftexport.standalone.writer.dumpTextAtPath
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.sir.printer.SirPrinter
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

public typealias InputModule = KlibInputModule<SwiftModuleConfig>

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
 * Translates a collection of fully exported Kotlin modules to their Swift equivalent,
 * handles dependencies among modules, writes Swift output files, and generates additional
 * runtime support modules for Swift integration.
 *
 * @param fullyExportedModules The set of modules that are fully defined for export to Swift.
 * @param transitivelyExportedModules The set of modules that are indirectly required for the translation process.
 * @param config The configuration object specifying the behavior, paths, and options for Swift export.
 * @return A [Result] containing a set of translated Swift export modules upon success, or an exception in case of a failure.
 */
public fun runSwiftExport(
    modules: Set<InputModule>,
    config: SwiftExportConfig,
): Result<Set<SwiftExportModule>> = runCatching {
    val config = patchConfigAndLogIssues(modules, config)
    val allModules = translateModules(modules, config)
    val packagesModule = writeKotlinPackagesModule(
        sirModule = allModules.createModuleForPackages(config),
        outputPath = config.outputPath.parent / config.moduleForPackagesName / "${config.moduleForPackagesName}.swift"
    )
    val runtimeSupportModule = writeRuntimeSupportModule(
        config = config,
        outputPath = config.outputPath.parent / config.runtimeSupportModuleName / "${config.runtimeSupportModuleName}.swift",
    )
    val coroutineSupportModule = writeCoroutineSupportModule(
        config = config,
        outputPath = config.outputPath.parent / config.coroutineSupportModuleName / "${config.coroutineSupportModuleName}.swift",
    )

    val predefinedModules = setOfNotNull(packagesModule, runtimeSupportModule, coroutineSupportModule)

    return@runCatching predefinedModules + allModules.map { it.writeModule(config) }
}

private fun translateModules(
    inputModules: Set<InputModule>,
    config: SwiftExportConfig,
): List<TranslationResult> {
    val allModules = inputModules + config.stdlibInputModule
    val kaModules = createKaModulesForStandaloneAnalysis(allModules, config.targetPlatform, config.platformLibsInputModule)
    val explicitModulesTranslationResults = allModules
        .filter { it.config.shouldBeFullyExported }
        .map { translateModulePublicApi(it, kaModules, config) }
    val transitiveExportRoots = allModules
        .filterNot { it.config.shouldBeFullyExported }
        .mapNotNull { kaModules.inputsToModules[it] }
        .associateWith { inputModule ->
            explicitModulesTranslationResults
                .flatMap { it.externalTypeDeclarationReferences[inputModule] ?: emptyList() }
        }
    val transitiveModulesTranslationResults = translateCrossReferencingModulesTransitively(transitiveExportRoots, kaModules, config)
    return explicitModulesTranslationResults + transitiveModulesTranslationResults
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
    val swiftSources = SirPrinter(
        stableDeclarationsOrder = true,
        renderDocComments = false,
    ).print(
        sirModule
    ).swiftSource

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
): SwiftExportModule.BridgesToKotlin = writeSupportModule(
    name = config.runtimeSupportModuleName,
    config = config,
    outputPath = outputPath
)

private fun writeCoroutineSupportModule(
    config: SwiftExportConfig,
    outputPath: Path,
): SwiftExportModule.BridgesToKotlin? = config.enableCoroutinesSupport.ifTrue {
    writeSupportModule(
        name = config.coroutineSupportModuleName,
        config = config,
        outputPath = outputPath
    )
}

private fun writeSupportModule(
    name: String,
    config: SwiftExportConfig,
    outputPath: Path,
): SwiftExportModule.BridgesToKotlin {
    require(name.isNotBlank() && name.first().isLetter() && name.all { it.isLetterOrDigit() }) { "Invalid module name $name" }

    val runtimeSupportContent = config.javaClass.getResource("/swift/$name.swift")?.readText()
        ?: error("Can not find runtime support swift source")

    val kotlinBridgeContent = config.javaClass.getResource("/swift/$name.kt")?.readText()
        ?: error("Can not find runtime support kotlin source")

    val cHeaderBridgeContent = config.javaClass.getResource("/swift/$name.h")?.readText()
        ?: error("Can not find runtime support c header")

    val modulePath = outputPath.parent / name
    val outputFiles = SwiftExportFiles(
        swiftApi = (modulePath / "$name.swift"),
        kotlinBridges = (modulePath / "$name.kt"),
        cHeaderBridges = (modulePath / "$name.h")
    )

    // arrayOf() used as workaround to target method from older kotlin-stlib due to https://github.com/gradle/gradle/issues/34442
    dumpTextAtPath(
        sequenceOf(*arrayOf(runtimeSupportContent)),
        BridgeSources(
            sequenceOf(*arrayOf(kotlinBridgeContent)),
            sequenceOf(*arrayOf(cHeaderBridgeContent)),
        ),
        outputFiles
    )

    return SwiftExportModule.BridgesToKotlin(
        name = name,
        dependencies = emptyList(), // or add dependencies if needed
        bridgeName = "${name}Bridge", // or another bridge name
        files = outputFiles
    )
}

private fun TranslationResult.writeModule(config: SwiftExportConfig): SwiftExportModule {
    // arrayOf() used as workaround to target method from older kotlin-stlib due to https://github.com/gradle/gradle/issues/34442
    val swiftSources = sequenceOf(*arrayOf(swiftModuleSources)) + moduleConfig.unsupportedDeclarationReporter.messages.map { "// $it" }
    val modulePath = config.outputPath / swiftModuleName
    val outputFiles = SwiftExportFiles(
        swiftApi = (modulePath / "$swiftModuleName.swift"),
        kotlinBridges = (modulePath / "$swiftModuleName.kt"),
        cHeaderBridges = (modulePath / "$swiftModuleName.h")
    )
    dumpTextAtPath(swiftSources, bridgeSources, outputFiles)

    return SwiftExportModule.BridgesToKotlin(
        name = swiftModuleName,
        dependencies = referencedSwiftModules,
        bridgeName = bridgesModuleName,
        files = outputFiles
    )
}
