/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfigurationService
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.backend.common.phaser.SimpleNamedCompilerPhase
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

sealed class FrontendPhaseResult {
    object ShouldNotGenerateCode : FrontendPhaseResult()

    data class Full(
            val moduleDescriptor: ModuleDescriptor,
            val bindingContext: BindingContext,
            val frontendServices: FrontendServices,
            val environment: KotlinCoreEnvironment,
    ) : FrontendPhaseResult()
}

internal interface FrontendContext : PhaseContext {
    var frontendServices: FrontendServices
}

internal class FrontendContextImpl(
        config: KonanConfig
) : BasicPhaseContext(config), FrontendContext {
    override lateinit var frontendServices: FrontendServices
}

internal val FrontendPhase = object : SimpleNamedCompilerPhase<FrontendContext, KotlinCoreEnvironment, FrontendPhaseResult>(
        "Frontend", "Compiler frontend",
) {
    override fun outputIfNotEnabled(
            phaseConfig: PhaseConfigurationService,
            phaserState: PhaserState<KotlinCoreEnvironment>,
            context: FrontendContext,
            input: KotlinCoreEnvironment
    ): FrontendPhaseResult =
            FrontendPhaseResult.ShouldNotGenerateCode

    override fun phaseBody(context: FrontendContext, input: KotlinCoreEnvironment): FrontendPhaseResult {
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
        } while(analysisResult is AnalysisResult.RetryWithAdditionalRoots)

        val moduleDescriptor = analysisResult.moduleDescriptor
        val bindingContext = analysisResult.bindingContext

        return if (analysisResult.shouldGenerateCode) {
            FrontendPhaseResult.Full(moduleDescriptor, bindingContext, context.frontendServices, input)
        } else {
            FrontendPhaseResult.ShouldNotGenerateCode
        }
    }
}

internal fun <T: FrontendContext> PhaseEngine<T>.runFrontend(environment: KotlinCoreEnvironment): FrontendPhaseResult {
    return this.runPhase(context, FrontendPhase, environment)
}