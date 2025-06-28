package org.jetbrains.kotlin.backend.driver

import org.jebrains.kotlin.backend.native.PhaseContext
import org.jebrains.kotlin.backend.native.driver.utilities.getDefaultIrActions
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
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.konan.target.KonanTarget

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

data class SpecialBackendChecksInput(
    val irModule: IrModuleFragment,
    val irBuiltIns: IrBuiltIns,
    val symbols: KonanSymbols,
    val target: KonanTarget,
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val SpecialBackendChecksPhase = createSimpleNamedCompilerPhase<PhaseContext, SpecialBackendChecksInput>(
    "SpecialBackendChecks",
    preactions = getDefaultIrActions(),
    postactions = getDefaultIrActions(),
) { context, input ->
    SpecialBackendChecksTraversal(context, input.symbols, input.irBuiltIns, input.target).lower(input.irModule)
}

/**
 * Kotlin/Native-specific language checks. Most importantly, it checks C/Objective-C interop restrictions.
 * TODO: Should be moved to compiler frontend after K2.
 */
internal class SpecialBackendChecksTraversal(
    private val context: PhaseContext,
    private val symbols: KonanSymbols,
    private val irBuiltIns: IrBuiltIns,
    private val target: KonanTarget,
) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
//        irFile.acceptChildrenVoid(BackendChecker(context, target, symbols, irBuiltIns, irFile))
        // EscapeAnalysisChecker only makes sense when compiling stdlib.
//        irFile.acceptChildrenVoid(EscapeAnalysisChecker(context, symbols, irFile))
    }
}

fun <T : PhaseContext> PhaseEngine<T>.runSpecialBackendChecks(input: SpecialBackendChecksInput) {
    runPhase(SpecialBackendChecksPhase, input)
}

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