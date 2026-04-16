/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibSingleFileMetadataSerializer
import org.jetbrains.kotlin.cli.pipeline.Fir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebFir2IrPipelineArtifact
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.js.getSerializedData
import org.jetbrains.kotlin.ir.backend.js.serializeModuleIntoKlib
import org.jetbrains.kotlin.js.config.incrementalDataProvider
import org.jetbrains.kotlin.js.config.outputDir
import org.jetbrains.kotlin.js.config.outputName
import org.jetbrains.kotlin.js.config.produceKlibFile
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.backend.handlers.AbstractKlibAbiDumpBeforeInliningSavingHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.klibEnvironmentConfigurator
import org.jetbrains.kotlin.utils.addToStdlib.unreachableBranch
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys

class FirWasmJsKlibAbiDumpBeforeInliningSavingHandler(testServices: TestServices) :
    AbstractKlibAbiDumpBeforeInliningSavingHandler(testServices) {
    override fun serializeModule(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        require(inputArtifact is Fir2IrCliBasedOutputArtifact<*> && inputArtifact.cliArtifact is WebFir2IrPipelineArtifact) {
            "FirWasmJsKlibAbiDumpBeforeInliningSavingHandler expects Fir2IrCliBasedOutputArtifact<WebFir2IrPipelineArtifact> as input, but it's ${inputArtifact::class}"
        }
        val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val diagnosticReporter = DiagnosticsCollectorImpl()
        val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
            diagnosticReporter,
            compilerConfiguration.languageVersionSettings
        )
        val outputFile = testServices.klibEnvironmentConfigurator.getKlibArtifactFile(testServices, module.name)

        // TODO can deduplicate with WebKlibSerializationPipelinePhase?
        val (fir2IrResult, firResult, configuration) = inputArtifact.cliArtifact as WebFir2IrPipelineArtifact
        val metadataSerializer = Fir2KlibMetadataSerializer(
            configuration,
            firOutputs = firResult.outputs,
            fir2IrActualizedResult = fir2IrResult,
            produceHeaderKlib = false,
        )
        val icData = configuration.incrementalDataProvider?.getSerializedData(metadataSerializer.sourceFiles)

        serializeModuleIntoKlib(
            moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!,
            configuration = configuration,
            diagnosticReporter = irDiagnosticReporter,
            metadataSerializer = metadataSerializer,
            klibPath = outputFile.path,
            moduleFragment = inputArtifact.irModuleFragment,
            irBuiltIns = inputArtifact.irBuiltIns,
            cleanFiles = icData ?: emptyList(),
            nopack = true,
            jsOutputName = null,
            builtInsPlatform = BuiltInsPlatform.WASM,
            wasmTarget = configuration.get(WasmConfigurationKeys.WASM_TARGET, WasmTarget.JS),
        )

        return BinaryArtifacts.KLib(outputFile, diagnosticReporter)
    }
}
