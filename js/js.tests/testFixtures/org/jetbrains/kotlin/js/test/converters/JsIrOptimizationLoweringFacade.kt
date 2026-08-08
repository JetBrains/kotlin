/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.cli.pipeline.web.WebLoadedIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.js.JsIrLoweringPipelinePhase
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.test.backend.ir.DeserializedFromKlibBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.diagnostics.DiagnosticsCollectorStub
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.IrPreSerializationLoweringFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

/**
 * Runs full [JsIrLoweringPipelinePhase] and exposes lowered IR for optimization / dataflow tests.
 * Does not continue to JS codegen (unlike [JsIrLoweringFacade]).
 */
class JsIrOptimizationLoweringFacade(
    testServices: TestServices,
) : IrPreSerializationLoweringFacade<IrBackendInput>(testServices, BackendKinds.IrBackend, BackendKinds.IrBackend) {

    override fun shouldTransform(module: TestModule): Boolean =
        JsEnvironmentConfigurator.isMainModule(module, testServices)

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): IrBackendInput {
        checkTestInfrastructure(inputArtifact is DeserializedFromKlibBackendInput<*>) {
            "JsIrOptimizationLoweringFacade expects DeserializedFromKlibBackendInput, got ${inputArtifact::class.simpleName}"
        }
        val cliArtifact = inputArtifact.cliArtifact
        checkTestInfrastructure(cliArtifact is WebLoadedIrPipelineArtifact) {
            "Expected WebLoadedIrPipelineArtifact, got ${cliArtifact::class.simpleName}"
        }
        val lowered = JsIrLoweringPipelinePhase.executePhase(cliArtifact)
            ?: error("JS IR lowering failed")
        return LoweredJsIrBackendInput(
            program = LoweredJsProgram(lowered.context, lowered.mainModule, lowered.allModules),
            irMangler = inputArtifact.irMangler,
        )
    }
}

/**
 * Fully lowered linked JS IR program for dataflow / optimization handlers.
 */
class LoweredJsIrBackendInput(
    val program: LoweredJsProgram,
    override val irMangler: KotlinMangler.IrMangler,
) : IrBackendInput() {
    val context: JsIrBackendContext get() = program.context
    val allModules: List<IrModuleFragment> get() = program.allModules

    override val irModuleFragment: IrModuleFragment get() = program.mainModule
    override val irBuiltIns get() = program.context.irBuiltIns
    override val diagnosticReporter = DiagnosticsCollectorStub()
}

/**
 * Fully lowered JS IR for the linked program under test.
 */
class LoweredJsProgram(
    val context: JsIrBackendContext,
    val mainModule: IrModuleFragment,
    val allModules: List<IrModuleFragment>,
)
