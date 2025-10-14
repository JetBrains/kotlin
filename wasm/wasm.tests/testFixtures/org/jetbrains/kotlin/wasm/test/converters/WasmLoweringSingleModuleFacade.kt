/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.backend.wasm.compileToLoweredIr
import org.jetbrains.kotlin.cli.pipeline.web.wasm.compileWasmLoweredFragmentsForSingleModule
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_NEW_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.test.handlers.getWasmTestOutputDirectory
import org.jetbrains.kotlin.wasm.test.precompiledKotlinTestNewExceptionsOutputDir
import org.jetbrains.kotlin.wasm.test.precompiledKotlinTestOutputDir
import org.jetbrains.kotlin.wasm.test.precompiledKotlinTestOutputName
import org.jetbrains.kotlin.wasm.test.precompiledStdlibNewExceptionsOutputDir
import org.jetbrains.kotlin.wasm.test.precompiledStdlibOutputDir
import org.jetbrains.kotlin.wasm.test.precompiledStdlibOutputName
import java.io.File

class WasmLoweringSingleModuleFacade(testServices: TestServices) :
    BackendFacade<IrBackendInput, BinaryArtifacts.Wasm>(testServices, BackendKinds.IrBackend, ArtifactKinds.Wasm) {


    private fun getJsModuleImportString(newExceptionProposal: Boolean): String {
        val outputDirBase = testServices.getWasmTestOutputDirectory()

        val (stdlibOutputDir, testOutputDir) = when (newExceptionProposal) {
            true -> precompiledStdlibNewExceptionsOutputDir to precompiledKotlinTestNewExceptionsOutputDir
            false -> precompiledStdlibOutputDir to precompiledKotlinTestOutputDir
        }

        val stdlibInitFile = File(stdlibOutputDir, "$precompiledStdlibOutputName.uninstantiated.mjs")
        val kotlinTestInitFile = File(testOutputDir, "$precompiledKotlinTestOutputName.uninstantiated.mjs")

        return """
    let stdlib = await import('${stdlibInitFile.relativeTo(outputDirBase).path.replace('\\', '/')}');
    imports['<kotlin>'] = (await stdlib.instantiate()).exports;
    
    let test = await import('${kotlinTestInitFile.relativeTo(outputDirBase).path.replace('\\', '/')}');
    imports['<kotlin-test>'] = (await test.instantiate(imports)).exports;
        """
    }

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
        val performanceManager = configuration.perfManager
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
            disableCrossFileOptimisations = true,
        )

        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val generateWat = debugMode >= DebugMode.DEBUG || configuration.getBoolean(WasmConfigurationKeys.WASM_GENERATE_WAT)

        val useNewExceptionProposal = USE_NEW_EXCEPTION_HANDLING_PROPOSAL in testServices.moduleStructure.allDirectives
        val singleModulePreloadJs = getJsModuleImportString(newExceptionProposal = useNewExceptionProposal)
        val outputName = "index".takeIf { WasmEnvironmentConfigurator.isMainModule(module, testServices) }

        val compilerResult = compileWasmLoweredFragmentsForSingleModule(
            configuration = configuration,
            loweredIrFragments = allModules,
            backendContext = backendContext,
            signatureRetriever = moduleInfo.symbolTable.irFactory as IdSignatureRetriever,
            stdlibIsMainModule = false,
            generateWat = generateWat,
            wasmDebug = true,
            outputFileNameBase = outputName,
            singleModulePreloadJs = singleModulePreloadJs,
        )

        return BinaryArtifacts.Wasm(
            compilerResult,
            compilerResult,
            null,
        )
    }
}
