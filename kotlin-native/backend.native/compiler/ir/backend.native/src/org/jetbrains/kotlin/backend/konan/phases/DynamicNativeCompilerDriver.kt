/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.phases

import kotlinx.cinterop.usingJvmCInteropCallbacks
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.konan.file.File
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
                        CompilerOutputKind.STATIC_CACHE -> buildStaticCache(config, environment)
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
        val createSymbolTablePhase = Phases.buildCreateSymbolTablePhase()
        val objCExportPhase = Phases.buildObjCExportPhase(createSymbolTablePhase)
        val psiToIrPhase = Phases.buildTranslatePsiToIrPhase(isProducingLibrary = false, createSymbolTablePhase)
        val destroySymbolTablePhase = Phases.buildDestroySymbolTablePhase(createSymbolTablePhase)
        val frontendContext: FrontendContext = FrontendContextImpl(config, environment)

        if (!runTopLevelPhase(frontendContext, frontendPhase, true)) {
            return
        }

        val context = Context(config)
        context.populateFromFrontend(frontendContext)

        if (config.omitFrameworkBinary) {
            val produceFrameworkInterface = Phases.buildProduceFrameworkInterfacePhase()
            val produceFramework = NamedCompilerPhase(
                    "ProduceFramework",
                    "Create Apple Framework",
                    nlevels = 1,
                    lower = createSymbolTablePhase then
                            objCExportPhase then
                            destroySymbolTablePhase then
                            produceFrameworkInterface
            )
            val objCExportContext: ObjCExportContext = context
            runTopLevelPhaseUnit(objCExportContext, produceFramework)
            return
        }
        val irGen = NamedCompilerPhase(
                "IRGen",
                "IR generation",
                nlevels = 1,
                lower = createSymbolTablePhase then
                        objCExportPhase then
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

        val bitcodegenPhase = buildBitcodePhases(config)
        val bitcodegenContext: BitcodegenContext = context
        runTopLevelPhase(bitcodegenContext, bitcodegenPhase, bitcodegenContext.irModule!!)

        val payload = BasicPhaseContextPayload(context.config, context.irBuiltIns, context.typeSystem, context.builtIns, context.ir)

        val llvmCodegenPhase = NamedCompilerPhase<LlvmCodegenContext, IrModuleFragment>(
                name = "LlvmCodegen",
                description = "Generation of bitcode file",
                nlevels = 1,
                lower = verifyBitcodePhase then
                        printBitcodePhase then
                        linkBitcodeDependenciesPhase then
                        bitcodePostprocessingPhase
        )
        val llvmcodegenContext: LlvmCodegenContext = LlvmCodegenContextImpl(
                payload,
                context.cStubsManager,
                context.objCExportNamer,
                context.llvm,
                context.coverage,
                context.llvmModule,
                context.llvmModuleSpecification
        )
        runTopLevelPhase(llvmcodegenContext, llvmCodegenPhase, bitcodegenContext.irModule!!)

        val writeLlvmModulePhase = Phases.buildWriteLlvmModule()
        runTopLevelPhaseUnit(llvmcodegenContext, writeLlvmModulePhase)

        val objectFilesContext = ObjectFilesContextImpl(config)
        val objectFilesPhase = Phases.buildObjectFilesPhase(llvmcodegenContext.bitcodeFileName)
        runTopLevelPhaseUnit(objectFilesContext, objectFilesPhase)

        val linkerContext = LinkerContextImpl(
                config,
                context.necessaryLlvmParts,
                context.coverage,
                context.llvmModuleSpecification
        )
        val linkerPhase = Phases.buildLinkerPhase(objectFilesContext.compilerOutput)
        runTopLevelPhaseUnit(linkerContext, linkerPhase)
    }

    private fun buildStaticCache(config: KonanConfig, environment: KotlinCoreEnvironment) {

        val frontendPhase = Phases.buildFrontendPhase()
        val frontendContext: FrontendContext = FrontendContextImpl(config, environment)
        if (!runTopLevelPhase(frontendContext, frontendPhase, true)) {
            return
        }

        val createSymbolTablePhase = Phases.buildCreateSymbolTablePhase()
        val psiToIrPhase = Phases.buildTranslatePsiToIrPhase(isProducingLibrary = false, createSymbolTablePhase)
        val destroySymbolTablePhase = Phases.buildDestroySymbolTablePhase(createSymbolTablePhase)
        val buildAdditionalCacheInfoPhase = Phases.buildBuildAdditionalCacheInfoPhase(psiToIrPhase)
        val objCExportPhase = Phases.buildObjCExportPhase(createSymbolTablePhase)
        val irGen = NamedCompilerPhase(
                "IRGen",
                "IR generation",
                nlevels = 1,
                lower = createSymbolTablePhase then
                        objCExportPhase then
                        psiToIrPhase then
                        destroySymbolTablePhase
        )
        val context = Context(config)
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

        val bitcodegenPhase = buildBitcodePhases(config)
        val bitcodegenContext: BitcodegenContext = context
        runTopLevelPhase(bitcodegenContext, bitcodegenPhase, bitcodegenContext.irModule!!)

        val llvmCodegenPhase = NamedCompilerPhase<LlvmCodegenContext, IrModuleFragment>(
                name = "LlvmCodegen",
                description = "Generation of bitcode file",
                nlevels = 1,
                lower = verifyBitcodePhase then
                        printBitcodePhase then
                        linkBitcodeDependenciesPhase then
                        bitcodePostprocessingPhase
        )

        val payload = BasicPhaseContextPayload(context.config, context.irBuiltIns, context.typeSystem, context.builtIns, context.ir)

        val llvmcodegenContext: LlvmCodegenContext = LlvmCodegenContextImpl(
                payload,
                context.cStubsManager,
                context.objCExportNamer,
                context.llvm,
                context.coverage,
                context.llvmModule,
                context.llvmModuleSpecification
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
                context.necessaryLlvmParts,
                context.coverage,
                context.llvmModuleSpecification
        )
        val linkerPhase = Phases.buildLinkerPhase(objectFilesContext.compilerOutput)
        runTopLevelPhaseUnit(linkerContext, linkerPhase)

        val finalizeCachePhase = Phases.buildFinalizeCachePhase()
        val cacheContext = CacheContextImpl(
                config,
                context.inlineFunctionBodies,
                context.classFields,
                context.llvmImports,
                context.constructedFromExportedInlineFunctions,
                context.calledFromExportedInlineFunctions
        )
        runTopLevelPhaseUnit(cacheContext, finalizeCachePhase)
    }

    private fun buildBitcodePhases(config: KonanConfig): NamedCompilerPhase<BitcodegenContext, IrModuleFragment> {
        // TODO: Ugly and hard to manage. Use `disable` mechanism instead.
        val dfgPhase = optionalPhase(config.optimizationsEnabled) { Phases.getBuildDFGPhase() }
        val devirtualizationAnalysisPhase = optionalPhase(config.optimizationsEnabled) { Phases.getDevirtualizationAnalysisPhase(dfgPhase) }
        val removeRedundantCallsToFileInitializersPhase = optionalPhase(config.optimizationsEnabled) { Phases.getRemoveRedundantCallsToFileInitializersPhase(devirtualizationAnalysisPhase) }
        val escapeAnalysisPhase = optionalPhase(config.optimizationsEnabled) { Phases.buildEscapeAnalysisPhase(dfgPhase, devirtualizationAnalysisPhase) }
        val ghaPhase = optionalPhase(config.optimizationsEnabled) { Phases.getGhaPhase() }
        val devirtualizationPhase = optionalPhase(config.optimizationsEnabled) { Phases.getDevirtualizationPhase(dfgPhase, devirtualizationAnalysisPhase) }
        val dcePhase = optionalPhase(config.optimizationsEnabled) { Phases.getDcePhase(devirtualizationAnalysisPhase) }
        val propertyAccessorInlinePhase = optionalPhase(config.optimizationsEnabled) { Phases.getPropertyAccessorInlinePhase() }
        val inlineClassPropertyAccessorsPhase = optionalPhase(config.optimizationsEnabled) { Phases.getInlineClassPropertyAccessorsPhase() }
        val unboxInlinePhase = optionalPhase(config.optimizationsEnabled) { Phases.getUnboxInlinePhase() }

        val bitcodegenPhase = NamedCompilerPhase<BitcodegenContext, IrModuleFragment>(
                name = "BitcodeGen",
                description = "Generation of LLVM module",
                nlevels = 1,
                lower = contextLLVMSetupPhase then
                        returnsInsertionPhase then
                        dfgPhase then
                        devirtualizationAnalysisPhase then
                        dcePhase then
                        removeRedundantCallsToFileInitializersPhase then
                        devirtualizationPhase then
                        propertyAccessorInlinePhase then // Have to run after link dependencies phase, because fields
                        // from dependencies can be changed during lowerings.
                        inlineClassPropertyAccessorsPhase then
                        redundantCoercionsCleaningPhase then
                        unboxInlinePhase then
                        createLLVMDeclarationsPhase then
                        ghaPhase then
                        RTTIPhase then
                        generateDebugInfoHeaderPhase then
                        escapeAnalysisPhase then
                        codegenPhase then
                        finalizeDebugInfoPhase
        )
        return bitcodegenPhase
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

    private fun getDumpTestsPhase(testDumpFile: File): NamedCompilerPhase<Context, IrModuleFragment> = makeCustomPhase<Context, IrModuleFragment>(
            name = "dumpTestsPhase",
            description = "Dump the list of all available tests",
            op = { context, _ ->
                if (!testDumpFile.exists)
                    testDumpFile.createNew()

                if (context.testCasesToDump.isEmpty())
                    return@makeCustomPhase

                testDumpFile.appendLines(
                        context.testCasesToDump
                                .flatMap { (suiteClassId, functionNames) ->
                                    val suiteName = suiteClassId.asString()
                                    functionNames.asSequence().map { "$suiteName:$it" }
                                }
                )
            }
    )

    private fun ltoPhases() = NamedCompilerPhase(
            name = "IR_LTO",
            description = "LTO over Kotlin IR",
            lower = buildDFGPhase then
                    devirtualizationAnalysisPhase then
                    dcePhase then
                    removeRedundantCallsToFileInitializersPhase then
                    devirtualizationPhase then
                    propertyAccessorInlinePhase then // Have to run after link dependencies phase, because fields
                    // from dependencies can be changed during lowerings.
                    inlineClassPropertyAccessorsPhase then
                    redundantCoercionsCleaningPhase then
                    unboxInlinePhase then
                    createLLVMDeclarationsPhase then
                    ghaPhase
    )
}

private fun <Context : CommonBackendContext, Input, Output> sink(output: (Input) -> Output) = object : CompilerPhase<Context, Input, Output> {
    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: Context, input: Input): Output =
            output(input)
}

private fun <Context : CommonBackendContext, Input> sinkId() = sink<Context, Input, Input> { it }

private fun <Context : CommonBackendContext, Input> stealIdentity(phase: NamedCompilerPhase<Context, Input>): NamedCompilerPhase<Context, Input> {
    return NamedCompilerPhase(phase.name, phase.description, lower = sinkId())
}

private inline fun <Context : CommonBackendContext, Input> optionalPhase(phaseOrNull: () -> NamedCompilerPhase<Context, Input>?) =
        phaseOrNull() ?: sinkId()

private inline fun <Context : CommonBackendContext, Input> optionalPhase(cond: Boolean, phase: () -> NamedCompilerPhase<Context, Input>): NamedCompilerPhase<Context, Input> =
        if (cond) {
            phase()
        } else {
            stealIdentity(phase())
        }