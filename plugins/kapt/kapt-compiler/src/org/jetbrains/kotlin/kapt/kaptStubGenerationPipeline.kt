/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.withNewDiagnosticCollector
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.kapt.base.KaptOptions
import org.jetbrains.kotlin.kapt.base.util.KaptLogger
import org.jetbrains.kotlin.kapt.stubs.OriginCollectingClassBuilderFactory
import org.jetbrains.kotlin.util.PhaseType

internal fun compileForStubGeneration(
    disposable: Disposable,
    configuration: CompilerConfiguration,
    options: KaptOptions,
    logger: KaptLogger,
    withJdk: Boolean,
    onFrontendOutput: (JvmFrontendPipelineArtifact) -> Boolean = { false },
): KaptContextForStubGeneration? {
    val frontendInput = ConfigurationPipelineArtifact(configuration, disposable)
        .withNewDiagnosticCollector(DiagnosticsCollectorImpl())
    val frontendOutput = JvmFrontendPipelinePhase.executePhase(frontendInput) ?: return null

    if (onFrontendOutput(frontendOutput)) return null

    configuration.perfManager?.notifyPhaseFinished(PhaseType.Analysis)

    // Every phase is run with a fresh diagnostics collector, because KAPT is expected to compile incomplete code, while each phase
    // early-returns if there are already errors in the collector.
    val fir2IrOutput = JvmFir2IrPipelinePhase.executePhase(
        frontendOutput.withNewDiagnosticCollector(DiagnosticsCollectorImpl()),
        irGenerationExtensions = emptyList(),
    ) ?: return null

    val builderFactory = OriginCollectingClassBuilderFactory(ClassBuilderMode.KAPT3)
    val backendInput = fir2IrOutput.withNewDiagnosticCollector(DiagnosticsCollectorImpl())
    backendInput.configuration.put(JvmBackendPipelinePhase.customClassBuilderFactory, builderFactory)

    val generationState = JvmBackendPipelinePhase.executePhase(backendInput).outputs.singleOrNull() ?: return null

    return KaptContextForStubGeneration(
        options, withJdk, logger, builderFactory.compiledClasses, builderFactory.origins,
        backendInput.configuration,
        generationState.factory,
        frontendOutput.frontendOutput.outputs.flatMap { it.fir },
        fir2IrOutput.result.irBuiltIns,
    )
}
