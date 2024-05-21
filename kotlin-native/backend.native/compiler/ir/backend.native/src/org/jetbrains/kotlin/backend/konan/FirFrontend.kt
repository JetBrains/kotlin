package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.phases.FirOutput
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.resolve.ImplicitIntegerCoercionModuleCapability
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms

@OptIn(SessionConfiguration::class)
internal inline fun <F> PhaseContext.firFrontend(
        input: KotlinCoreEnvironment,
        files: List<F>,
        fileHasSyntaxErrors: (F) -> Boolean,
        noinline isCommonSource: (F) -> Boolean,
        noinline fileBelongsToModule: (F, String) -> Boolean,
        buildResolveAndCheckFir: (FirSession, List<F>, BaseDiagnosticsCollector) -> ModuleCompilerAnalyzedOutput,
): FirOutput {
    val configuration = input.configuration
    val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()
    val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)

    // FIR
    val extensionRegistrars = FirExtensionRegistrar.getInstances(input.project)
    val mainModuleName = Name.special("<${config.moduleId}>")
    val syntaxErrors = files.fold(false) { errorsFound, file -> fileHasSyntaxErrors(file) or errorsFound }
    val binaryModuleData = BinaryModuleData.initialize(mainModuleName, CommonPlatforms.defaultCommonPlatform)
    val dependencyList = DependencyListForCliModule.build(binaryModuleData) {
        val (interopLibs, regularLibs) = config.resolvedLibraries.getFullList().partition { it.isCInteropLibrary() }
        dependencies(regularLibs.map { it.libraryFile.absolutePath })
        if (interopLibs.isNotEmpty()) {
            val interopModuleData =
                    BinaryModuleData.createDependencyModuleData(
                            Name.special("<regular interop dependencies of $mainModuleName>"),
                            CommonPlatforms.defaultCommonPlatform,
                            FirModuleCapabilities.create(listOf(ImplicitIntegerCoercionModuleCapability))
                    )
            dependencies(interopModuleData, interopLibs.map { it.libraryFile.absolutePath })
        }
        friendDependencies(config.friendModuleFiles.map { it.absolutePath })
        dependsOnDependencies(config.refinesModuleFiles.map { it.absolutePath })
        // TODO: !!! dependencies module data?
    }
    val resolvedLibraries: List<KotlinResolvedLibrary> = config.resolvedLibraries.getFullResolvedList()

    val sessionsWithSources = prepareNativeSessions(
            files,
            configuration,
            mainModuleName,
            resolvedLibraries,
            dependencyList,
            extensionRegistrars,
            metadataCompilationMode = config.metadataKlib,
            isCommonSource = isCommonSource,
            fileBelongsToModule = fileBelongsToModule,
    )

    val outputs = sessionsWithSources.map { (session, sources) ->
        buildResolveAndCheckFir(session, sources, diagnosticsReporter).also {
            if (shouldPrintFiles()) {
                it.fir.forEach { file -> println(file.render()) }
            }
        }
    }

    outputs.runPlatformCheckers(diagnosticsReporter)

    FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)
    return if (syntaxErrors || diagnosticsReporter.hasErrors) {
        throw KonanCompilationException("Compilation failed: there were frontend errors")
    } else {
        FirOutput.Full(FirResult(outputs))
    }
}

internal fun PhaseContext.firFrontendWithPsi(input: KotlinCoreEnvironment): FirOutput {
    val configuration = input.configuration
    val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    // FIR

    val ktFiles = input.getSourceFiles()
    return firFrontend(
            input,
            ktFiles,
            fileHasSyntaxErrors = {
                AnalyzerWithCompilerReport.reportSyntaxErrors(it, messageCollector).isHasErrors
            },
            isCommonSource = isCommonSourceForPsi,
            fileBelongsToModule = fileBelongsToModuleForPsi,
            buildResolveAndCheckFir = { session, files, diagnosticsReporter ->
                buildResolveAndCheckFirFromKtFiles(session, files, diagnosticsReporter)
            },
    )
}

internal fun PhaseContext.firFrontendWithLightTree(input: KotlinCoreEnvironment): FirOutput {
    val configuration = input.configuration
    val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    // FIR

    val groupedSources = collectSources(configuration, input.project, messageCollector)

    val ktSourceFiles = mutableListOf<KtSourceFile>().apply {
        addAll(groupedSources.commonSources)
        addAll(groupedSources.platformSources)
    }

    val metadataVersion = configuration.get(CommonConfigurationKeys.METADATA_VERSION) ?: BuiltInsBinaryVersion.INSTANCE

    return firFrontend(
            input,
            ktSourceFiles,
            fileHasSyntaxErrors = { false },
            isCommonSource = { groupedSources.isCommonSourceForLt(it) },
            fileBelongsToModule = { file, it -> groupedSources.fileBelongsToModuleForLt(file, it) },
            buildResolveAndCheckFir = { session, files, diagnosticsReporter ->
                buildResolveAndCheckFirViaLightTree(
                        session, files, metadataVersion, configuration.languageVersionSettings, diagnosticsReporter, null
                )
            },
    )
}
