/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.pipeline.web.WebFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebKlibSerializationPipelinePhase
import org.jetbrains.kotlin.test.backend.handlers.AbstractKlibAbiDumpBeforeInliningSavingHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.klibEnvironmentConfigurator

class FirWasmJsKlibAbiDumpBeforeInliningSavingHandler(testServices: TestServices) :
    AbstractKlibAbiDumpBeforeInliningSavingHandler(testServices) {
    override fun serializeModule(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        require(inputArtifact is Fir2IrCliBasedOutputArtifact<*>) {
            "FirWasmJsKlibAbiDumpBeforeInliningSavingHandler expects Fir2IrCliBasedOutputArtifact as input, but got ${inputArtifact::class.simpleName}"
        }
        val cliArtifact = inputArtifact.cliArtifact
        require(cliArtifact is WebFir2IrPipelineArtifact) {
            "FirWasmJsKlibAbiDumpBeforeInliningSavingHandler expects WebFir2IrPipelineArtifact as cliArtifact, but got ${cliArtifact::class.simpleName}"
        }

        val artifact = WebKlibSerializationPipelinePhase.executePhase(cliArtifact)

        val outputFile = testServices.klibEnvironmentConfigurator.getKlibArtifactFile(testServices, module.name)
        assert(outputFile.path == artifact.outputKlibPath) {
            "Klib path mismatch: expected ${outputFile.path}, got ${artifact.outputKlibPath}"
        }

        return BinaryArtifacts.KLib(outputFile, artifact.configuration.diagnosticsCollector)
    }
}
