/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.LegacyK2CliPipeline
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.prepareJvmSessions
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.createContextForIncrementalCompilation
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.FrontendContext
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFirFromKtFiles
import org.jetbrains.kotlin.fir.pipeline.runPlatformCheckers
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.multiplatform.hmppModuleName
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.util.PotentiallyIncorrectPhaseTimeMeasurement
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

@OptIn(LegacyK2CliPipeline::class)
fun runFrontendForKapt(
    environment: VfsBasedProjectEnvironment,
    configuration: CompilerConfiguration,
    messageCollector: MessageCollector,
    sources: List<KtFile>,
    module: Module,
): FirResult {
    val context = FrontendContextForSingleModulePsi(
        environment,
        messageCollector,
        configuration,
    )
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter(messageCollector)
    return context.compileSourceFilesToAnalyzedFirViaPsi(
        sources, diagnosticsReporter, module.getModuleName(), module.getFriendPaths(), true
    )!!
}

@OptIn(LegacyK2CliPipeline::class)
private class FrontendContextForSingleModulePsi(
    override val projectEnvironment: VfsBasedProjectEnvironment,
    override val messageCollector: MessageCollector,
    override val configuration: CompilerConfiguration
) : FrontendContext {
    override val extensionRegistrars: List<FirExtensionRegistrar> = FirExtensionRegistrar.getInstances(projectEnvironment.project)
}

@LegacyK2CliPipeline
private fun FrontendContext.compileSourceFilesToAnalyzedFirViaPsi(
    ktFiles: List<KtFile>,
    diagnosticsReporter: BaseDiagnosticsCollector,
    rootModuleName: String,
    friendPaths: List<String>,
    ignoreErrors: Boolean = false,
): FirResult? {
    val performanceManager = configuration.get(CLIConfigurationKeys.PERF_MANAGER)
    @OptIn(PotentiallyIncorrectPhaseTimeMeasurement::class)
    performanceManager?.notifyCurrentPhaseFinishedIfNeeded()
    performanceManager?.notifyAnalysisStarted()

    val syntaxErrors = ktFiles.fold(false) { errorsFound, ktFile ->
        AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, messageCollector).isHasErrors or errorsFound
    }

    val scriptsInCommonSourcesErrors = JvmFrontendPipelinePhase.checkIfScriptsInCommonSources(configuration, ktFiles)

    val sourceScope: AbstractProjectFileSearchScope = projectEnvironment.getSearchScopeByPsiFiles(ktFiles) +
            projectEnvironment.getSearchScopeForProjectJavaSources()

    var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()

    val providerAndScopeForIncrementalCompilation = createContextForIncrementalCompilation(projectEnvironment, configuration, sourceScope)

    providerAndScopeForIncrementalCompilation?.precompiledBinariesFileScope?.let {
        librariesScope -= it
    }
    val sessionsWithSources = prepareJvmSessions(
        ktFiles,
        rootModuleName,
        friendPaths,
        librariesScope,
        isCommonSource = { it.isCommonSource == true },
        isScript = { it.isScript() },
        fileBelongsToModule = { file, moduleName -> file.hmppModuleName == moduleName },
        createProviderAndScopeForIncrementalCompilation = { providerAndScopeForIncrementalCompilation }
    )

    val outputs = sessionsWithSources.map { (session, sources) ->
        buildResolveAndCheckFirFromKtFiles(session, sources, diagnosticsReporter)
    }
    outputs.runPlatformCheckers(diagnosticsReporter)

    performanceManager?.notifyAnalysisFinished()
    return runUnless(!ignoreErrors && (syntaxErrors || scriptsInCommonSourcesErrors || diagnosticsReporter.hasErrors)) { FirResult(outputs) }
}
