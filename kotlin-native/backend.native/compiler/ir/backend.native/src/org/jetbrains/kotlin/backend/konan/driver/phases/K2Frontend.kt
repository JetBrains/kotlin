/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.makeSerializedKlibMetadata
import org.jetbrains.kotlin.backend.common.serialization.metadata.serializeKlibHeader
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.konan.FrontendServices
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.driver.BasicPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentTypeTransformer
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.Fir2IrConverter
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrResult
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.backend.jvm.Fir2IrJvmSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.pipeline.buildFirFromKtFiles
import org.jetbrains.kotlin.fir.pipeline.runCheckers
import org.jetbrains.kotlin.fir.pipeline.runResolution
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.session.FirNativeSessionFactory
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrModuleSerializer
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.File

sealed class K2FrontendPhaseOutput {
    object ShouldNotGenerateCode : K2FrontendPhaseOutput()

    data class Full(
            val serializerOutput: SerializerOutput
    ) : K2FrontendPhaseOutput()
}

internal interface K2FrontendContext : PhaseContext {
    var frontendServices: FrontendServices
}

internal class K2FrontendContextImpl(
        config: KonanConfig
) : BasicPhaseContext(config), K2FrontendContext {
    override lateinit var frontendServices: FrontendServices
}

internal fun <T : K2FrontendContext> PhaseEngine<T>.runFrontend(environment: KotlinCoreEnvironment): K2FrontendPhaseOutput {
    return this.runPhase(K2FrontendPhase, environment)
}

internal val K2FrontendPhase = createSimpleNamedCompilerPhase(
        "K2Frontend", "K2 Compiler frontend",
        outputIfNotEnabled = { _, _, _, _ -> K2FrontendPhaseOutput.ShouldNotGenerateCode }
) { context: K2FrontendContext, input: KotlinCoreEnvironment ->
    phaseBody(input, context)
}

internal val KlibFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer, PlatformDependentTypeTransformer.None)

private fun phaseBody(
        environment: KotlinCoreEnvironment,
        context: K2FrontendContext
): K2FrontendPhaseOutput {
    val configuration = environment.configuration
    val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()
    val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)

    // FIR

    val sessionProvider = FirProjectSessionProvider()
    val extensionRegistrars = FirExtensionRegistrar.getInstances(environment.project)
    val sessionConfigurator: FirSessionConfigurator.() -> Unit = {
        if (configuration.languageVersionSettings.getFlag(AnalysisFlags.extendedCompilerChecks)) {
            registerExtendedCommonCheckers()
        }
    }

    val mainModuleName = Name.special("<${context.config.moduleId}>")

    val ktFiles = environment.getSourceFiles()
    val syntaxErrors = ktFiles.fold(false) { errorsFound, ktFile ->
        AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, messageCollector).isHasErrors or errorsFound
    }

    val dependencyList = DependencyListForCliModule.build(mainModuleName, CommonPlatforms.defaultCommonPlatform, NativePlatformAnalyzerServices) {
        dependencies(context.config.resolvedLibraries.getFullList().map { it.libraryFile.absolutePath })
        friendDependencies(context.config.friendModuleFiles.map { it.absolutePath })
        // TODO: !!! dependencies module data?
    }
    val resolvedLibraries: List<KotlinResolvedLibrary> = context.config.resolvedLibraries.getFullResolvedList()

    FirNativeSessionFactory.createLibrarySession(
            mainModuleName,
            sessionProvider,
            dependencyList,
            resolvedLibraries,
            configuration.languageVersionSettings,
    )

    val mainModuleData = FirModuleDataImpl(
            mainModuleName,
            dependencyList.regularDependencies,
            dependencyList.dependsOnDependencies,
            dependencyList.friendsDependencies,
            dependencyList.platform,
            dependencyList.analyzerServices
    )

    val session = FirNativeSessionFactory.createModuleBasedSession(
            mainModuleData,
            sessionProvider,
            extensionRegistrars,
            configuration.languageVersionSettings,
            sessionConfigurator,
    )

    val rawFirFiles = session.buildFirFromKtFiles(ktFiles)
    val (scopeSession, firFiles) = session.runResolution(rawFirFiles)
    session.runCheckers(scopeSession, firFiles, diagnosticsReporter)

    if (syntaxErrors || diagnosticsReporter.hasErrors) {
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)
        return K2FrontendPhaseOutput.ShouldNotGenerateCode
    }
    firFiles.forEach { println(it.render()) }

    // FIR2IR

    val fir2IrExtensions = Fir2IrExtensions.Default
    val signaturer = IdSignatureDescriptor(KonanManglerDesc)
    val commonFirFiles = session.moduleData.dependsOnDependencies
            .map { it.session }
            .filter { it.kind == FirSession.Kind.Source }
            .flatMap { (it.firProvider as FirProviderImpl).getAllFirFiles() }

    var builtInsModule: KotlinBuiltIns? = null
    val dependencies = mutableListOf<ModuleDescriptorImpl>()

    val librariesDescriptors = resolvedLibraries.map { resolvedLibrary ->
        val storageManager = LockBasedStorageManager("ModulesStructure")

        val moduleDescriptor = KlibFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                resolvedLibrary.library,
                configuration.languageVersionSettings,
                storageManager,
                builtInsModule,
                packageAccessHandler = null,
                lookupTracker = LookupTracker.DO_NOTHING
        )
        dependencies += moduleDescriptor
        moduleDescriptor.setDependencies(ArrayList(dependencies))

        val isBuiltIns = resolvedLibrary.library.unresolvedDependencies.isEmpty()
        if (isBuiltIns) builtInsModule = moduleDescriptor.builtIns

        moduleDescriptor
    }

    val fir2irResult = Fir2IrConverter.createModuleFragmentWithSignaturesIfNeeded(
            session, scopeSession, firFiles + commonFirFiles,
            configuration.languageVersionSettings, signaturer,
            fir2IrExtensions,
            FirJvmKotlinMangler(session), // TODO: replace with potentially simpler Konan version
            KonanManglerIr, IrFactoryImpl,
            Fir2IrVisibilityConverter.Default,
            Fir2IrJvmSpecialAnnotationSymbolProvider(), // TODO: replace with appropriate (probably empty) implementation
            IrGenerationExtension.getInstances(environment.project),
            generateSignatures = false,
            kotlinBuiltIns = builtInsModule ?: DefaultBuiltIns.Instance // TODO: consider passing externally
    ).also {
        (it.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = librariesDescriptors }
    }

    // Serialize KLib

    val firFilesBySourceFile = firFiles.associateBy { it.sourceFile }
    val metadataVersion =
            configuration.get(CommonConfigurationKeys.METADATA_VERSION)
                    ?: GenerationState.LANGUAGE_TO_METADATA_VERSION.getValue(configuration.languageVersionSettings.languageVersion)

    val serializerOutput = serializeModuleIntoKlib(
            environment,
            context,
            firFiles,
            fir2irResult.irModuleFragment,
    ) { file ->
        val firFile = firFilesBySourceFile[file] ?: error("cannot find FIR file by source file ${file.name} (${file.path})")
        serializeSingleFirFile(firFile, session, scopeSession, metadataVersion)
    }

    return K2FrontendPhaseOutput.Full(serializerOutput)
}

private val CompilerConfiguration.expectActualLinker: Boolean
    get() = get(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER) ?: false


// inspired by from compiler/ir/serialization.js/src/org/jetbrains/kotlin/ir/backend/js/klib.kt:serializeModuleIntoKlib()
internal fun serializeModuleIntoKlib(
        environment: KotlinCoreEnvironment,
        context: K2FrontendContext,
        firFiles: List<FirFile>,
        moduleFragment: IrModuleFragment,
        serializeSingleFile: (KtSourceFile) -> ProtoBuf.PackageFragment
): SerializerOutput {
    val configuration = environment.configuration
    val messageLogger: IrMessageLogger = configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None
    val sourceFiles = firFiles.mapNotNull { it.sourceFile }
    val cleanFiles = environment.configuration.incrementalDataProvider?.getSerializedData(sourceFiles) ?: emptyList()

    assert(sourceFiles.size == moduleFragment.files.size)

    val compatibilityMode = CompatibilityMode(KotlinAbiVersion.CURRENT) // TODO get from test file data)
    val sourceBaseDirs = configuration[CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES] ?: emptyList()
    val absolutePathNormalization = configuration[CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH] ?: false

    val serializedIr =
            KonanIrModuleSerializer(
                    messageLogger,
                    moduleFragment.irBuiltins,
                    mutableMapOf(), // TODO: expect -> actual mapping
                    skipExpects = !configuration.expectActualLinker,
                    compatibilityMode,
                    normalizeAbsolutePaths = absolutePathNormalization,
                    sourceBaseDirs = sourceBaseDirs
            ).serializedIrModule(moduleFragment)

    val incrementalResultsConsumer = configuration.get(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER)
    val empty = ByteArray(0)

    fun processCompiledFileData(ioFile: File, compiledFile: KotlinFileSerializedData) {
        incrementalResultsConsumer?.run {
            processPackagePart(ioFile, compiledFile.metadata, empty, empty)
            with(compiledFile.irData) {
                processIrFile(ioFile, fileData, types, signatures, strings, declarations, bodies, fqName.toByteArray(), debugInfo)
            }
        }
    }

    val additionalFiles = mutableListOf<KotlinFileSerializedData>()

    for ((ktSourceFile, binaryFile) in sourceFiles.zip(serializedIr.files)) {
        assert(ktSourceFile.path == binaryFile.path) {
            """The Kt and Ir files are put in different order
                Kt: ${ktSourceFile.path}
                Ir: ${binaryFile.path}
            """.trimMargin()
        }
        val packageFragment = serializeSingleFile(ktSourceFile)
        val compiledKotlinFile = KotlinFileSerializedData(packageFragment.toByteArray(), binaryFile)

        additionalFiles += compiledKotlinFile
        val ioFile = ktSourceFile.toIoFileOrNull()
        assert(ioFile != null) {
            "No file found for source ${ktSourceFile.path}"
        }
        processCompiledFileData(ioFile!!, compiledKotlinFile)
    }

    val compiledKotlinFiles = (cleanFiles + additionalFiles)

    val header = serializeKlibHeader(
            configuration.languageVersionSettings, moduleFragment.descriptor,
            compiledKotlinFiles.map { it.irData.fqName }.distinct().sorted(),
            emptyList()
    ).toByteArray()

    incrementalResultsConsumer?.run {
        processHeader(header)
    }

    val serializedMetadata =
            makeSerializedKlibMetadata(
                    compiledKotlinFiles.groupBy { it.irData.fqName }
                            .map { (fqn, data) -> fqn to data.sortedBy { it.irData.path }.map { it.metadata } }.toMap(),
                    header
            )

    val fullSerializedIr = SerializedIrModule(compiledKotlinFiles.map { it.irData })

    return SerializerOutput(
            serializedMetadata,
            fullSerializedIr,
            null,
            context.config.librariesWithDependencies(moduleFragment.descriptor))
}
