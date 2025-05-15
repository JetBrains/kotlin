/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.cli.pipeline.web.JsFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.JsSerializedKlibPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebKlibSerializationPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WebKlibSerializationPipelinePhase.serializeFirKlib
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.js.config.perModuleOutputName
import org.jetbrains.kotlin.js.config.produceKlibDir
import org.jetbrains.kotlin.js.config.wasmCompilation
import org.jetbrains.kotlin.js.test.utils.JsIrIncrementalDataProvider
import org.jetbrains.kotlin.js.test.utils.jsIrIncrementalDataProvider
import org.jetbrains.kotlin.test.backend.AbstractKlibSerializerFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.SKIP_GENERATING_KLIB
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.processErrorFromCliPhase
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.service
import org.jetbrains.kotlin.wasm.config.wasmTarget
import java.io.File

class FirKlibSerializerCliWebFacade(
    testServices: TestServices,
    private val firstTimeCompilation: Boolean = true,
) : AbstractKlibSerializerFacade(testServices) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::JsIrIncrementalDataProvider))

    override fun shouldTransform(module: TestModule): Boolean {
        return testServices.defaultsProvider.backendKind == inputKind && SKIP_GENERATING_KLIB !in module.directives
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib? {
        require(inputArtifact is Fir2IrCliBasedOutputArtifact<*>) {
            "FirKlibSerializerCliWebFacade expects Fir2IrCliBasedWebOutputArtifact as input"
        }
        val cliArtifact = inputArtifact.cliArtifact
        require(cliArtifact is JsFir2IrPipelineArtifact) {
            "FirKlibSerializerCliWebFacade expects JsFir2IrPipelineArtifact as input"
        }
        val messageCollector = cliArtifact.configuration.messageCollector
        val diagnosticReporter = DiagnosticReporterFactory.createPendingReporter(messageCollector)
        val input = cliArtifact.copy(diagnosticCollector = diagnosticReporter)

        val output = if (firstTimeCompilation) {
            WebKlibSerializationPipelinePhase.executePhase(input)
                ?: return processErrorFromCliPhase(messageCollector, testServices)
        } else {
            JsSerializedKlibPipelineArtifact(
                outputKlibPath = JsEnvironmentConfigurator.getKlibArtifactFile(testServices, module.name).absolutePath,
                diagnosticsCollector = diagnosticReporter,
                configuration = cliArtifact.configuration,
            )
        }

        if (JsEnvironmentConfigurator.incrementalEnabled(testServices)) {
            testServices.jsIrIncrementalDataProvider.recordIncrementalData(module, output)
        }

        return BinaryArtifacts.KLib(File(output.outputKlibPath), output.diagnosticsCollector)
    }

    override fun serializeBare(
        module: TestModule,
        inputArtifact: IrBackendInput,
        outputKlibArtifactFile: File,
        configuration: CompilerConfiguration,
        diagnosticReporter: BaseDiagnosticsCollector,
    ) {
        require(inputArtifact is Fir2IrCliBasedOutputArtifact<*>) {
            "FirKlibSerializerCliWebFacade expects Fir2IrCliBasedWebOutputArtifact as input"
        }
        val cliArtifact = inputArtifact.cliArtifact
        require(cliArtifact is JsFir2IrPipelineArtifact) {
            "FirKlibSerializerCliWebFacade expects JsFir2IrPipelineArtifact as input"
        }

        serializeFirKlib(
            moduleStructure = cliArtifact.moduleStructure,
            firOutputs = cliArtifact.analyzedFirOutput.output,
            fir2IrActualizedResult = cliArtifact.result,
            outputKlibPath = outputKlibArtifactFile.path,
            nopack = configuration.produceKlibDir,
            messageCollector = configuration.messageCollector,
            diagnosticsReporter = diagnosticReporter,
            jsOutputName = configuration.perModuleOutputName,
            useWasmPlatform = configuration.wasmCompilation,
            wasmTarget = configuration.wasmTarget,
            hasErrors = cliArtifact.hasErrors,
        )
    }
}
