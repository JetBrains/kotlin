/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.cli.pipeline.web.JsFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebKlibInliningPipelinePhase
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.IrPreSerializationLoweringFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

class JsIrPreSerializationLoweringFacade(
    testServices: TestServices,
) : IrPreSerializationLoweringFacade<IrBackendInput>(testServices, BackendKinds.IrBackend, BackendKinds.IrBackend) {
    override fun shouldTransform(module: TestModule): Boolean {
        // `transform` needs to be executed, no matter if IrInlinerBeforeKlibSerialization is enabled or not
        // to replace diagnosticReporter, so diagnostics from the previous FIR2IR stage would not be reported twice
        return true
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): IrBackendInput {
        require(module.languageVersionSettings.languageVersion.usesK2)

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val diagnosticReporter = DiagnosticReporterFactory.createReporter(configuration.messageCollector)

        when (inputArtifact) {
            is Fir2IrCliBasedOutputArtifact<*> -> {
                val cliArtifact = inputArtifact.cliArtifact
                require(cliArtifact is JsFir2IrPipelineArtifact) {
                    "Fir2IrCliBasedOutputArtifact should have JsFir2IrPipelineArtifact as cliArtifact, but has ${cliArtifact::class}"
                }
                val input = cliArtifact.copy(diagnosticCollector = diagnosticReporter)
                if (!module.languageVersionSettings.supportsFeature(LanguageFeature.IrInlinerBeforeKlibSerialization)) {
                    return Fir2IrCliBasedOutputArtifact(input)
                }
                val output = WebKlibInliningPipelinePhase.executePhase(input)

                // The returned artifact will be stored in dependencyProvider instead of `inputArtifact`, with same kind=BackendKinds.IrBackend
                // Later, third artifact of class `JsIrDeserializedFromKlibBackendInput` might replace it again during some test pipelines.
                return Fir2IrCliBasedOutputArtifact(output)
            }
            else -> {
                throw IllegalArgumentException("Unexpected inputArtifact type: ${inputArtifact.javaClass.simpleName}")
            }
        }
    }
}
