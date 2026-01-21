/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.backend.wasm.DebuggerCompileOptions
import org.jetbrains.kotlin.backend.wasm.WasmIrModuleConfiguration
import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.compileToLoweredIr
import org.jetbrains.kotlin.backend.wasm.linkAndCompileWasmIrToBinary
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.backend.wasm.ir2wasm.ExceptionTagType
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledFileFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleFragmentGenerator
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleMetadataCache
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaseSet
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.dce.DceDumpNameCache
import org.jetbrains.kotlin.ir.backend.js.dce.dumpDeclarationIrSizesIfNeed
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.ir.WasmModule
import org.jetbrains.kotlin.wasm.test.handlers.getWasmTestOutputDirectory
import org.jetbrains.kotlin.wasm.test.tools.WasmOptimizer
import java.io.File

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
        val phaseConfig = if (debugMode >= DebugMode.SUPER_DEBUG) {
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
        configuration.phaseConfig = phaseConfig

        val testPackage = extractTestPackage(testServices)
        val performanceManager = configuration.perfManager
        performanceManager?.let {
            it.notifyPhaseFinished(PhaseType.Initialization)
            it.notifyPhaseStarted(PhaseType.TranslationToIr)
        }
        val generateDwarf = WasmEnvironmentConfigurationDirectives.GENERATE_DWARF in testServices.moduleStructure.allDirectives
        val generateSourceMaps = WasmEnvironmentConfigurationDirectives.GENERATE_SOURCE_MAP in testServices.moduleStructure.allDirectives
        val generateDts = WasmEnvironmentConfigurationDirectives.CHECK_TYPESCRIPT_DECLARATIONS in testServices.moduleStructure.allDirectives
        val useDebuggerCustomFormatters = debugMode >= DebugMode.DEBUG || configuration.getBoolean(JSConfigurationKeys.USE_DEBUGGER_CUSTOM_FORMATTERS)
        configuration.put(WasmConfigurationKeys.WASM_DISABLE_CROSS_FILE_OPTIMISATIONS, false)
        val (allModules, backendContext, typeScriptFragment) = compileToLoweredIr(
            moduleInfo,
            mainModule,
            configuration,
            performanceManager,
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
            skipCommentInstructions = !generateWat,
            skipLocations = !generateDwarf && !generateSourceMaps,
        )
        val wasmCompiledFileFragments = allModules.map { codeGenerator.generateModuleAsSingleFileFragment(it) }

        val debuggerOptions = DebuggerCompileOptions(
            emitNameSection = true,
            generateSourceMaps = generateSourceMaps,
            generateDwarf = generateDwarf,
            useDebuggerCustomFormatters = useDebuggerCustomFormatters,
        )

        val parameters = WasmIrModuleConfiguration(
            wasmCompiledFileFragments = wasmCompiledFileFragments,
            moduleName = allModules.last().descriptor.name.asString(),
            configuration = configuration,
            typeScriptFragment = typeScriptFragment,
            baseFileName = baseFileName,
            generateWat = generateWat,
            debuggerOptions = debuggerOptions,
            multimoduleOptions = null,
        )

        val compilerResult = linkAndCompileWasmIrToBinary(parameters)

        val linkedModule = getLinkedModule(
            parameters,
            wasmCompiledFileFragments,
            configuration
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
            skipCommentInstructions = !generateWat,
            skipLocations = !generateDwarf && !generateSourceMaps,
        )
        val wasmCompiledFileFragmentsDce = allModules.map { codeGeneratorDce.generateModuleAsSingleFileFragment(it) }

        val dceParameters = WasmIrModuleConfiguration(
            wasmCompiledFileFragments = wasmCompiledFileFragmentsDce,
            moduleName = allModules.last().descriptor.name.asString(),
            configuration = configuration,
            typeScriptFragment = typeScriptFragment,
            baseFileName = baseFileName,
            generateWat = generateWat,
            debuggerOptions = debuggerOptions,
            multimoduleOptions = null,
        )

        val compilerResultWithDCE = linkAndCompileWasmIrToBinary(dceParameters)

        return BinaryArtifacts.Wasm(
            linkedModule,
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

internal fun getLinkedModule(moduleConfiguration: WasmIrModuleConfiguration, wasmCompiledFileFragments: List<WasmCompiledFileFragment>, configuration: CompilerConfiguration): WasmModule {
    val isWasmJsTarget = configuration.get(WasmConfigurationKeys.WASM_TARGET) != WasmTarget.WASI

    val wasmCompiledModuleFragment = WasmCompiledModuleFragment(
        wasmCompiledFileFragments,
        isWasmJsTarget,
    )

    val multimoduleParameters = moduleConfiguration.multimoduleOptions

    val exceptionTagType: ExceptionTagType = when {
        configuration.getBoolean(WasmConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS) ->
            ExceptionTagType.TRAP
        isWasmJsTarget -> ExceptionTagType.JS_TAG
        else -> ExceptionTagType.WASM_TAG
    }

    val wasmCommandModuleInitialization = configuration.get(WasmConfigurationKeys.WASM_COMMAND_MODULE) ?: false

    return wasmCompiledModuleFragment.linkWasmCompiledFragments(
        multimoduleOptions = multimoduleParameters,
        exceptionTagType = exceptionTagType,
        wasmCommandModuleInitialization = wasmCommandModuleInitialization
    )
}