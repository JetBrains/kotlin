/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.WasmIrModuleConfiguration
import org.jetbrains.kotlin.backend.wasm.compileWasmIrToBinary
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.backend.wasm.linkWasmIr
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WholeWorldCompiler
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WholeWorldMultiModuleCompiler
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaseSet
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.js.config.dce
import org.jetbrains.kotlin.js.config.generateDts
import org.jetbrains.kotlin.js.config.outputDir
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
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.wasmDebug
import org.jetbrains.kotlin.wasm.config.wasmForceDebugFriendlyCompilation
import org.jetbrains.kotlin.wasm.config.wasmGenerateClosedWorldMultimodule
import org.jetbrains.kotlin.wasm.config.wasmGenerateDwarf
import org.jetbrains.kotlin.wasm.config.wasmGenerateWat
import org.jetbrains.kotlin.wasm.config.wasmIncludedModuleOnly
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
        val outputDirBase = testServices.getWasmTestOutputDirectory()
        val phaseConfigToConfigure = if (debugMode >= DebugMode.SUPER_DEBUG) {
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
            outputDir = outputDirBase
            configureWith(testServices.moduleStructure.allDirectives)
        }

        val testPackage = extractTestPackage(testServices)
        val exportedBoxDeclaration = setOf(FqName.fromSegments(listOfNotNull(testPackage, "box")))

        configuration.perfManager?.notifyPhaseFinished(PhaseType.Initialization)

        val irFactory = moduleInfo.symbolTable.irFactory as IrFactoryImplForWasmIC

        val compiler = if (configuration.wasmGenerateClosedWorldMultimodule) {
            WholeWorldMultiModuleCompiler(configuration, irFactory)
        } else {
            WholeWorldCompiler(configuration, irFactory)
        }

        val loweredIr = configuration.perfManager.tryMeasurePhaseTime(PhaseType.IrLowering) {
            compiler.lowerIr(moduleInfo, mainModule, exportedBoxDeclaration)
        }

        val parameters = configuration.perfManager.tryMeasurePhaseTime(PhaseType.Backend) {
            configuration.dce = false
            compiler.compileIr(loweredIr)
        }
        val compilationSet = makeCompilationSet(parameters)

        configuration.dce = true
        val dceParameters = compiler.compileIr(loweredIr)
        val dceCompilationSet = makeCompilationSet(dceParameters)

        val runOptimiser = WasmEnvironmentConfigurationDirectives.RUN_THIRD_PARTY_OPTIMIZER in testServices.moduleStructure.allDirectives
        val optimised = runIf(runOptimiser) {
            val multiModuleOptimization = configuration.wasmGenerateClosedWorldMultimodule
            val optimisedResult = dceCompilationSet.compilerResult.runThirdPartyOptimizer(multiModule = multiModuleOptimization)
            val optimisedDependencies = dceCompilationSet.compilationDependencies.map {
                BinaryArtifacts.WasmCompilationSet(
                    compiledModule = it.compiledModule,
                    compilerResult = it.compilerResult.runThirdPartyOptimizer(multiModule = multiModuleOptimization)
                )
            }
            BinaryArtifacts.WasmCompilationSet(
                compiledModule = dceCompilationSet.compiledModule,
                compilerResult = optimisedResult,
                compilationDependencies = optimisedDependencies
            )
        }

        return BinaryArtifacts.Wasm.CompilationSets(
            compilation = compilationSet,
            dceCompilation = dceCompilationSet,
            optimisedCompilation = optimised,
        )
    }

    fun makeCompilationSet(parameters: List<WasmIrModuleConfiguration>): BinaryArtifacts.WasmCompilationSet {
        val compilationSets = parameters.map { current ->
            val linkedModule = linkWasmIr(current)
            val compilerResult = compileWasmIrToBinary(current, linkedModule)
            BinaryArtifacts.WasmCompilationSet(linkedModule, compilerResult)
        }

        val main = compilationSets.last()
        val dependencies = compilationSets.dropLast(1)

        return BinaryArtifacts.WasmCompilationSet(
            main.compiledModule,
            main.compilerResult,
            dependencies
        )
    }

    private fun WasmCompilerResult.runThirdPartyOptimizer(multiModule: Boolean): WasmCompilerResult {
        val (newWasm, newWat) = supportedOptimizer.run(wasm, withText = wat != null, multiModule = multiModule)
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