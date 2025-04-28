/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.cli.common.runPreSerializationLoweringPhases
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
import org.jetbrains.kotlin.ir.backend.js.JsPreSerializationLoweringContext
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibCheckers
import org.jetbrains.kotlin.ir.backend.js.jsLoweringsOfTheFirstPhase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.IrInliningFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.createJsTestPhaseConfig

class JsIrInliningFacade(
    testServices: TestServices,
) : IrInliningFacade<IrBackendInput>(testServices, BackendKinds.IrBackend, BackendKinds.IrBackend) {
    override fun shouldTransform(module: TestModule): Boolean {
        return module.languageVersionSettings.supportsFeature(LanguageFeature.IrInlinerBeforeKlibSerialization)
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): IrBackendInput {
        require(module.languageVersionSettings.languageVersion.usesK2)

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val diagnosticReporter = DiagnosticReporterFactory.createReporter(configuration.messageCollector)

        when (inputArtifact) {
            is Fir2IrCliBasedWebOutputArtifact -> {
                runKlibCheckers(diagnosticReporter, configuration, inputArtifact.irModuleFragment)
                val input = inputArtifact.cliArtifact.copy(diagnosticCollector = diagnosticReporter)

                if (diagnosticReporter.hasErrors) {
                    // Should errors be found by checkers, there's a chance that JsCodeOutlineLowering will throw an exception on unparseable code.
                    // In test pipeline, it's unwanted, so let's avoid crashes. Already found errors would already be enough for diagnostic tests.
                    return Fir2IrCliBasedWebOutputArtifact(input)
                }

                val output = WebKlibInliningPipelinePhase.executePhase(input)

                // The returned artifact will be stored in dependencyProvider instead of `inputArtifact`, with same kind=BackendKinds.IrBackend
                // Later, third artifact of class `JsIrDeserializedFromKlibBackendInput` might replace it again during some test pipelines.
                return Fir2IrCliBasedWebOutputArtifact(output)
            }
            is IrBackendInput.JsIrAfterFrontendBackendInput -> {
                runKlibCheckers(diagnosticReporter, configuration, inputArtifact.irModuleFragment)
                val phaseConfig = createJsTestPhaseConfig(testServices, module)
                if (diagnosticReporter.hasErrors) {
                    // Should errors be found by checkers, there's a chance that JsCodeOutlineLowering will throw an exception on unparseable code.
                    // In test pipeline, it's unwanted, so let's avoid crashes. Already found errors would already be enough for diagnostic tests.
                    return inputArtifact.copy(diagnosticReporter = diagnosticReporter)
                }

                val transformedModule = PhaseEngine(
                    phaseConfig,
                    PhaserState(),
                    JsPreSerializationLoweringContext(
                        inputArtifact.irPluginContext.irBuiltIns,
                        configuration,
                        diagnosticReporter,
                    ),
                ).runPreSerializationLoweringPhases(
                    jsLoweringsOfTheFirstPhase(module.languageVersionSettings),
                    inputArtifact.irModuleFragment,
                )

                // The returned artifact will be stored in dependencyProvider instead of `inputArtifact`, with same kind=BackendKinds.IrBackend
                // Later, third artifact of class `JsIrDeserializedFromKlibBackendInput` might replace it again during some test pipelines.
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
            JsKlibCheckers.makeChecker(
                irDiagnosticReporter,
                configuration,
                doCheckCalls = true,
                doModuleLevelChecks = false,
            )
        )
    }
}
