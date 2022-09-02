/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.phases

import kotlinx.cinterop.usingJvmCInteropCallbacks
import llvm.LLVMContextCreate
import llvm.LLVMCreateDIBuilder
import llvm.LLVMModuleCreateWithNameInContext
import org.jetbrains.kotlin.backend.common.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.llvm.DebugInfo
import org.jetbrains.kotlin.backend.konan.llvm.Llvm
import org.jetbrains.kotlin.backend.konan.llvm.llvmContext
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.util.usingNativeMemoryAllocator

/**
 * Compiler driver that constructs its pipeline based on its inputs.
 */
class DynamicNativeCompilerDriver(
        private val config: KonanConfig,
        private val arguments: K2NativeCompilerArguments,
) {
    private val messageCollector = config.configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            ?: MessageCollector.NONE

    // It should be a proper (Input, Context, Phases) -> Output pure function, but NamedCompilerPhase is a bit tricky to refactor.
    private fun <Context : PhaseContext, Data> runTopLevelPhase(context: Context, phase: NamedCompilerPhase<Context, Data>, input: Data) =
            phase.invokeToplevel(createPhaseConfig(phase, arguments, messageCollector), context, input)

    private fun <Context : PhaseContext> runTopLevelPhaseUnit(context: Context, phase: NamedCompilerPhase<Context, Unit>) =
            runTopLevelPhase(context, phase, Unit)

    companion object {
        fun canRun(config: KonanConfig): Boolean {
            val kind = config.produce
            if (kind in setOf(CompilerOutputKind.LIBRARY, CompilerOutputKind.STATIC_CACHE, CompilerOutputKind.FRAMEWORK)) {
                return true
            }
            return false
        }
    }

    fun build(config: KonanConfig, environment: KotlinCoreEnvironment, kind: CompilerOutputKind) {
        usingNativeMemoryAllocator {
            usingJvmCInteropCallbacks {
                try {
                    when (kind) {
                        CompilerOutputKind.LIBRARY -> buildKlib(config, environment)
                        CompilerOutputKind.FRAMEWORK -> buildFramework(config, environment)
                        CompilerOutputKind.STATIC_CACHE -> {
                            if (!config.producePerFileCache) {
                                buildStaticCache(config, environment)
                            } else {
                                buildPerFileStaticCache(config, environment)
                            }
                        }
                        else -> error("Unsupported kind $kind")
                    }
                } finally {
                }
            }
        }
    }

    private fun buildKlib(config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendPhase = Phases.buildFrontendPhase()

        val createSymbolTablePhase = Phases.buildCreateSymbolTablePhase()
        val psiToIrPhase = Phases.buildTranslatePsiToIrPhase(isProducingLibrary = true, createSymbolTablePhase)
        val destroySymbolTablePhase = Phases.buildDestroySymbolTablePhase(createSymbolTablePhase)
        val serializerPhase = Phases.buildSerializerPhase()
        val produceKlibPhase = Phases.buildProduceKlibPhase()

        val irGen = NamedCompilerPhase(
                "IRGen",
                "IR generation",
                nlevels = 1,
                lower = createSymbolTablePhase then
                        psiToIrPhase then
                        destroySymbolTablePhase
        )

        val generateKlib = NamedCompilerPhase(
                "GenerateKlib",
                "Library serialization",
                nlevels = 1,
                lower = serializerPhase then
                        produceKlibPhase
        )

        val context = Context(config)
        context.environment = environment
        if (!runTopLevelPhase(context, frontendPhase, true)) {
            return
        }
        // TODO: Create new specialized context instead
        val psiToIrContext: PsiToIrContext = context
        runTopLevelPhaseUnit(psiToIrContext, irGen)

        val klibProducingContext: KlibProducingContext = context
        runTopLevelPhaseUnit(klibProducingContext, generateKlib)

        val time = frontendPhase.time + irGen.time + generateKlib.time
        println("It took ${time}")
    }

    private fun buildFramework(config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendPhase = Phases.buildFrontendPhase()
        val frontendContext: FrontendContext = FrontendContextImpl(config, environment)

        if (!runTopLevelPhase(frontendContext, frontendPhase, true)) {
            return
        }

        val objCExportPhase = Phases.buildObjCExportPhase()
        val objCExport = runTopLevelPhase(frontendContext, objCExportPhase, null)

        val context = Context(config, objCExport)
        context.populateFromFrontend(frontendContext)

        if (config.omitFrameworkBinary) {
            val produceFrameworkInterface = Phases.buildProduceFrameworkInterfacePhase()
            val objCExportContext: ObjCExportContext = context
            runTopLevelPhaseUnit(objCExportContext, produceFrameworkInterface)
            // So the only things we need is
            // 1. Parse sources
            // 2. Generate headers
            // 3. Write them to frameworks directory.
            return
        }

        val createSymbolTablePhase = Phases.buildCreateSymbolTablePhase()
        val objCExportCodeSpecPhase = Phases.buildObjCCodeSpecPhase(createSymbolTablePhase)
        val psiToIrPhase = Phases.buildTranslatePsiToIrPhase(isProducingLibrary = false, createSymbolTablePhase)
        val destroySymbolTablePhase = Phases.buildDestroySymbolTablePhase(createSymbolTablePhase)

        val irGen = NamedCompilerPhase(
                "IRGen",
                "IR generation",
                nlevels = 1,
                lower = createSymbolTablePhase then
                        objCExportCodeSpecPhase then
                        psiToIrPhase then
                        destroySymbolTablePhase
        )
        val objCExportContext: ObjCExportContext = context
        runTopLevelPhaseUnit(objCExportContext, irGen)

        val specialBackendChecksPhase = Phases.buildSpecialBackendChecksPhase()
        val copyDefaultValuesToActualPhase = Phases.buildCopyDefaultValuesToActualPhase()
        val buildFunctionsWithoutBoundCheck = Phases.buildFunctionsWithoutBoundCheck()
        val irProcessing = NamedCompilerPhase<MiddleEndContext, Unit>(
                "IRProcessing",
                "Process linked IR",
                nlevels = 1,
                lower = copyDefaultValuesToActualPhase then
                        specialBackendChecksPhase then
                        buildFunctionsWithoutBoundCheck
        )
        val middleEndContext: MiddleEndContext = context
        runTopLevelPhaseUnit(middleEndContext, irProcessing)
        val allLowerings = Phases.buildAllLoweringsPhase()
        runTopLevelPhase(middleEndContext, allLowerings, middleEndContext.irModule!!)
        dependenciesLowering(middleEndContext.irModule!!, middleEndContext)

        val payload = BasicPhaseContextPayload(
                context.config,
                context.irBuiltIns,
                context.typeSystem,
                context.builtIns,
                context.ir,
                librariesWithDependencies = context.librariesWithDependencies,
        )

        val ltoPhases = Phases.buildLtoAndMiscPhases(config)
        val ltoContext: LtoContext = LtoContextImpl(
                payload,
                context.bridgesSupport,
                context.irModule!!,
                context.classIdComputerHolder,
                context.classITablePlacer,
                context.classVTableEntries,
                context.layoutBuildersHolder,
        )
        runTopLevelPhase(ltoContext, ltoPhases, middleEndContext.irModule!!)

        val smartHoldersCollection = SmartHoldersCollection(
                context.classIdComputerHolder,
                context.classITablePlacer,
                context.classVTableEntries,
                context.classFieldsLayoutHolder,
                context.layoutBuildersHolder
        )

        val ltoResult = LtoResults(
                ltoContext.globalHierarchyAnalysisResult,
                ltoContext.ghaEnabled(),
                ltoContext.devirtualizationAnalysisResult,
                ltoContext.moduleDFG,
                ltoContext.referencedFunctions,
                ltoContext.lifetimes
        )

        val cacheContext = CacheContextImpl(config, context)

        // TODO: Move into phase
        // TODO: Dispose
        llvmContext = LLVMContextCreate()!!
        val llvmModule = LLVMModuleCreateWithNameInContext("out", llvmContext)!!
        val llvm = Llvm(middleEndContext, middleEndContext.llvmModuleSpecification, config, llvmModule)
        val debugInfo = DebugInfo(middleEndContext, llvm, llvm.runtime.targetData)
        debugInfo.builder = LLVMCreateDIBuilder(llvmModule)

        val bitcodegenPhase = Phases.buildBitcodePhases(needSetup = false)
        val bitcodegenContext: BitcodegenContext = BitcodegenContextImpl(
                payload,
                context.llvmModuleSpecification,
                llvmModule,
                middleEndContext.irModule!!,
                middleEndContext.irLinker,
                context.coverage,
                llvm,
                smartHoldersCollection,
                ltoResult,
                cAdapterGenerator = null,
                objCExport,
                debugInfo,
                middleEndContext.localClassNames,
                middleEndContext.bridgesSupport,
                middleEndContext.enumsSupport,
                cacheContext
        )

        runTopLevelPhase(bitcodegenContext, bitcodegenPhase, bitcodegenContext.irModule!!)

        val llvmCodegenPhase = Phases.buildLlvmCodegenPhase()
        val llvmcodegenContext: LlvmCodegenContext = LlvmCodegenContextImpl(
                payload,
                context.cStubsManager,
                context.objCExportNamer,
                bitcodegenContext.llvm,
                bitcodegenContext.coverage,
                bitcodegenContext.llvmModule,
                bitcodegenContext.llvmModuleSpecification
        )
        runTopLevelPhase(llvmcodegenContext, llvmCodegenPhase, bitcodegenContext.irModule!!)

        val writeLlvmModulePhase = Phases.buildWriteLlvmModule()
        runTopLevelPhaseUnit(llvmcodegenContext, writeLlvmModulePhase)

        val objectFilesContext = ObjectFilesContextImpl(config)
        val objectFilesPhase = Phases.buildObjectFilesPhase(llvmcodegenContext.bitcodeFileName)
        runTopLevelPhaseUnit(objectFilesContext, objectFilesPhase)

        val linkerContext = LinkerContextImpl(
                config,
                bitcodegenContext.necessaryLlvmParts,
                bitcodegenContext.coverage,
                bitcodegenContext.llvmModuleSpecification
        )
        val linkerPhase = Phases.buildLinkerPhase(objectFilesContext.compilerOutput)
        runTopLevelPhaseUnit(linkerContext, linkerPhase)
    }

    private fun buildPerFileStaticCache(config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendPhase = Phases.buildFrontendPhase()
        val frontendContext: FrontendContext = FrontendContextImpl(config, environment)
        if (!runTopLevelPhase(frontendContext, frontendPhase, true)) {
            return
        }

        val objCExportPhase = Phases.buildObjCExportPhase()
        val objCExport = runTopLevelPhase(frontendContext, objCExportPhase, null)

        val createSymbolTablePhase = Phases.buildCreateSymbolTablePhase()
        val psiToIrPhase = Phases.buildTranslatePsiToIrPhase(isProducingLibrary = false, createSymbolTablePhase)
        val destroySymbolTablePhase = Phases.buildDestroySymbolTablePhase(createSymbolTablePhase)

        val objcExportCodeSpecPhase = Phases.buildObjCCodeSpecPhase(createSymbolTablePhase)

        val buildAdditionalCacheInfoPhase = Phases.buildBuildAdditionalCacheInfoPhase(psiToIrPhase)
        val irGen = NamedCompilerPhase(
                "IRGen",
                "IR generation",
                nlevels = 1,
                lower = createSymbolTablePhase then
                        objcExportCodeSpecPhase then
                        psiToIrPhase then
                        destroySymbolTablePhase
        )
        val context = Context(config, objCExport)
        context.populateFromFrontend(frontendContext)
        val objcExportContext: ObjCExportContext = context
        runTopLevelPhaseUnit(objcExportContext, irGen)

        val specialBackendChecksPhase = Phases.buildSpecialBackendChecksPhase()
        val copyDefaultValuesToActualPhase = Phases.buildCopyDefaultValuesToActualPhase()
        val buildFunctionsWithoutBoundCheck = Phases.buildFunctionsWithoutBoundCheck()
        val irProcessing = NamedCompilerPhase(
                "IRProcessing",
                "Process linked IR",
                nlevels = 1,
                lower = buildAdditionalCacheInfoPhase then
                        copyDefaultValuesToActualPhase then
                        specialBackendChecksPhase then
                        buildFunctionsWithoutBoundCheck
        )
        val middleEndContext: MiddleEndContext = context
        runTopLevelPhaseUnit(middleEndContext, irProcessing)

        val allLowerings = Phases.buildAllLoweringsPhase()
        runTopLevelPhase(middleEndContext, allLowerings, middleEndContext.irModule!!)
        dependenciesLowering(middleEndContext.irModule!!, middleEndContext)

        val ltoPhases = Phases.buildLtoAndMiscPhases(config)
        val ltoContext: LtoContext = context
        runTopLevelPhase(ltoContext, ltoPhases, middleEndContext.irModule!!)

        val payload = BasicPhaseContextPayload(
                context.config,
                context.irBuiltIns,
                context.typeSystem,
                context.builtIns,
                context.ir,
                librariesWithDependencies = context.librariesWithDependencies,
        )
        val cacheContext = CacheContextImpl(config, context)

        val smartHoldersCollection = SmartHoldersCollection(
                context.classIdComputerHolder,
                context.classITablePlacer,
                context.classVTableEntries,
                context.classFieldsLayoutHolder,
                context.layoutBuildersHolder
        )

        // TODO: Move into phase
        // TODO: Dispose
        llvmContext = LLVMContextCreate()!!

        val objectFiles = mutableListOf<ObjectFile>()
        val allCachedBitcodeDependencies = mutableSetOf<KonanLibrary>()
        val nativeDependenciesToLink = mutableSetOf<KonanLibrary>()
        val allNativeDependencies = mutableSetOf<KonanLibrary>()
        middleEndContext.irModule!!.files.forEachIndexed { idx, file ->
            val binaryName = "${idx}.kt.bin"
            println("Compiling ${file.fileEntry.name} as $binaryName")
            val llvmModule = LLVMModuleCreateWithNameInContext(binaryName, llvmContext)!!
            val spec = FileCacheLlvmModuleSpecification(context, config.cachedLibraries, config.libraryToCache!!)
            val llvm = Llvm(middleEndContext, spec, config, llvmModule)
            val debugInfo = DebugInfo(middleEndContext, llvm, llvm.runtime.targetData)
            debugInfo.builder = LLVMCreateDIBuilder(llvmModule)
            context.config.libraryToCache!!.strategy = CacheDeserializationStrategy.SingleFile(file.path, file.fqName.asString())

            val bitcodegenPhase = Phases.buildBitcodePhases(needSetup = false)
            val bitcodegenContext: BitcodegenContext = BitcodegenContextImpl(
                    payload,
                    spec,
                    llvmModule,
                    middleEndContext.irModule!!,
                    middleEndContext.irLinker,
                    context.coverage,
                    llvm,
                    smartHoldersCollection,
                    LtoResults.EMPTY,
                    cAdapterGenerator = null,
                    objCExport,
                    debugInfo,
                    middleEndContext.localClassNames,
                    middleEndContext.bridgesSupport,
                    middleEndContext.enumsSupport,
                    cacheContext
            )

            runTopLevelPhase(bitcodegenContext, bitcodegenPhase, bitcodegenContext.irModule!!)

            val llvmCodegenPhase = Phases.buildLlvmCodegenPhase()

            val llvmcodegenContext: LlvmCodegenContext = LlvmCodegenContextImpl(
                    payload,
                    context.cStubsManager,
                    context.objCExportNamer,
                    bitcodegenContext.llvm,
                    bitcodegenContext.coverage,
                    bitcodegenContext.llvmModule,
                    bitcodegenContext.llvmModuleSpecification
            )
            runTopLevelPhase(llvmcodegenContext, llvmCodegenPhase, bitcodegenContext.irModule!!)

            val writeLlvmModulePhase = Phases.buildWriteLlvmModule(binaryName)
            runTopLevelPhaseUnit(llvmcodegenContext, writeLlvmModulePhase)

            val saveAdditionalCacheInfoPhase = Phases.buildSaveAdditionalCacheInfoPhase()
            val cacheAwareContext: CacheAwareContext = context
            runTopLevelPhaseUnit(cacheAwareContext, saveAdditionalCacheInfoPhase)

            val objectFilesContext: ObjectFilesContext = ObjectFilesContextImpl(config)
            val objectFilesPhase = Phases.buildObjectFilesPhase(llvmcodegenContext.bitcodeFileName)
            runTopLevelPhaseUnit(objectFilesContext, objectFilesPhase)
            objectFiles += objectFilesContext.compilerOutput
            allCachedBitcodeDependencies += bitcodegenContext.necessaryLlvmParts.allCachedBitcodeDependencies
            nativeDependenciesToLink += bitcodegenContext.necessaryLlvmParts.nativeDependenciesToLink
            allNativeDependencies += bitcodegenContext.necessaryLlvmParts.allNativeDependencies
        }

        val necessaryLlvmParts = NecessaryLlvmParts(
                allCachedBitcodeDependencies,
                nativeDependenciesToLink,
                allNativeDependencies
        )

        val linkerContext = LinkerContextImpl(
                config,
                necessaryLlvmParts,
                context.coverage,
                context.llvmModuleSpecification
        )
        val linkerPhase = Phases.buildLinkerPhase(objectFiles)
        runTopLevelPhaseUnit(linkerContext, linkerPhase)

        val finalizeCachePhase = Phases.buildFinalizeCachePhase()
        runTopLevelPhaseUnit(cacheContext, finalizeCachePhase)
    }

    private fun buildStaticCache(config: KonanConfig, environment: KotlinCoreEnvironment) {
        val frontendPhase = Phases.buildFrontendPhase()
        val frontendContext: FrontendContext = FrontendContextImpl(config, environment)
        if (!runTopLevelPhase(frontendContext, frontendPhase, true)) {
            return
        }

        val objCExportPhase = Phases.buildObjCExportPhase()
        val objCExport = runTopLevelPhase(frontendContext, objCExportPhase, null)

        val createSymbolTablePhase = Phases.buildCreateSymbolTablePhase()
        val psiToIrPhase = Phases.buildTranslatePsiToIrPhase(isProducingLibrary = false, createSymbolTablePhase)
        val destroySymbolTablePhase = Phases.buildDestroySymbolTablePhase(createSymbolTablePhase)

        val objcExportCodeSpecPhase = Phases.buildObjCCodeSpecPhase(createSymbolTablePhase)

        val buildAdditionalCacheInfoPhase = Phases.buildBuildAdditionalCacheInfoPhase(psiToIrPhase)
        val irGen = NamedCompilerPhase(
                "IRGen",
                "IR generation",
                nlevels = 1,
                lower = createSymbolTablePhase then
                        objcExportCodeSpecPhase then
                        psiToIrPhase then
                        destroySymbolTablePhase
        )
        val context = Context(config, objCExport)
        context.populateFromFrontend(frontendContext)
        val objcExportContext: ObjCExportContext = context
        runTopLevelPhaseUnit(objcExportContext, irGen)

        val specialBackendChecksPhase = Phases.buildSpecialBackendChecksPhase()
        val copyDefaultValuesToActualPhase = Phases.buildCopyDefaultValuesToActualPhase()
        val buildFunctionsWithoutBoundCheck = Phases.buildFunctionsWithoutBoundCheck()
        val irProcessing = NamedCompilerPhase(
                "IRProcessing",
                "Process linked IR",
                nlevels = 1,
                lower = buildAdditionalCacheInfoPhase then
                        copyDefaultValuesToActualPhase then
                        specialBackendChecksPhase then
                        buildFunctionsWithoutBoundCheck
        )
        val middleEndContext: MiddleEndContext = context
        runTopLevelPhaseUnit(middleEndContext, irProcessing)

        val allLowerings = Phases.buildAllLoweringsPhase()
        runTopLevelPhase(middleEndContext, allLowerings, middleEndContext.irModule!!)
        dependenciesLowering(middleEndContext.irModule!!, middleEndContext)

        val ltoPhases = Phases.buildLtoAndMiscPhases(config)
        val ltoContext: LtoContext = context
        runTopLevelPhase(ltoContext, ltoPhases, middleEndContext.irModule!!)

        val payload = BasicPhaseContextPayload(
                context.config,
                context.irBuiltIns,
                context.typeSystem,
                context.builtIns,
                context.ir,
                librariesWithDependencies = context.librariesWithDependencies,
        )
        val cacheContext = CacheContextImpl(config, context)

        val smartHoldersCollection = SmartHoldersCollection(
                context.classIdComputerHolder,
                context.classITablePlacer,
                context.classVTableEntries,
                context.classFieldsLayoutHolder,
                context.layoutBuildersHolder
        )

        // TODO: Move into phase
        // TODO: Dispose
        llvmContext = LLVMContextCreate()!!
        val llvmModule = LLVMModuleCreateWithNameInContext("out", llvmContext)!!
        val llvm = Llvm(middleEndContext, middleEndContext.llvmModuleSpecification, config, llvmModule)
        val debugInfo = DebugInfo(middleEndContext, llvm, llvm.runtime.targetData)
        debugInfo.builder = LLVMCreateDIBuilder(llvmModule)

        val bitcodegenPhase = Phases.buildBitcodePhases(needSetup = false)
        val bitcodegenContext: BitcodegenContext = BitcodegenContextImpl(
                payload,
                context.llvmModuleSpecification,
                llvmModule,
                middleEndContext.irModule!!,
                middleEndContext.irLinker,
                context.coverage,
                llvm,
                smartHoldersCollection,
                LtoResults.EMPTY,
                cAdapterGenerator = null,
                objCExport,
                debugInfo,
                middleEndContext.localClassNames,
                middleEndContext.bridgesSupport,
                middleEndContext.enumsSupport,
                cacheContext
        )

        runTopLevelPhase(bitcodegenContext, bitcodegenPhase, bitcodegenContext.irModule!!)

        val llvmCodegenPhase = Phases.buildLlvmCodegenPhase()

        val llvmcodegenContext: LlvmCodegenContext = LlvmCodegenContextImpl(
                payload,
                context.cStubsManager,
                context.objCExportNamer,
                bitcodegenContext.llvm,
                bitcodegenContext.coverage,
                bitcodegenContext.llvmModule,
                bitcodegenContext.llvmModuleSpecification
        )
        runTopLevelPhase(llvmcodegenContext, llvmCodegenPhase, bitcodegenContext.irModule!!)

        val writeLlvmModulePhase = Phases.buildWriteLlvmModule()
        runTopLevelPhaseUnit(llvmcodegenContext, writeLlvmModulePhase)

        val saveAdditionalCacheInfoPhase = Phases.buildSaveAdditionalCacheInfoPhase()
        val cacheAwareContext: CacheAwareContext = context
        runTopLevelPhaseUnit(cacheAwareContext, saveAdditionalCacheInfoPhase)

        val objectFilesContext: ObjectFilesContext = ObjectFilesContextImpl(config)
        val objectFilesPhase = Phases.buildObjectFilesPhase(llvmcodegenContext.bitcodeFileName)
        runTopLevelPhaseUnit(objectFilesContext, objectFilesPhase)

        val linkerContext = LinkerContextImpl(
                config,
                bitcodegenContext.necessaryLlvmParts,
                bitcodegenContext.coverage,
                bitcodegenContext.llvmModuleSpecification
        )
        val linkerPhase = Phases.buildLinkerPhase(objectFilesContext.compilerOutput)
        runTopLevelPhaseUnit(linkerContext, linkerPhase)

        val finalizeCachePhase = Phases.buildFinalizeCachePhase()
        runTopLevelPhaseUnit(cacheContext, finalizeCachePhase)
    }

    private fun dependenciesLowering(input: IrModuleFragment, context: MiddleEndContext) {
        val files = mutableListOf<IrFile>()
        files += input.files
        input.files.clear()

        // TODO: KonanLibraryResolver.TopologicalLibraryOrder actually returns libraries in the reverse topological order.
        context.config.librariesWithDependencies(context.moduleDescriptor)
                .reversed()
                .forEach {
                    val libModule = context.irModules[it.libraryName]
                            ?: return@forEach

                    input.files += libModule.files
                    val lowerings = Phases.buildAllLoweringsPhase()
                    runTopLevelPhase(context, lowerings, input)

                    input.files.clear()
                }

        // Save all files for codegen in reverse topological order.
        // This guarantees that libraries initializers are emitted in correct order.
        context.config.librariesWithDependencies(context.moduleDescriptor)
                .forEach {
                    val libModule = context.irModules[it.libraryName]
                            ?: return@forEach
                    input.files += libModule.files
                }

        input.files += files
    }
}