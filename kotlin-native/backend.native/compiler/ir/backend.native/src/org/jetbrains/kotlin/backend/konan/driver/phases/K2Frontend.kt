/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.FrontendServices
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.driver.BasicPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirModuleDataImpl
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.pipeline.buildFirFromKtFiles
import org.jetbrains.kotlin.fir.pipeline.runCheckers
import org.jetbrains.kotlin.fir.pipeline.runResolution
import org.jetbrains.kotlin.fir.session.FirNativeSessionFactory
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices

sealed class K2FrontendPhaseOutput {
    object ShouldNotGenerateCode : K2FrontendPhaseOutput()

    data class Full(
//            val moduleDescriptor: ModuleDescriptor,
//            val bindingContext: BindingContext,
//            val frontendServices: FrontendServices,
            val firFiles: List<FirFile>,
            val environment: KotlinCoreEnvironment,
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

internal val K2FrontendPhase = createSimpleNamedCompilerPhase(
        "K2Frontend", "K2 Compiler frontend",
        outputIfNotEnabled = { _, _, _, _ -> K2FrontendPhaseOutput.ShouldNotGenerateCode }
) { context: K2FrontendContext, input: KotlinCoreEnvironment ->

    val configuration = input.configuration
    val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()
    val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)

    val sessionProvider = FirProjectSessionProvider()
    val extensionRegistrars = FirExtensionRegistrar.getInstances(input.project)
    val sessionConfigurator: FirSessionConfigurator.() -> Unit = {
        if (configuration.languageVersionSettings.getFlag(AnalysisFlags.extendedCompilerChecks)) {
            registerExtendedCommonCheckers()
        }
    }

    val mainModuleName = Name.special("<${configuration.get(KonanConfigKeys.OUTPUT)!!}>")

    val ktFiles = input.getSourceFiles()
    val syntaxErrors = ktFiles.fold(false) { errorsFound, ktFile ->
        AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, messageCollector).isHasErrors or errorsFound
    }

//    val mainModule = MainModule.SourceFiles(input.getSourceFiles())

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
        K2FrontendPhaseOutput.ShouldNotGenerateCode
    } else
        K2FrontendPhaseOutput.Full(
//                moduleDescriptor,
//                bindingContext,
//                context.frontendServices,
                firFiles,
                input)
}

internal fun <T : K2FrontendContext> PhaseEngine<T>.runFrontend(environment: KotlinCoreEnvironment): K2FrontendPhaseOutput {
    return this.runPhase(K2FrontendPhase, environment)
}
