package org.jetbrains.kotlin.backend.driver

import org.jetbrains.kotlin.backend.common.phaser.PhaseContext
import org.jetbrains.kotlin.backend.common.phaser.getDefaultIrActions
import org.jetbrains.kotlin.backend.Fir2IrOutput
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.KotlinBackendIrHolder
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.KonanCompilationException
import org.jetbrains.kotlin.backend.konan.NativePreSerializationLoweringContext
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.runPreSerializationLoweringPhases
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.inline.konan.nativeLoweringsOfTheFirstPhase
import org.jetbrains.kotlin.konan.target.KonanTarget

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun <T : PhaseContext> PhaseEngine<T>.runPreSerializationLowerings(fir2IrOutput: Fir2IrOutput, environment: KotlinCoreEnvironment): Fir2IrOutput {
    val diagnosticReporter = DiagnosticReporterFactory.createReporter(environment.configuration.messageCollector)
    val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
        diagnosticReporter,
        environment.configuration.languageVersionSettings
    )
    val loweringContext = NativePreSerializationLoweringContext(
        fir2IrOutput.fir2irActualizedResult.irBuiltIns,
        environment.configuration,
        irDiagnosticReporter,
    )
    val preSerializationLowered = newEngine(loweringContext) { engine ->
        engine.runPreSerializationLoweringPhases(
            fir2IrOutput.fir2irActualizedResult,
            nativeLoweringsOfTheFirstPhase(environment.configuration.languageVersionSettings),
        )
    }
    // TODO: After KT-73624, generate native diagnostic tests for `compiler/testData/diagnostics/irInliner/syntheticAccessors`
    FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
        diagnosticReporter,
        environment.configuration.messageCollector,
        environment.configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME),
    )
    if (diagnosticReporter.hasErrors) {
        throw KonanCompilationException("Compilation failed: there were some diagnostics during IR Inliner")
    }

    return fir2IrOutput.copy(
        fir2irActualizedResult = preSerializationLowered,
    )
}