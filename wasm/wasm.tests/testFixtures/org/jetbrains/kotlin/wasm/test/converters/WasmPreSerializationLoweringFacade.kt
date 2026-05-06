/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.cli.pipeline.web.WebFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebKlibInliningPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.withNewDiagnosticCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.WasmAfterFrontendBackendInput
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.IrPreSerializationLoweringFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class WasmPreSerializationLoweringFacade(
    testServices: TestServices,
) : IrPreSerializationLoweringFacade<IrBackendInput>(testServices, BackendKinds.IrBackend, BackendKinds.IrBackend) {
    override fun shouldTransform(module: TestModule): Boolean {
        return module.languageVersionSettings.languageVersion.usesK2
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): IrBackendInput {
        require(module.languageVersionSettings.languageVersion.usesK2)

        require(inputArtifact is Fir2IrCliBasedOutputArtifact<*>) {
            "WasmPreSerializationLoweringFacade expects Fir2IrCliBasedOutputArtifact as input, but got ${inputArtifact::class.simpleName}"
        }
        val cliArtifact = inputArtifact.cliArtifact
        require(cliArtifact is WebFir2IrPipelineArtifact) {
            "WasmPreSerializationLoweringFacade expects WebFir2IrPipelineArtifact as cliArtifact, but got ${cliArtifact::class.simpleName}"
        }

        val diagnosticReporter = DiagnosticsCollectorImpl()

        val input = cliArtifact.withNewDiagnosticCollector(diagnosticReporter)

        val output = WebKlibInliningPipelinePhase.executePhase(input)

        // The returned artifact will be stored in dependencyProvider instead of `inputArtifact`, with same kind=BackendKinds.IrBackend
        return Fir2IrCliBasedOutputArtifact(output)
    }

}
