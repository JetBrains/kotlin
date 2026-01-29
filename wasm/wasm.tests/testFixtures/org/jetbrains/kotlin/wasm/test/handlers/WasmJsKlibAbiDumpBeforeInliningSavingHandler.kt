/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.backend.common.lower.inline.SignaturesComputationLowering
import org.jetbrains.kotlin.backend.wasm.WasmPreSerializationLoweringContext
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.js.serializeModuleIntoKlib
import org.jetbrains.kotlin.js.config.outputDir
import org.jetbrains.kotlin.js.config.outputName
import org.jetbrains.kotlin.js.config.produceKlibFile
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.backend.handlers.AbstractKlibAbiDumpBeforeInliningSavingHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.klibEnvironmentConfigurator
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys

class FirWasmJsKlibAbiDumpBeforeInliningSavingHandler(testServices: TestServices) :
    AbstractKlibAbiDumpBeforeInliningSavingHandler(testServices) {
    override fun serializeModule(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        require(inputArtifact is IrBackendInput.WasmAfterFrontendBackendInput) {
            "FirWasmJsKlibAbiDumpBeforeInliningSavingHandler expects WasmAfterFrontendBackendInput as input, but it's ${inputArtifact::class}"
        }
        val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val diagnosticReporter = DiagnosticReporterFactory.createReporter()
        val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
            diagnosticReporter.deduplicating(),
            compilerConfiguration.languageVersionSettings
        )
        val outputFile = testServices.klibEnvironmentConfigurator.getKlibArtifactFile(testServices, module.name)

        val configuration = compilerConfiguration.copy().apply {
            produceKlibFile = true
            outputDir = outputFile.parentFile
            outputName = outputFile.name.removeSuffix(".klib")
        }

        val preSerializationLoweringContext = WasmPreSerializationLoweringContext(
            irBuiltIns = inputArtifact.irBuiltIns,
            configuration = configuration,
            diagnosticReporter = irDiagnosticReporter
        )
        SignaturesComputationLowering(
            preSerializationLoweringContext
        ).lower(inputArtifact.irModuleFragment)

        serializeModuleIntoKlib(
            moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!,
            configuration = configuration,
            diagnosticReporter = irDiagnosticReporter,
            metadataSerializer = inputArtifact.metadataSerializer,
            klibPath = outputFile.path,
            moduleFragment = inputArtifact.irModuleFragment,
            irBuiltIns = inputArtifact.irBuiltIns,
            cleanFiles = inputArtifact.icData,
            nopack = true,
            jsOutputName = null,
            builtInsPlatform = BuiltInsPlatform.WASM,
            wasmTarget = configuration.get(WasmConfigurationKeys.WASM_TARGET, WasmTarget.JS),
            declarationTable = preSerializationLoweringContext.declarationTable
        )

        return BinaryArtifacts.KLib(outputFile, diagnosticReporter)
    }
}
