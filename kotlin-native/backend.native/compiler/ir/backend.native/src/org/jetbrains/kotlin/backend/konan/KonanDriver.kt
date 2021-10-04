/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.usingJvmCInteropCallbacks
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.konan.util.usingNativeMemoryAllocator
import org.jetbrains.kotlin.utils.addToStdlib.cast

fun runTopLevelPhases(konanConfig: KonanConfig, environment: KotlinCoreEnvironment) {

    val config = konanConfig.configuration

    val targets = konanConfig.targetManager
    if (config.get(KonanConfigKeys.LIST_TARGETS) ?: false) {
        targets.list()
    }

    val context = Context(konanConfig)
    context.environment = environment
    context.phaseConfig.konanPhasesConfig(konanConfig) // TODO: Wrong place to call it

    if (konanConfig.infoArgsOnly) return

    if (!context.frontendPhase()) return

    usingNativeMemoryAllocator {
        usingJvmCInteropCallbacks {
            try {
                toplevelPhase.cast<CompilerPhase<Context, Unit, Unit>>().invokeToplevel(context.phaseConfig, context, Unit)
            } finally {
                context.disposeLlvm()
            }
        }
    }
}

// returns true if should generate code.
internal fun Context.frontendPhase(): Boolean {
    lateinit var analysisResult: AnalysisResult

    do {
        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(messageCollector,
                environment.configuration.languageVersionSettings)

        // Build AST and binding info.
        analyzerWithCompilerReport.analyzeAndReport(environment.getSourceFiles()) {
            TopDownAnalyzerFacadeForKonan.analyzeFiles(environment.getSourceFiles(), this)
        }
        if (analyzerWithCompilerReport.hasErrors()) {
            throw KonanCompilationException()
        }
        analysisResult = analyzerWithCompilerReport.analysisResult
        if (analysisResult is AnalysisResult.RetryWithAdditionalRoots) {
            environment.addKotlinSourceRoots(analysisResult.additionalKotlinRoots)
        }
    } while(analysisResult is AnalysisResult.RetryWithAdditionalRoots)

    moduleDescriptor = analysisResult.moduleDescriptor
    bindingContext = analysisResult.bindingContext

    return analysisResult.shouldGenerateCode
}