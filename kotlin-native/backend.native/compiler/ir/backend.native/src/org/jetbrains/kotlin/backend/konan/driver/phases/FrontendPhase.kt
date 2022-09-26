/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.TopDownAnalyzerFacadeForKonan
import org.jetbrains.kotlin.backend.konan.driver.context.ConfigChecks
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.BindingContext

sealed class FrontendPhaseResult() {
    object ShouldNotGenerateCode : FrontendPhaseResult()

    class Full(
            val moduleDescriptor: ModuleDescriptor,
            val bindingContext: BindingContext,
            val frontendServices: FrontendServices,
    ) : FrontendPhaseResult()
}

class FrontendContext(
        override val config: KonanConfig
) : ConfigChecks {
    lateinit var frontendServices: FrontendServices
}

class FrontendPhase {
    fun run(context: FrontendContext, messageCollector: MessageCollector, environment: KotlinCoreEnvironment): FrontendPhaseResult {
        lateinit var analysisResult: AnalysisResult

        do {
            val analyzerWithCompilerReport = AnalyzerWithCompilerReport(
                    messageCollector,
                    environment.configuration.languageVersionSettings,
                    environment.configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
            )

            // Build AST and binding info.
            analyzerWithCompilerReport.analyzeAndReport(environment.getSourceFiles()) {
                TopDownAnalyzerFacadeForKonan.analyzeFiles(environment.getSourceFiles(), context)
            }
            if (analyzerWithCompilerReport.hasErrors()) {
                throw KonanCompilationException()
            }
            analysisResult = analyzerWithCompilerReport.analysisResult
            if (analysisResult is AnalysisResult.RetryWithAdditionalRoots) {
                environment.addKotlinSourceRoots(analysisResult.additionalKotlinRoots)
            }
        } while(analysisResult is AnalysisResult.RetryWithAdditionalRoots)

        val moduleDescriptor = analysisResult.moduleDescriptor
        val bindingContext = analysisResult.bindingContext

        return if (analysisResult.shouldGenerateCode) {
            FrontendPhaseResult.Full(moduleDescriptor, bindingContext, context.frontendServices)
        } else {
            FrontendPhaseResult.ShouldNotGenerateCode
        }
    }
}