/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.konan.FrontendServices
import org.jetbrains.kotlin.backend.konan.KonanCompilationException
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.TopDownAnalyzerFacadeForKonan
import org.jetbrains.kotlin.backend.konan.driver.BasicPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.BindingContext

sealed class FrontendPhaseOutput {
    object ShouldNotGenerateCode : FrontendPhaseOutput()

    data class Full(
            val moduleDescriptor: ModuleDescriptor,
            val bindingContext: BindingContext,
            val frontendServices: FrontendServices,
            val environment: KotlinCoreEnvironment,
    ) : FrontendPhaseOutput()
}

internal interface FrontendContext : PhaseContext {
    var frontendServices: FrontendServices
}

internal class FrontendContextImpl(
        config: KonanConfig
) : BasicPhaseContext(config), FrontendContext {
    override lateinit var frontendServices: FrontendServices
}

internal val FrontendPhase = createSimpleNamedCompilerPhase(
        "Frontend", "Compiler frontend",
        outputIfNotEnabled = { _, _, _, _ -> FrontendPhaseOutput.ShouldNotGenerateCode }
) { context: FrontendContext, input: KotlinCoreEnvironment ->
    lateinit var analysisResult: AnalysisResult

    do {
        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(
                context.messageCollector,
                input.configuration.languageVersionSettings,
                input.configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        )

        // Build AST and binding info.
        analyzerWithCompilerReport.analyzeAndReport(input.getSourceFiles()) {
            TopDownAnalyzerFacadeForKonan.analyzeFiles(input.getSourceFiles(), context)
        }
        if (analyzerWithCompilerReport.hasErrors()) {
            throw KonanCompilationException()
        }
        analysisResult = analyzerWithCompilerReport.analysisResult
        if (analysisResult is AnalysisResult.RetryWithAdditionalRoots) {
            input.addKotlinSourceRoots(analysisResult.additionalKotlinRoots)
        }
    } while (analysisResult is AnalysisResult.RetryWithAdditionalRoots)

    val moduleDescriptor = analysisResult.moduleDescriptor
    val bindingContext = analysisResult.bindingContext

    if (analysisResult.shouldGenerateCode) {
        FrontendPhaseOutput.Full(moduleDescriptor, bindingContext, context.frontendServices, input)
    } else {
        FrontendPhaseOutput.ShouldNotGenerateCode
    }
}

internal fun <T : FrontendContext> PhaseEngine<T>.runFrontend(environment: KotlinCoreEnvironment): FrontendPhaseOutput {
    return this.runPhase(FrontendPhase, environment)
}