/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.backend.wasm.compileWasmIrToBinary
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.backend.wasm.linkIr
import org.jetbrains.kotlin.backend.wasm.linkWasmIr
import org.jetbrains.kotlin.cli.pipeline.web.wasm.SingleModuleCompiler
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.ir.backend.js.MainModule
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
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import org.jetbrains.kotlin.wasm.config.wasmDependencyResolutionMap
import org.jetbrains.kotlin.wasm.config.wasmForceDebugFriendlyCompilation
import org.jetbrains.kotlin.wasm.config.wasmUseNewExceptionProposal
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
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val moduleInfo = inputArtifact.moduleInfo
        val mainModule = MainModule.Klib(inputArtifact.klib.absolutePath)

        val testPackage = extractTestPackage(testServices)
        val exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, "box")))

        with(configuration) {
            configureWith(testServices.moduleStructure.allDirectives)
            outputDir = testServices.getWasmTestOutputDirectory()
        }

        configuration.perfManager?.notifyPhaseFinished(PhaseType.Initialization)

        val currentSetup = when {
            configuration.wasmForceDebugFriendlyCompilation -> PrecompileSetup.DEBUG_FRIENDLY
            configuration.wasmUseNewExceptionProposal -> PrecompileSetup.NEW_EXCEPTION_PROPOSAL
            else -> PrecompileSetup.REGULAR
        }
        configureModuleResolutionMap(configuration, currentSetup)

        if (WasmEnvironmentConfigurator.isMainModule(module, testServices)) {
            configuration.outputName = WasmEnvironmentConfigurator.WASM_BASE_FILE_NAME
        }

        val irFactory = moduleInfo.symbolTable.irFactory as IrFactoryImplForWasmIC
        val compiler = SingleModuleCompiler(configuration, irFactory, isWasmStdlib = false)

        val (allModules, context) = configuration.perfManager.tryMeasurePhaseTime(PhaseType.IrLinking) {
            linkIr(moduleInfo, configuration, mainModule)
        }

        val loweredIr = configuration.perfManager.tryMeasurePhaseTime(PhaseType.IrLowering) {
            compiler.lowerIr(moduleInfo, exportedDeclarations, allModules, context)
        }

        val compiledIr = configuration.perfManager.tryMeasurePhaseTime(PhaseType.Backend) {
            compiler.compileIr(loweredIr)
        }.single()

        val linkedModule = linkWasmIr(compiledIr)
        val compileResult = compileWasmIrToBinary(compiledIr, linkedModule)

        return WasmCompilationSetsBinaryArtifact(
            WasmCompilationSet(linkedModule, compileResult)
        )
    }
}
