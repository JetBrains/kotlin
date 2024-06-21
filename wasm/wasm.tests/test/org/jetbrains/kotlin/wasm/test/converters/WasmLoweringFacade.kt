/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.compileToLoweredIr
import org.jetbrains.kotlin.backend.wasm.compileWasm
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleFragmentGenerator
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleMetadataCache
import org.jetbrains.kotlin.backend.wasm.wasmPhases
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.dce.DceDumpNameCache
import org.jetbrains.kotlin.ir.backend.js.dce.dumpDeclarationIrSizesIfNeed
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.test.handlers.getWasmTestOutputDirectory
import org.jetbrains.kotlin.wasm.test.tools.WasmOptimizer
import java.io.File

class WasmLoweringFacade(
    testServices: TestServices,
) : BackendFacade<IrBackendInput, BinaryArtifacts.Wasm>(testServices, BackendKinds.IrBackend, ArtifactKinds.Wasm) {
    private val supportedOptimizer: WasmOptimizer = WasmOptimizer.Binaryen

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        require(module.backendKind == inputKind && module.binaryKind == outputKind)
        return WasmEnvironmentConfigurator.isMainModule(module, testServices)
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.Wasm? {
        require(WasmEnvironmentConfigurator.isMainModule(module, testServices))
        require(inputArtifact is IrBackendInput.WasmDeserializedFromKlibBackendInput)

        val moduleInfo = inputArtifact.moduleInfo
        val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")
        val phaseConfig = if (debugMode >= DebugMode.SUPER_DEBUG) {
            val outputDirBase = testServices.getWasmTestOutputDirectory()
            val dumpOutputDir = File(outputDirBase, "irdump")
            println("\n ------ Dumping phases to file://${dumpOutputDir.absolutePath}")
            PhaseConfig(
                wasmPhases,
                dumpToDirectory = dumpOutputDir.path,
                toDumpStateAfter = wasmPhases.toPhaseMap().values.toSet(),
            )
        } else {
            PhaseConfig(wasmPhases)
        }

        val mainModule = MainModule.Klib(inputArtifact.klib.absolutePath)
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val testPackage = extractTestPackage(testServices)
        val performanceManager = configuration[CLIConfigurationKeys.PERF_MANAGER]
        val generateSourceMaps = WasmEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP in testServices.moduleStructure.allDirectives
        val generateDts = WasmEnvironmentConfigurationDirectives.CHECK_TYPESCRIPT_DECLARATIONS in testServices.moduleStructure.allDirectives
        val (allModules, backendContext, typeScriptFragment) = compileToLoweredIr(
            moduleInfo,
            mainModule,
            configuration,
            performanceManager,
            phaseConfig = phaseConfig,
            exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, "box"))),
            propertyLazyInitialization = true,
            generateTypeScriptFragment = generateDts
        )
        val generateWat = debugMode >= DebugMode.DEBUG || configuration.getBoolean(WasmConfigurationKeys.WASM_GENERATE_WAT)
        val baseFileName = "index"

        val wasmModuleMetadataCache = WasmModuleMetadataCache(backendContext)
        val codeGenerator = WasmModuleFragmentGenerator(
            backendContext,
            wasmModuleMetadataCache,
            moduleInfo.symbolTable.irFactory as IrFactoryImplForWasmIC,
            allowIncompleteImplementations = false,
        )
        val wasmCompiledFileFragments = allModules.flatMap { codeGenerator.generateModule(it) }

        val compilerResult = compileWasm(
            wasmCompiledFileFragments = wasmCompiledFileFragments,
            moduleName = allModules.last().descriptor.name.asString(),
            configuration = configuration,
            typeScriptFragment = typeScriptFragment,
            baseFileName = baseFileName,
            emitNameSection = true,
            generateWat = generateWat,
            generateSourceMaps = generateSourceMaps,
        )

        val dceDumpNameCache = DceDumpNameCache()
        eliminateDeadDeclarations(allModules, backendContext, dceDumpNameCache)

        dumpDeclarationIrSizesIfNeed(System.getProperty("kotlin.wasm.dump.declaration.ir.size.to.file"), allModules, dceDumpNameCache)

        val wasmModuleMetadataCacheDce = WasmModuleMetadataCache(backendContext)
        val codeGeneratorDce = WasmModuleFragmentGenerator(
            backendContext,
            wasmModuleMetadataCacheDce,
            moduleInfo.symbolTable.irFactory as IrFactoryImplForWasmIC,
            allowIncompleteImplementations = true,
        )
        val wasmCompiledFileFragmentsDce = allModules.flatMap { codeGeneratorDce.generateModule(it) }

        val compilerResultWithDCE = compileWasm(
            wasmCompiledFileFragments = wasmCompiledFileFragmentsDce,
            moduleName = allModules.last().descriptor.name.asString(),
            configuration = configuration,
            typeScriptFragment = typeScriptFragment,
            baseFileName = baseFileName,
            emitNameSection = true,
            generateWat = generateWat,
            generateSourceMaps = generateSourceMaps,
        )

        return BinaryArtifacts.Wasm(
            compilerResult,
            compilerResultWithDCE,
            runIf(WasmEnvironmentConfigurationDirectives.RUN_THIRD_PARTY_OPTIMIZER in testServices.moduleStructure.allDirectives) {
                compilerResultWithDCE.runThirdPartyOptimizer()
            }
        )
    }

    private fun WasmCompilerResult.runThirdPartyOptimizer(): WasmCompilerResult {
        val (newWasm, newWat) = supportedOptimizer.run(wasm, withText = wat != null)
        return WasmCompilerResult(
            wat = newWat,
            jsUninstantiatedWrapper = jsUninstantiatedWrapper,
            jsWrapper = jsWrapper,
            wasm = newWasm,
            debugInformation = null,
            dts = dts
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
