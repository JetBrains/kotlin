/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jebrains.kotlin.backend.native.BaseNativeConfig
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.TopDownAnalyzerFacadeForKonan
import org.jebrains.kotlin.backend.native.PhaseContext
import org.jetbrains.kotlin.backend.FrontendContext
import org.jetbrains.kotlin.backend.FrontendContextImpl
import org.jetbrains.kotlin.backend.FrontendPhaseInput
import org.jetbrains.kotlin.backend.FrontendPhaseOutput
import org.jetbrains.kotlin.backend.NativeFrontendConfig
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal inline fun <C : PhaseContext, T> PhaseEngine<C>.startFrontendEngine(
    baseNativeConfig: BaseNativeConfig,
    body: (PhaseEngine<FrontendContext>) -> T): T
{
    return useContext(FrontendContextImpl(NativeFrontendConfig(baseNativeConfig)), body)
}

val FrontendPhase = createSimpleNamedCompilerPhase(
        "Frontend",
        outputIfNotEnabled = { _, _, _, _ -> FrontendPhaseOutput.ShouldNotGenerateCode }
) { context: FrontendContext, (environment, project): FrontendPhaseInput ->
    lateinit var analysisResult: AnalysisResult

    do {
        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(
                context.messageCollector,
                environment.configuration.languageVersionSettings,
                environment.configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        )

        val sourceFiles = environment.getSourceFiles()

        require(context.config.produce == CompilerOutputKind.LIBRARY || sourceFiles.isEmpty()) {
            "Internal error: no source files should have been passed here (${sourceFiles.first().virtualFilePath} in particular)\n" +
                    "to produce binary (e.g. a ${context.config.produce.name.toLowerCaseAsciiOnly()})\n" +
                    "KonanDriver.kt::splitOntoTwoStages() must transform such compilation into two-stage compilation. Please report this here: https://kotl.in/issue"
        }

        // Build AST and binding info.
        analyzerWithCompilerReport.analyzeAndReport(sourceFiles) {
            TopDownAnalyzerFacadeForKonan.analyzeFiles(sourceFiles, context, project)
        }
        if (analyzerWithCompilerReport.hasErrors()) {
            throw KonanCompilationException()
        }
        analysisResult = analyzerWithCompilerReport.analysisResult
        if (analysisResult is AnalysisResult.RetryWithAdditionalRoots) {
            environment.addKotlinSourceRoots(analysisResult.additionalKotlinRoots)
        }
    } while (analysisResult is AnalysisResult.RetryWithAdditionalRoots)

    val moduleDescriptor = analysisResult.moduleDescriptor
    val bindingContext = analysisResult.bindingContext

    if (analysisResult.shouldGenerateCode) {
        FrontendPhaseOutput.Full(moduleDescriptor, bindingContext, context.frontendServices, environment)
    } else {
        FrontendPhaseOutput.ShouldNotGenerateCode
    }
}