/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.backend.wasm.compileWasmIrToBinary
import org.jetbrains.kotlin.backend.wasm.linkWasmIr
import org.jetbrains.kotlin.cli.pipeline.executePhaseIsolatedWithActions
import org.jetbrains.kotlin.cli.pipeline.web.WebLoadedIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmIrLinkingPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmIrLoweringPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmSingleModuleBackendIrGenerationPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.js.config.outputDir
import org.jetbrains.kotlin.js.config.outputName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.backend.ir.DeserializedFromKlibBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.wasm.config.*
import org.jetbrains.kotlin.wasm.test.PrecompileSetup
import org.jetbrains.kotlin.wasm.test.handlers.getWasmTestOutputDirectory
import org.jetbrains.kotlin.wasm.test.precompiledKotlinTestOutputName
import org.jetbrains.kotlin.wasm.test.precompiledStdlibOutputName
import java.io.File

class WasmLoweringSingleModuleFacade(testServices: TestServices) :
    BackendFacade<IrBackendInput, BinaryArtifacts.Wasm>(testServices, BackendKinds.IrBackend, ArtifactKinds.Wasm) {

    override fun shouldTransform(module: TestModule): Boolean {
        require(with(testServices.defaultsProvider) { backendKind == inputKind && artifactKind == outputKind })
        return true
    }

    private fun configureModuleResolutionMap(configuration: CompilerConfiguration,currentSetup: PrecompileSetup) {
        val stdlibInitFile = File(currentSetup.stdlibOutputDir, "$precompiledStdlibOutputName.mjs")
        val kotlinTestInitFile = File(currentSetup.kotlinTestOutputDir, "$precompiledKotlinTestOutputName.mjs")

        val outputDir = testServices.getWasmTestOutputDirectory()

        val relativeStdlibPath = stdlibInitFile.relativeTo(outputDir).path.replace('\\', '/').substringBeforeLast('.')
        val relativeKotlinTestPath = kotlinTestInitFile.relativeTo(outputDir).path.replace('\\', '/').substringBeforeLast('.')

        configuration.wasmDependencyResolutionMap = "<kotlin>:$relativeStdlibPath,<kotlin-test>:$relativeKotlinTestPath"
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.Wasm {
        require(inputArtifact is DeserializedFromKlibBackendInput<*>)
        val cliInputArtifact = inputArtifact.cliArtifact as? WebLoadedIrPipelineArtifact
            ?: testInfraError("WasmLoweringSingleModuleFacade expects WebLoadedIrPipelineArtifact")
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val moduleInfo = inputArtifact.moduleInfo

        val testPackage = extractTestPackage(testServices)
        configuration.wasmTestBoxFunctionToExport = FqName.fromSegments(listOfNotNull(testPackage, "box"))

        with(configuration) {
            configureWith(testServices.moduleStructure.allDirectives)
            outputDir = testServices.getWasmTestOutputDirectory()
        }

        configuration.perfManager?.notifyPhaseFinished(PhaseType.Initialization)

        val currentSetup = when {
            configuration.wasmForceDebugFriendlyCompilation -> PrecompileSetup.DEBUG_FRIENDLY
            configuration.wasmUseNewExceptionProposal -> PrecompileSetup.NEW_EXCEPTION_PROPOSAL
            configuration.wasmUseStackSwitchingProposal -> PrecompileSetup.STACK_SWITCHING_PROPOSAL
            else -> PrecompileSetup.REGULAR
        }
        configureModuleResolutionMap(configuration, currentSetup)

        if (WasmEnvironmentConfigurator.isMainModule(module, testServices)) {
            configuration.outputName = WasmEnvironmentConfigurator.WASM_BASE_FILE_NAME
        }

        val linkedIr = WasmIrLinkingPipelinePhase.executePhaseIsolatedWithActions(cliInputArtifact)!!
        val loweredIr = WasmIrLoweringPipelinePhase.executePhaseIsolatedWithActions(linkedIr)!!
        val compiledIr = WasmSingleModuleBackendIrGenerationPipelinePhase.executePhaseIsolatedWithActions(loweredIr)!!.backendIr.single()

        val linkedModule = linkWasmIr(compiledIr)
        val compileResult = compileWasmIrToBinary(compiledIr, linkedModule)

        return WasmCompilationSetsBinaryArtifact(
            WasmCompilationSet(compileResult)
        )
    }
}
