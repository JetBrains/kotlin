/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.pipeline.web.WebFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebKlibSerializationPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WebSerializedKlibPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.withNewDiagnosticCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.js.test.utils.JsIrIncrementalDataProvider
import org.jetbrains.kotlin.js.test.utils.jsIrIncrementalDataProvider
import org.jetbrains.kotlin.test.backend.ir.IrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.SKIP_GENERATING_KLIB
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.klibEnvironmentConfigurator
import java.io.File

class FirKlibSerializerCliJsFacade(
    testServices: TestServices,
    firstTimeCompilation: Boolean = true,
) : FirKlibSerializerCliWebFacade(testServices, firstTimeCompilation) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::JsIrIncrementalDataProvider))

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        val (klibArtifact, output) = super.sharedTransform(module, inputArtifact)

        // JS-specific IC config
        if (JsEnvironmentConfigurator.incrementalEnabled(testServices)) {
            // We have to register the KLIB artifact here because `recordIncrementalData` will use the second-stage CompilerConfiguration,
            // which will be created by `JsSecondStageEnvironmentConfigurator`, which needs the registered KLIB artifact.
            testServices.artifactsProvider.registerArtifact(module, klibArtifact)
            testServices.jsIrIncrementalDataProvider.recordIncrementalData(module, output)
        }

        return klibArtifact
    }
}

class FirKlibSerializerCliWasmFacade(
    testServices: TestServices,
    firstTimeCompilation: Boolean = true,
) : FirKlibSerializerCliWebFacade(testServices, firstTimeCompilation)

sealed class FirKlibSerializerCliWebFacade(
    testServices: TestServices,
    private val firstTimeCompilation: Boolean = true,
) : IrBackendFacade<BinaryArtifacts.KLib>(testServices, ArtifactKinds.KLib) {
    // additionalServices differ between js and wasm, so is NOT overridden here in the parent


    override fun shouldTransform(module: TestModule): Boolean {
        return testServices.defaultsProvider.backendKind == inputKind && SKIP_GENERATING_KLIB !in module.directives
    }

    protected fun sharedTransform(
        module: TestModule,
        inputArtifact: IrBackendInput,
    ): Pair<BinaryArtifacts.KLib, WebSerializedKlibPipelineArtifact> {
        require(inputArtifact is Fir2IrCliBasedOutputArtifact<*>) {
            "FirKlibSerializerCliWebFacade expects Fir2IrCliBasedOutputArtifact as input, but got ${inputArtifact::class.simpleName}"
        }
        val cliArtifact = inputArtifact.cliArtifact
        require(cliArtifact is WebFir2IrPipelineArtifact) {
            "FirKlibSerializerCliWebFacade expects WebFir2IrPipelineArtifact as input, but got ${cliArtifact::class.simpleName}"
        }
        val input = cliArtifact.withNewDiagnosticCollector(DiagnosticsCollectorImpl())

        val output = if (firstTimeCompilation) {
            WebKlibSerializationPipelinePhase.executePhase(input)
        } else {
            WebSerializedKlibPipelineArtifact(
                outputKlibPath = testServices.klibEnvironmentConfigurator.getKlibArtifactFile(testServices, module.name).absolutePath,
                configuration = cliArtifact.configuration,
            )
        }

        val klibArtifact = BinaryArtifacts.KLib(File(output.outputKlibPath), output.configuration.diagnosticsCollector)

        return Pair(klibArtifact, output)
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib =
        sharedTransform(module, inputArtifact).first

}
