/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.wasm.WasmPreSerializationLoweringContext
import org.jetbrains.kotlin.backend.wasm.wasmLoweringsOfTheFirstPhase
import org.jetbrains.kotlin.cli.common.runPreSerializationLoweringPhases
import org.jetbrains.kotlin.cli.pipeline.web.JsFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebKlibInliningPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.js.wasm.WasmKlibCheckers
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.IrPreSerializationLoweringFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.createJsTestPhaseConfig

class WasmPreSerializationLoweringFacade(
    testServices: TestServices,
) : IrPreSerializationLoweringFacade<IrBackendInput>(testServices, BackendKinds.IrBackend, BackendKinds.IrBackend) {
    override fun shouldTransform(module: TestModule): Boolean {
        return module.languageVersionSettings.supportsFeature(LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization)
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): IrBackendInput {
        require(module.languageVersionSettings.languageVersion.usesK2)

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val diagnosticReporter = DiagnosticReporterFactory.createReporter(configuration.messageCollector)

        when (inputArtifact) {
            is Fir2IrCliBasedOutputArtifact<*> -> {
                val cliArtifact = inputArtifact.cliArtifact
                // TODO: When KT-74671 would be implemented, feel free to adjust the following code for possibly another artifact type
                require(cliArtifact is JsFir2IrPipelineArtifact) {
                    "Fir2IrCliBasedOutputArtifact should have JsFir2IrPipelineArtifact as cliArtifact, but has ${cliArtifact::class}"
                }
                val input = cliArtifact.copy(diagnosticCollector = diagnosticReporter)

                if (diagnosticReporter.hasErrors) {
                    // Should errors be found by checkers, there's a chance that JsCodeOutlineLowering will throw an exception on unparseable code.
                    // In test pipeline, it's unwanted, so let's avoid crashes. Already found errors would already be enough for diagnostic tests.
                    return Fir2IrCliBasedOutputArtifact(input)
                }

                val output = WebKlibInliningPipelinePhase.executePhase(input)

                // The returned artifact will be stored in dependencyProvider instead of `inputArtifact`, with same kind=BackendKinds.IrBackend
                // Later, third artifact of class `JsIrDeserializedFromKlibBackendInput` might replace it again during some test pipelines.
                return Fir2IrCliBasedOutputArtifact(output)
            }
            is IrBackendInput.WasmAfterFrontendBackendInput -> {
                // TODO: When KT-74671 would be implemented, the following code would be never used and is subject to be deleted
                runKlibCheckers(diagnosticReporter, configuration, inputArtifact.irModuleFragment)
                val phaseConfig = createJsTestPhaseConfig(testServices, module)
                if (diagnosticReporter.hasErrors) {
                    // Should errors be found by checkers, there's a chance that some lowering will throw an exception on unparseable code.
                    // In test pipeline, it's unwanted, so let's avoid crashes. Already found errors would already be enough for diagnostic tests.
                    return inputArtifact.copy(diagnosticReporter = diagnosticReporter)
                }

                val transformedModule = PhaseEngine(
                    phaseConfig,
                    PhaserState(),
                    WasmPreSerializationLoweringContext(
                        inputArtifact.irPluginContext.irBuiltIns,
                        configuration,
                        diagnosticReporter,
                    ),
                ).runPreSerializationLoweringPhases(
                    wasmLoweringsOfTheFirstPhase(module.languageVersionSettings),
                    inputArtifact.irModuleFragment,
                )

                // The returned artifact will be stored in dependencyProvider instead of `inputArtifact`, with same kind=BackendKinds.IrBackend
                // Later, third artifact of class `WasmDeserializedFromKlibBackendInput` might replace it again during some test pipelines.
                return inputArtifact.copy(irModuleFragment = transformedModule, diagnosticReporter = diagnosticReporter)
            }
            else -> {
                throw IllegalArgumentException("Unexpected inputArtifact type: ${inputArtifact.javaClass.simpleName}")
            }
        }
    }

    private fun runKlibCheckers(
        diagnosticReporter: BaseDiagnosticsCollector,
        configuration: CompilerConfiguration,
        irModuleFragment: IrModuleFragment,
    ) {
        val irDiagnosticReporter =
            KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter.deduplicating(), configuration.languageVersionSettings)
        irModuleFragment.acceptVoid(
            WasmKlibCheckers.makeChecker(
                irDiagnosticReporter,
                configuration,
            )
        )
    }
}
