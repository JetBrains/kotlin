/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.backend.common.eliminateLibrariesWithDuplicatedUniqueNames
import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.backend.common.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.cli.common.allowKotlinPackage
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.contentRoots
import org.jetbrains.kotlin.cli.common.createPerformanceManagerFor
import org.jetbrains.kotlin.cli.common.metadataDestinationDirectory
import org.jetbrains.kotlin.cli.common.renderDiagnosticInternalName
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.K2MetadataConfigurationKeys
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.metadata.*
import org.jetbrains.kotlin.commonizer.CommonizerLogLevel
import org.jetbrains.kotlin.commonizer.CompiledDependency
import org.jetbrains.kotlin.commonizer.cli.CliLoggerAdapter
import org.jetbrains.kotlin.commonizer.konan.DefaultModulesProvider
import org.jetbrains.kotlin.commonizer.konan.NativeLibrary
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.invokeToplevel
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.backend.js.moduleName
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

data class NamedMetadata(
    val name: Name,
    val metadata: SerializedMetadata,
) {
    constructor(name: String, metadata: SerializedMetadata) : this(Name.special("<${name}>"), metadata)
}

infix fun SerializedMetadata.named(name: String) = NamedMetadata(name, this)

private fun nativeDistributionPath(): File =
    System.getProperty("kotlin.internal.native.test.nativeHome")?.let(::File)
        ?: error("Missing 'kotlin.internal.native.test.nativeHome' system property")

private fun nativeDistributionKlibPath(): File = nativeDistributionPath().resolve("klib")

private fun stdlibPath(): File = nativeDistributionKlibPath().resolve("common").resolve("stdlib")

private fun mockJdk(): File = KtTestUtil.findMockJdkRtJar()

fun loadStdlibMetadata() = loadStdlibMetadata(KotlinTestUtils.newConfiguration())

fun loadStdlibMetadata(configuration: CompilerConfiguration): NamedMetadata {
    val kotlinLoaderResult = KlibLoader {
        libraryPaths(stdlibPath().absolutePath)
        maxPermittedAbiVersion(KotlinAbiVersion.CURRENT)
    }.load()
        .apply { reportLoadingProblemsIfAny(configuration, allAsErrors = true) }
        // TODO (KT-76785): Handling of duplicated names is a workaround that needs to be removed in the future.
        .eliminateLibrariesWithDuplicatedUniqueNames(configuration)

    val stdlib = kotlinLoaderResult.librariesStdlibFirst.firstOrNull() ?: error("No stdlib found")
    val dummyLogger = CliLoggerAdapter(CommonizerLogLevel.Quiet, 2)
    val stdlibModuleProvider = DefaultModulesProvider.forDependencies(listOf(NativeLibrary(stdlib)), dummyLogger)

    return NamedMetadata(stdlib.libraryName, stdlibModuleProvider.loadModuleMetadata(stdlib.moduleName))
}

fun createEmptyModule(name: String, disposable: Disposable): SerializedMetadata {
    val module = InlineSourceBuilder.ModuleBuilder().apply {
        this.name = name
        source(content = "", "empty.kt")
    }.build()

    return createModule(module, disposable).second.metadata
}

fun createModule(
    module: InlineSourceBuilder.Module,
    disposable: Disposable,
): Pair<CompilerConfiguration, MetadataInMemorySerializationArtifact> {
    val moduleRoot = FileUtil.createTempDirectory(module.name, null)
    module.sourceFiles.forEach { sourceFile ->
        moduleRoot.resolve(sourceFile.name).writeText(sourceFile.content)
    }

    val dependencyToMetadata = mutableMapOf<InlineSourceBuilder.Module, CompiledDependency>()
    val compiledDependenciesRoot = FileUtil.createTempDirectory(module.name, null)

    // TODO: avoid recompiling the same dependencies of dependencies multiple times, see use sites of `createModule()`
    return serializeModuleAndAllDependenciesToMetadata(module, disposable, dependencyToMetadata, compiledDependenciesRoot)
}

fun serializeModuleAndAllDependenciesToMetadata(
    module: InlineSourceBuilder.Module,
    disposable: Disposable,
    dependencyToMetadata: MutableMap<InlineSourceBuilder.Module, CompiledDependency>,
    compiledDependenciesRoot: File,
): Pair<CompilerConfiguration, MetadataInMemorySerializationArtifact> {
    val moduleRoot = FileUtil.createTempDirectory(module.name, null)

    module.sourceFiles.forEach { sourceFile ->
        moduleRoot.resolve(sourceFile.name).writeText(sourceFile.content)
    }

    for (dependency in module.dependencies) {
        if (dependency !in dependencyToMetadata) {
            val (configuration, dependencyArtifact) = serializeModuleAndAllDependenciesToMetadata(
                dependency, disposable, dependencyToMetadata, compiledDependenciesRoot,
            )
            configuration.metadataDestinationDirectory = File(compiledDependenciesRoot.path + File.pathSeparator + dependency.name)
            dependencyToMetadata[dependency] = CompiledDependency(
                namedMetadata = dependencyArtifact.metadata named dependency.name,
                destination = MetadataKlibFileWriterPhase.executePhase(dependencyArtifact).destination,
            )
        }
    }

    return serializeModuleToMetadata(
        module.name, moduleRoot, CommonPlatforms.defaultCommonPlatform, disposable,
        regularDependencies = module.dependencies.map { dependency ->
            dependencyToMetadata[dependency]?.let { JvmClasspathRoot(File(it.destination)) }
                ?: error("Missing dependency metadata for ${dependency.name}")
        }
    )
}

fun serializeModuleToMetadata(
    moduleName: String,
    moduleRoot: File,
    targetPlatform: TargetPlatform,
    disposable: Disposable,
    regularDependencies: List<ContentRoot> = emptyList(),
    refinesDependencies: List<String> = emptyList(),
    isCommon: Boolean = false,
    firTransformationPhase: PipelinePhase<MetadataFrontendPipelineArtifact, MetadataFrontendPipelineArtifact>? = null,
): Pair<CompilerConfiguration, MetadataInMemorySerializationArtifact> {
    check(Name.isValidIdentifier(moduleName))

    val configuration = KotlinTestUtils.newConfiguration()
    configuration.put(CommonConfigurationKeys.MODULE_NAME, moduleName)
    configuration.allowKotlinPackage = true

    configuration.languageVersionSettings = LanguageVersionSettingsImpl(
        LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE,
        specificFeatures = mapOf(
            LanguageFeature.MultiPlatformProjects to LanguageFeature.State.ENABLED
        ),
    )

    configuration.targetPlatform = targetPlatform
    configuration.renderDiagnosticInternalName = true

    configuration.contentRoots += JvmClasspathRoot(stdlibPath())
    if (targetPlatform.isJvm()) {
        configuration.contentRoots += JvmClasspathRoot(mockJdk())
    }
    configuration.contentRoots += regularDependencies + refinesDependencies.map { JvmClasspathRoot(File(it)) }
    configuration.putIfNotNull(K2MetadataConfigurationKeys.REFINES_PATHS, refinesDependencies.takeIf { it.isNotEmpty() })
    configuration.contentRoots += moduleRoot.walkTopDown()
        .filter { it.isFile }
        .map { KotlinSourceRoot(it.path, isCommon, hmppModuleName = null) }
        .toList()

    val diagnosticCollector = DiagnosticReporterFactory.createReporter(configuration.messageCollector)
    val performanceManager = createPerformanceManagerFor(JvmPlatforms.unspecifiedJvmPlatform)

    val phaseConfig = PhaseConfig()
    val context = PipelineContext(
        configuration.messageCollector,
        diagnosticCollector,
        performanceManager,
        renderDiagnosticInternalName = true,
        kaptMode = false,
    )

    val configurationArtifact = ConfigurationPipelineArtifact(configuration, diagnosticCollector, disposable)
    val serializationPipeline = MetadataFrontendPipelinePhase thenMaybe firTransformationPhase then MetadataKlibInMemorySerializerPhase
    return configuration to serializationPipeline.invokeToplevel(phaseConfig, context, configurationArtifact)
}

private infix fun <Context : LoggingContext, Input, Output> CompilerPhase<Context, Input, Output>.thenMaybe(
    other: CompilerPhase<Context, Output, Output>?
): CompilerPhase<Context, Input, Output> = when (other) {
    null -> this
    else -> this then other
}
