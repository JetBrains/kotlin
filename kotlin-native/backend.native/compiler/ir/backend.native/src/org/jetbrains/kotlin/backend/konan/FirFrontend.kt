package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.phases.FirOutput
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.session.FirNativeSessionFactory
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource

internal fun PhaseContext.firFrontend(
        input: KotlinCoreEnvironment
): FirOutput {
    val configuration = input.configuration
    val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()
    val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    // FIR
    val sessionProvider = FirProjectSessionProvider()
    val extensionRegistrars = FirExtensionRegistrar.getInstances(input.project)
    val sessionConfigurator: FirSessionConfigurator.() -> Unit = {
        if (configuration.languageVersionSettings.getFlag(AnalysisFlags.extendedCompilerChecks)) {
            registerExtendedCommonCheckers()
        }
    }
    val mainModuleName = Name.special("<${config.moduleId}>")
    val ktFiles = input.getSourceFiles()
    val syntaxErrors = ktFiles.fold(false) { errorsFound, ktFile ->
        AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, messageCollector).isHasErrors or errorsFound
    }
    val binaryModuleData = BinaryModuleData.initialize(mainModuleName, CommonPlatforms.defaultCommonPlatform, NativePlatformAnalyzerServices)
    val dependencyList = DependencyListForCliModule.build(binaryModuleData) {
        dependencies(config.resolvedLibraries.getFullList().map { it.libraryFile.absolutePath })
        friendDependencies(config.friendModuleFiles.map { it.absolutePath })
        // TODO: !!! dependencies module data?
    }
    val resolvedLibraries: List<KotlinResolvedLibrary> = config.resolvedLibraries.getFullResolvedList()
    FirNativeSessionFactory.createLibrarySession(
            mainModuleName,
            resolvedLibraries,
            sessionProvider,
            dependencyList.moduleDataProvider,
            configuration.languageVersionSettings,
            registerExtraComponents = {},
    )

    fun runFrontend(
            moduleName: Name,
            dependsOn: List<FirModuleData>,
            ktFiles: List<KtFile>
    ): ModuleCompilerAnalyzedOutput {
        val moduleData = FirModuleDataImpl(
                moduleName,
                dependencyList.regularDependencies,
                dependsOn,
                dependencyList.friendsDependencies,
                CommonPlatforms.defaultCommonPlatform,
                NativePlatformAnalyzerServices
        )
        val session = FirNativeSessionFactory.createModuleBasedSession(
                moduleData,
                sessionProvider,
                extensionRegistrars,
                configuration.languageVersionSettings,
                sessionConfigurator,
        )
        val output = buildResolveAndCheckFir(session, ktFiles, diagnosticsReporter)
        if (shouldPrintFiles())
            output.fir.forEach { println(it.render()) }

        return output
    }

    val isMppEnabled = configuration.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)

    val firResult = if (isMppEnabled) {
        val (commonKtFiles, platformKtFiles) = ktFiles.partition { it.isCommonSource == true }
        val commonOutput = runFrontend(Name.identifier("${mainModuleName}-common"), emptyList(), commonKtFiles)
        val platformOutput = runFrontend(mainModuleName, listOf(commonOutput.session.moduleData), platformKtFiles)
        FirResult(platformOutput, commonOutput)
    } else {
        FirResult(runFrontend(mainModuleName, emptyList(), ktFiles), null)
    }

    return if (syntaxErrors || diagnosticsReporter.hasErrors) {
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)
        FirOutput.ShouldNotGenerateCode
    } else {
        FirOutput.Full(firResult)
    }
}
