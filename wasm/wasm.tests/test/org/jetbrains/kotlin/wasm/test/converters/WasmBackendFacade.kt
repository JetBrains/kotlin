/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.backend.wasm.wasmPhases
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.dce.DceDumpNameCache
import org.jetbrains.kotlin.ir.backend.js.dce.dumpDeclarationIrSizesIfNeed
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.ir.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.test.handlers.getWasmTestOutputDirectory
import org.jetbrains.kotlin.wasm.test.tools.WasmOptimizer
import java.io.File

class WasmBackendFacade(
    private val testServices: TestServices
) : AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Wasm>() {
    private val supportedOptimizer: WasmOptimizer = WasmOptimizer.Binaryen

    override val inputKind = ArtifactKinds.KLib
    override val outputKind = ArtifactKinds.Wasm

    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): BinaryArtifacts.Wasm? {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val generateSourceMaps = WasmEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP in testServices.moduleStructure.allDirectives
        val generateDts = WasmEnvironmentConfigurationDirectives.CHECK_TYPESCRIPT_DECLARATIONS in testServices.moduleStructure.allDirectives

        // Enforce PL with the ERROR log level to fail any tests where PL detected any incompatibilities.
        configuration.setupPartialLinkageConfig(PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.ERROR))

        val isMainModule = WasmEnvironmentConfigurator.isMainModule(module, testServices)
        if (!isMainModule) return null

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

        val suffix = when (configuration.get(WasmConfigurationKeys.WASM_TARGET, WasmTarget.JS)) {
            WasmTarget.JS -> "-js"
            WasmTarget.WASI -> "-wasi"
            else -> error("Unexpected wasi target")
        }

        val libraries = listOf(
            System.getProperty("kotlin.wasm$suffix.stdlib.path")!!,
            System.getProperty("kotlin.wasm$suffix.kotlin.test.path")!!
        ) + WasmEnvironmentConfigurator.getAllRecursiveLibrariesFor(module, testServices).map { it.key.libraryFile.canonicalPath }

        val friendLibraries = emptyList<String>()
        val mainModule = MainModule.Klib(inputArtifact.outputFile.absolutePath)
        val project = testServices.compilerConfigurationProvider.getProject(module)
        val moduleStructure = ModulesStructure(
            project,
            mainModule,
            configuration,
            libraries + mainModule.libPath,
            friendLibraries
        )

        val irFactory = IrFactoryImplForWasmIC(WholeWorldStageController())

        val testPackage = extractTestPackage(testServices)
        val (allModules, backendContext, typeScriptFragment) = compileToLoweredIr(
            depsDescriptors = moduleStructure,
            phaseConfig = phaseConfig,
            irFactory = irFactory,
            exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, "box"))),
            propertyLazyInitialization = true,
            generateTypeScriptFragment = generateDts
        )
        val generateWat = debugMode >= DebugMode.DEBUG || configuration.getBoolean(WasmConfigurationKeys.WASM_GENERATE_WAT)
        val baseFileName = "index"

        val compilerResult = compileWasm(
            allModules = allModules,
            backendContext = backendContext,
            typeScriptFragment = typeScriptFragment,
            baseFileName = baseFileName,
            idSignatureRetriever = irFactory,
            emitNameSection = true,
            allowIncompleteImplementations = false,
            generateWat = generateWat,
            generateSourceMaps = generateSourceMaps,
        )

        val dceDumpNameCache = DceDumpNameCache()
        eliminateDeadDeclarations(allModules, backendContext, dceDumpNameCache)

        dumpDeclarationIrSizesIfNeed(System.getProperty("kotlin.wasm.dump.declaration.ir.size.to.file"), allModules, dceDumpNameCache)

        val compilerResultWithDCE = compileWasm(
            allModules = allModules,
            backendContext = backendContext,
            typeScriptFragment = typeScriptFragment,
            baseFileName = baseFileName,
            idSignatureRetriever = irFactory,
            emitNameSection = true,
            allowIncompleteImplementations = true,
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

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return WasmEnvironmentConfigurator.isMainModule(module, testServices)
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
