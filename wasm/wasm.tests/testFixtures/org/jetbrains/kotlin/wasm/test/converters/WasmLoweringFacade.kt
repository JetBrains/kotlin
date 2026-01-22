/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.compileWasmIrToBinary
import org.jetbrains.kotlin.backend.wasm.linkWasmIr
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmBackendPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaseSet
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.js.config.generateDts
import org.jetbrains.kotlin.js.config.outputName
import org.jetbrains.kotlin.js.config.propertyLazyInitialization
import org.jetbrains.kotlin.js.config.sourceMap
import org.jetbrains.kotlin.js.config.useDebuggerCustomFormatters
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.FORCE_DEBUG_FRIENDLY_COMPILATION
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.GENERATE_DWARF
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_NEW_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_OLD_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.wasmDebug
import org.jetbrains.kotlin.wasm.config.wasmForceDebugFriendlyCompilation
import org.jetbrains.kotlin.wasm.config.wasmGenerateDwarf
import org.jetbrains.kotlin.wasm.config.wasmGenerateWat
import org.jetbrains.kotlin.wasm.config.wasmTarget
import org.jetbrains.kotlin.wasm.config.wasmUseNewExceptionProposal
import org.jetbrains.kotlin.wasm.test.handlers.getWasmTestOutputDirectory
import org.jetbrains.kotlin.wasm.test.tools.WasmOptimizer
import java.io.File

internal fun CompilerConfiguration.configureWith(directives: RegisteredDirectives) {
    val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
    sourceMap = WasmEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP in directives
    wasmGenerateDwarf = GENERATE_DWARF in directives
    generateDts = WasmEnvironmentConfigurationDirectives.CHECK_TYPESCRIPT_DECLARATIONS in directives
    useDebuggerCustomFormatters = debugMode >= DebugMode.DEBUG || useDebuggerCustomFormatters
    wasmUseNewExceptionProposal =
        (USE_OLD_EXCEPTION_HANDLING_PROPOSAL !in directives) && (USE_NEW_EXCEPTION_HANDLING_PROPOSAL in directives || wasmTarget == WasmTarget.WASI)
    wasmForceDebugFriendlyCompilation = FORCE_DEBUG_FRIENDLY_COMPILATION in directives
    useDebuggerCustomFormatters = debugMode >= DebugMode.DEBUG || useDebuggerCustomFormatters
    wasmGenerateWat = debugMode >= DebugMode.DEBUG || wasmGenerateWat
    propertyLazyInitialization = true
    wasmDebug = true
}

class WasmLoweringFacade(
    testServices: TestServices,
) : BackendFacade<IrBackendInput, BinaryArtifacts.Wasm>(testServices, BackendKinds.IrBackend, ArtifactKinds.Wasm) {
    private val supportedOptimizer: WasmOptimizer = WasmOptimizer.Binaryen

    override fun shouldTransform(module: TestModule): Boolean {
        require(with(testServices.defaultsProvider) { backendKind == inputKind && artifactKind == outputKind })
        return WasmEnvironmentConfigurator.isMainModule(module, testServices)
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.Wasm? {
        require(WasmEnvironmentConfigurator.isMainModule(module, testServices))
        require(inputArtifact is IrBackendInput.WasmDeserializedFromKlibBackendInput)

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val moduleInfo = inputArtifact.moduleInfo
        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val phaseConfigToConfigure = if (debugMode >= DebugMode.SUPER_DEBUG) {
            val outputDirBase = testServices.getWasmTestOutputDirectory()
            val dumpOutputDir = File(outputDirBase, "irdump")
            println("\n ------ Dumping phases to file://${dumpOutputDir.absolutePath}")
            PhaseConfig(
                toDumpStateAfter = PhaseSet.All,
                dumpToDirectory = dumpOutputDir.path,
            )
        } else {
            PhaseConfig()
        }

        val mainModule = MainModule.Klib(inputArtifact.klib.absolutePath)
        with(configuration) {
            phaseConfig = phaseConfigToConfigure
            outputName = "index"
            configureWith(testServices.moduleStructure.allDirectives)
        }

        val testPackage = extractTestPackage(testServices)
        val exportedBoxDeclaration = setOf(FqName.fromSegments(listOfNotNull(testPackage, "box")))

        configuration.perfManager?.notifyPhaseFinished(PhaseType.Initialization)

        val (noDceParameters, withDceParameters) = WasmBackendPipelinePhase.compileWholeProgramModeToWasmIrWithAndWithoutDCE(
            configuration = configuration,
            irModuleInfo = moduleInfo,
            mainModule = mainModule,
            idSignatureRetriever = moduleInfo.symbolTable.irFactory as IdSignatureRetriever,
            exportedDeclarations = exportedBoxDeclaration,
        )

        val linkedModule = linkWasmIr(noDceParameters)
        val linkedModuleDce = linkWasmIr(withDceParameters)

        val compilerResult = compileWasmIrToBinary(noDceParameters, linkedModule)
        val dceCompilerResult = compileWasmIrToBinary(withDceParameters, linkedModuleDce)

        return BinaryArtifacts.Wasm(
            compiledModule = linkedModule,
            compilerResult = compilerResult,
            compilerResultWithDCE = dceCompilerResult,
            compilerResultWithOptimizer = runIf(WasmEnvironmentConfigurationDirectives.RUN_THIRD_PARTY_OPTIMIZER in testServices.moduleStructure.allDirectives) {
                dceCompilerResult.runThirdPartyOptimizer()
            }
        )
    }

    private fun WasmCompilerResult.runThirdPartyOptimizer(): WasmCompilerResult {
        val (newWasm, newWat) = supportedOptimizer.run(wasm, withText = wat != null)
        return WasmCompilerResult(
            wat = newWat,
            jsWrapper = jsWrapper,
            wasm = newWasm,
            debugInformation = null,
            dts = dts,
            useDebuggerCustomFormatters = useDebuggerCustomFormatters,
            dynamicJsModules = dynamicJsModules,
            baseFileName = baseFileName,
        )
    }
}

fun extractTestPackage(testServices: TestServices): String? {
    val ktFiles = testServices.moduleStructure.modules.flatMap { module ->
        module.files
            .filter { it.isKtFile }
            .map {
                val project = testServices.compilerConfigurationProvider.getProject(module)
                testServices.sourceFileProvider.getKtFileForSourceFile(it, project)
            }
    }

    val fileWithBoxFunction = ktFiles.find { file ->
        file.declarations.find { it is KtNamedFunction && it.name == "box" } != null
    } ?: return null

    return fileWithBoxFunction.packageFqName.asString().takeIf { it.isNotEmpty() }
}