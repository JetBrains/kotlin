/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.backend.wasm.compileToLoweredIr
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.pipeline.web.wasm.compileWasmLoweredFragmentsForSingleModule
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys

class WasmLoweringSingleModuleFacade(testServices: TestServices) :
    BackendFacade<IrBackendInput, BinaryArtifacts.Wasm>(testServices, BackendKinds.IrBackend, ArtifactKinds.Wasm) {

    override fun shouldTransform(module: TestModule): Boolean {
        require(with(testServices.defaultsProvider) { backendKind == inputKind && artifactKind == outputKind })
        return true
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.Wasm {
        require(inputArtifact is IrBackendInput.WasmDeserializedFromKlibBackendInput)

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val moduleInfo = inputArtifact.moduleInfo

        val mainModule = MainModule.Klib(inputArtifact.klib.absolutePath)

        val testPackage = extractTestPackage(testServices)
        val exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, "box")))

        val performanceManager = configuration[CLIConfigurationKeys.PERF_MANAGER]
        performanceManager?.let {
            it.notifyPhaseFinished(PhaseType.Initialization)
            it.notifyPhaseStarted(PhaseType.TranslationToIr)
        }

        val (allModules, backendContext, _) = compileToLoweredIr(
            moduleInfo,
            mainModule,
            configuration,
            performanceManager = performanceManager,
            exportedDeclarations = exportedDeclarations,
            propertyLazyInitialization = true,
            generateTypeScriptFragment = false,
            isIncremental = true,
        )

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val generateWat = debugMode >= DebugMode.DEBUG || configuration.getBoolean(WasmConfigurationKeys.WASM_GENERATE_WAT)

        val compilerResult = compileWasmLoweredFragmentsForSingleModule(
            configuration = configuration,
            loweredIrFragments = allModules,
            backendContext = backendContext,
            signatureRetriever = moduleInfo.symbolTable.irFactory as IdSignatureRetriever,
            generateWat = generateWat,
            wasmDebug = true,
        )

        return BinaryArtifacts.Wasm(
            compilerResult,
            compilerResult,
            null,
        )
    }
}
