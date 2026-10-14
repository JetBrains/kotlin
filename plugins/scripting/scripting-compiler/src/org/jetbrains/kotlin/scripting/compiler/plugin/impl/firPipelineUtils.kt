/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.backend.common.actualizer.IrActualizedResult
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelinePhase.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmWriteOutputsPhase.writeOutputsIfNeeded
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.outputDirectory
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrPluginContext
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.backend.utils.extractFirDeclarations
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.PotentiallyIncorrectPhaseTimeMeasurement
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.asSuccess

/**
 * Used for marking API used in the legacy K2 CLI pipeline.
 */
@RequiresOptIn("Consider using the new pipeline API from `org.jetbrains.kotlin.cli.pipeline.jvm`")
annotation class LegacyK2CliPipeline

@LegacyK2CliPipeline
internal fun convertAnalyzedFirToIr(
    configuration: CompilerConfiguration,
    targetId: TargetId,
    frontendOutput: AllModulesFrontendOutput,
    environment: ModuleCompilerEnvironment
): ModuleCompilerIrBackendInput {
    val extensions = JvmFir2IrExtensions()

    (
        val moduleFragment = irModuleFragment,
        val components,
        val pluginContext,
        val irActualizedResult,
        val symbolTable,
    ) =
        frontendOutput.convertToIrAndActualizeForJvm(
            extensions, configuration, environment.diagnosticsReporter,
            configuration.getCompilerExtensions(IrGenerationExtension),
        )

    return ModuleCompilerIrBackendInput(
        targetId,
        configuration,
        extensions,
        moduleFragment,
        components,
        pluginContext,
        irActualizedResult,
        symbolTable
    )
}

/**
 * Converts the analyzed [frontendOutput] to IR and generates the code from it, unless errors are reported before or during the conversion.
 * The diagnostics collected by the [environment] reporter are passed to the [messageCollector] once, on failure or after the codegen.
 */
@LegacyK2CliPipeline
internal fun generateCodeIfNoErrors(
    configuration: CompilerConfiguration,
    targetId: TargetId,
    frontendOutput: AllModulesFrontendOutput,
    environment: ModuleCompilerEnvironment,
    messageCollector: ScriptDiagnosticsMessageCollector,
    renderDiagnosticName: Boolean,
): ResultWithDiagnostics<Pair<ModuleCompilerIrBackendInput, GenerationState>> {
    val diagnosticsReporter = environment.diagnosticsReporter

    fun reportDiagnostics() = diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)

    fun reportedFailure(): ResultWithDiagnostics.Failure {
        reportDiagnostics()
        return failure(messageCollector)
    }

    if (diagnosticsReporter.hasErrors) return reportedFailure()

    val irInput = convertAnalyzedFirToIr(configuration, targetId, frontendOutput, environment)
    if (diagnosticsReporter.hasErrors) return reportedFailure()

    val generationState = generateCodeFromIr(irInput, environment)
    if (diagnosticsReporter.hasErrors) return reportedFailure()

    reportDiagnostics()
    return (irInput to generationState).asSuccess()
}

@LegacyK2CliPipeline
internal fun generateCodeFromIr(
    input: ModuleCompilerIrBackendInput,
    environment: ModuleCompilerEnvironment
): GenerationState {
    val generationState = GenerationState(
        environment.projectEnvironment.project,
        input.irModuleFragment.descriptor,
        input.configuration,
        ClassBuilderFactories.BINARIES,
        targetId = input.targetId,
        moduleName = input.targetId.name,
        jvmBackendClassResolver = FirJvmBackendClassResolver(input.components),
        diagnosticReporter = environment.diagnosticsReporter,
    )

    val performanceManager = input.configuration.perfManager
    @OptIn(PotentiallyIncorrectPhaseTimeMeasurement::class)
    performanceManager?.notifyCurrentPhaseFinishedIfNeeded() // It should be `notifyIRGenerationFinished`, but this phase not always started or already finished
    lateinit var codegenFactory: JvmIrCodegenFactory
    val codegenInput = performanceManager.tryMeasurePhaseTime(PhaseType.IrLowering) {
        val backendInput = JvmIrCodegenFactory.BackendInput(
            input.irModuleFragment,
            input.pluginContext.irBuiltIns,
            input.symbolTable,
            input.components.irProviders,
            debuggerExtensions = null,
            FirJvmBackendExtension(
                input.components,
                input.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations()
            ),
            input.pluginContext,
        )

        codegenFactory = JvmIrCodegenFactory(input.configuration)
        codegenFactory.invokeLowerings(generationState, backendInput)
    }

    codegenFactory.invokeCodegen(codegenInput)

    // It's allowed to call `tryMeasurePhaseTime` multiple times on the same phase (`Backend`)
    performanceManager.tryMeasurePhaseTime(PhaseType.Backend) {
        if (input.configuration.outputDirectory != null) {
            writeOutputsIfNeeded(
                environment.projectEnvironment.project,
                input.configuration,
                environment.diagnosticsReporter.hasErrors,
                listOf(generationState),
                mainClassFqName = null,
            )
        }
    }

    return generationState
}


@LegacyK2CliPipeline
internal data class ModuleCompilerEnvironment(
    val projectEnvironment: VfsBasedProjectEnvironment,
    val diagnosticsReporter: BaseDiagnosticsCollector
)

internal data class ModuleCompilerIrBackendInput(
    val targetId: TargetId,
    val configuration: CompilerConfiguration,
    val extensions: JvmFir2IrExtensions,
    val irModuleFragment: IrModuleFragment,
    val components: Fir2IrComponents,
    val pluginContext: Fir2IrPluginContext,
    val irActualizedResult: IrActualizedResult?,
    val symbolTable: SymbolTable,
)
