/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.pipeline.executePhaseIsolatedWithActions
import org.jetbrains.kotlin.cli.pipeline.web.WebLoadedIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmIrLinkingPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmIrLoweringPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmOutputGenerationPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmSingleModuleBackendIrGenerationPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.js.config.outputDir
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.backend.ir.DeserializedFromKlibBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.fir.processErrorFromCliPhase
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.wasm.config.*
import org.jetbrains.kotlin.wasm.test.PrecompileSetup
import org.jetbrains.kotlin.wasm.test.handlers.getWasmTestOutputDirectory
import org.jetbrains.kotlin.wasm.test.precompileWasmModules
import org.jetbrains.kotlin.wasm.test.precompiledKotlinTestOutputName
import org.jetbrains.kotlin.wasm.test.precompiledStdlibOutputName
import java.io.File

/**
 * Compiles one module alone in the single-module mode (`WASM_INCLUDED_MODULE_ONLY`), against the precompiled stdlib and
 * kotlin-test which its wrapper finds through the dependency resolution map.
 *
 * @param artifactsDir the directory the artifact is going to be written into, which the resolution map is relative to;
 * by default, the output directory of the test. A grouping stage facade writes the modules of a whole batch into one directory.
 */
class WasmLoweringSingleModuleFacade(
    testServices: TestServices,
    private val artifactsDir: File? = null,
) : BackendFacade<IrBackendInput, BinaryArtifacts.Wasm>(testServices, BackendKinds.IrBackend, ArtifactKinds.Wasm) {

    override fun shouldTransform(module: TestModule): Boolean {
        require(with(testServices.defaultsProvider) { backendKind == inputKind && artifactKind == outputKind })
        return true
    }

    private fun configureModuleResolutionMap(configuration: CompilerConfiguration, currentSetup: PrecompileSetup, artifactsDir: File) {
        val stdlibInitFile = File(currentSetup.stdlibOutputDir, "$precompiledStdlibOutputName.mjs")
        val kotlinTestInitFile = File(currentSetup.kotlinTestOutputDir, "$precompiledKotlinTestOutputName.mjs")

        val relativeStdlibPath = stdlibInitFile.relativeTo(artifactsDir).path.replace('\\', '/').substringBeforeLast('.')
        val relativeKotlinTestPath = kotlinTestInitFile.relativeTo(artifactsDir).path.replace('\\', '/').substringBeforeLast('.')

        configuration.wasmDependencyResolutionMap = "<kotlin>:$relativeStdlibPath,<kotlin-test>:$relativeKotlinTestPath"
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.Wasm? {
        require(inputArtifact is DeserializedFromKlibBackendInput<*>)
        val cliInputArtifact = inputArtifact.cliArtifact as? WebLoadedIrPipelineArtifact
            ?: testInfraError("WasmLoweringSingleModuleFacade expects WebLoadedIrPipelineArtifact")
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val testPackage = extractTestPackage(testServices)
        configuration.wasmTestBoxFunctionToExport = FqName.fromSegments(listOfNotNull(testPackage, "box"))

        val artifactsDir = this.artifactsDir ?: testServices.getWasmTestOutputDirectory()
        with(configuration) {
            configureWith(testServices.moduleStructure.allDirectives)
            outputDir = artifactsDir
        }

        configuration.perfManager?.notifyPhaseFinished(PhaseType.Initialization)

        val currentSetup = when {
            configuration.wasmForceDebugFriendlyCompilation -> PrecompileSetup.DEBUG_FRIENDLY
            configuration.wasmUseNewExceptionProposal -> PrecompileSetup.NEW_EXCEPTION_PROPOSAL
            configuration.wasmUseStackSwitchingProposal -> PrecompileSetup.STACK_SWITCHING_PROPOSAL
            else -> PrecompileSetup.REGULAR
        }
        precompileWasmModules(currentSetup)
        configureModuleResolutionMap(configuration, currentSetup, artifactsDir)

        val compileResult =
            (WasmIrLinkingPipelinePhase then
                    WasmIrLoweringPipelinePhase then
                    WasmSingleModuleBackendIrGenerationPipelinePhase then
                    WasmOutputGenerationPipelinePhase)
                .executePhaseIsolatedWithActions(cliInputArtifact)?.result?.single()
                ?: return processErrorFromCliPhase(configuration, testServices)

        return WasmCompilationSetsBinaryArtifact(
            WasmCompilationSet(compileResult)
        )
    }
}
