/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.phases

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

class DynamicNativeCompilerDriver(
        private val config: KonanConfig,
        private val arguments: K2NativeCompilerArguments,
) {
    private val messageCollector = config.configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            ?: MessageCollector.NONE

    private fun buildCodegenPhases() = namedUnitPhase(
            name = "Backend",
            description = "All backend",
            lower = getBackendCodegen() then
                    // TODO: Drop
                    produceOutputPhase then
                    disposeLLVMPhase then
                    unitSink()
    )

    private fun <Context : CommonBackendContext, Input, Output> sink(output: (Input) -> Output) = object : CompilerPhase<Context, Input, Output> {
        override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: Context, input: Input): Output =
                output(input)
    }

    private fun <Context : CommonBackendContext, Input> sinkId() = sink<Context, Input, Input> { it }

    private inline fun optionalPhase(phaseOrNull: () -> NamedCompilerPhase<Context, IrModuleFragment>?) =
            phaseOrNull() ?: sinkId()

    private fun getBackendCodegen(): NamedCompilerPhase<Context, Unit> {
        val dumpTestPhase = optionalPhase {
            config.testDumpFile?.let { getDumpTestsPhase(it) }
        }

        val verifyBitcode = optionalPhase {
            val needBitcodeVerification = config.needCompilerVerification || config.configuration.getBoolean(KonanConfigKeys.VERIFY_BITCODE)
            verifyBitcodePhase.takeIf { needBitcodeVerification }
        }

        val entryPoint = optionalPhase {
            entryPointPhase.takeIf { config.produce == CompilerOutputKind.PROGRAM }
        }

        val printBitcode = optionalPhase {
            printBitcodePhase.takeIf { config.configuration.getBoolean(KonanConfigKeys.PRINT_BITCODE) }
        }

        return namedUnitPhase(
                name = "Backend codegen",
                description = "Backend code generation",
                lower = takeFromContext<Context, Unit, IrModuleFragment> { it.irModule!! } then
                        entryPoint then
                        functionsWithoutBoundCheck then
                        allLoweringsPhase then // Lower current module first.
                        dependenciesLowerPhase then // Then lower all libraries in topological order.
                        // With that we guarantee that inline functions are unlowered while being inlined.
                        dumpTestPhase then
                        getBitcodePhase() then
                        verifyBitcode then
                        printBitcode then
                        linkBitcodeDependenciesPhase then
                        bitcodePostprocessingPhase then
                        unitSink()
        )
    }

//    private fun buildProgramTopLevelPhases(): CompilerPhase<*, Unit, Unit> {
//        val createSymbolTablePhase = getCreateSymbolTablePhase<Any>()
//        val psiToIrPhase = getPsiToIrPhase()
//        return namedUnitPhase(
//                name = "Program compiler",
//                description = "The whole compilation process",
//                lower = createSymbolTablePhase then
//                        psiToIrPhase then
//                        destroySymbolTablePhase then
//                        copyDefaultValuesToActualPhase then
//                        checkSamSuperTypesPhase then
//                        specialBackendChecksPhase then
//                        buildCodegenPhases() then
//                        objectFilesPhase then
//                        linkerPhase
//        )
//    }

    private fun buildFileCache(fileName: String, cacheKind: CompilerOutputKind) {
        val phaseConfig = config.configuration.get(CLIConfigurationKeys.PHASE_CONFIG)!!
        val subConfiguration = config.configuration.copy()
        subConfiguration.put(KonanConfigKeys.PRODUCE, cacheKind)
        subConfiguration.put(KonanConfigKeys.FILE_TO_CACHE, fileName)
        subConfiguration.put(KonanConfigKeys.MAKE_PER_FILE_CACHE, false)
        subConfiguration.put(CLIConfigurationKeys.PHASE_CONFIG, phaseConfig.toBuilder().build())
//        KonanConfig(project, subConfiguration).runTopLevelPhases(phases)
    }

//    private fun buildStaticCacheTopLevelPhases(): CompilerPhase<*, Unit, Unit> {
//        val fileNames = config.configuration.get(KonanConfigKeys.LIBRARY_TO_ADD_TO_CACHE)?.let { libPath ->
//            if (config.configuration.get(KonanConfigKeys.MAKE_PER_FILE_CACHE) != true)
//                null
//            else {
//                val lib = createKonanLibrary(File(libPath), "default", null, true)
//                (0 until lib.fileCount()).map { fileIndex ->
//                    val proto = IrFile.parseFrom(lib.file(fileIndex).codedInputStream, ExtensionRegistryLite.newInstance())
//                    proto.fileEntry.name
//                }
//            }
//        }
//        if (fileNames == null) {
//        } else {
//            fileNames.forEach { buildFileCache(it, CompilerOutputKind.PRELIMINARY_CACHE) }
//            fileNames.forEach { buildFileCache(it, config.configuration.get(KonanConfigKeys.PRODUCE)!!) }
//        }
//        return namedUnitPhase(
//                name = "Static cache compiler",
//                description = "The whole compilation process",
//                lower = getCreateSymbolTablePhase() then
//                        getPsiToIrPhase() then
//                        buildAdditionalCacheInfoPhase then
//                        destroySymbolTablePhase then
//                        copyDefaultValuesToActualPhase then
//                        checkSamSuperTypesPhase then
//                        specialBackendChecksPhase then
//                        buildCodegenPhases() then
//                        saveAdditionalCacheInfoPhase then
//                        objectFilesPhase then
//                        linkerPhase then
//                        finalizeCachePhase
//        )
//    }

    private fun <Context: PhaseContext, Data> runTopLevelPhase(context: Context, phase: NamedCompilerPhase<Context, Data>, input: Data) =
        phase.invokeToplevel(createPhaseConfig(phase, arguments, messageCollector), context, input)

    private fun <Context: PhaseContext> runTopLevelPhaseUnit(context: Context, phase: NamedCompilerPhase<Context, Unit>) =
            runTopLevelPhase(context, phase, Unit)

    fun buildKlib(config: KonanConfig, environment: KotlinCoreEnvironment) {
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

    fun buildFramework(config: KonanConfig, environment: KotlinCoreEnvironment) {
        var time = 0L

        val frontendPhase = Phases.buildFrontendPhase()
        val createSymbolTablePhase = Phases.buildCreateSymbolTablePhase()
        val objCExportPhase = Phases.buildObjCExportPhase(createSymbolTablePhase)
        val psiToIrPhase = Phases.buildTranslatePsiToIrPhase(isProducingLibrary = false, createSymbolTablePhase)
        val destroySymbolTablePhase = Phases.buildDestroySymbolTablePhase(createSymbolTablePhase)

        val context = Context(config)
        context.environment = environment
        if (!runTopLevelPhase(context, frontendPhase, true)) {
            return
        }
        time += frontendPhase.time

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
            time += produceFramework.time
            println("It took ${time}")
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
        time += irGen.time


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

    private fun getBitcodePhase(): NamedCompilerPhase<Context, IrModuleFragment> {
        val lto = optionalPhase {
            ltoPhases().takeIf { requiresLto() }
        }
        val generateDebugInfoHeader = optionalPhase {
            generateDebugInfoHeaderPhase.takeIf { shouldContainAnyDebugInfo() }
        }
        return NamedCompilerPhase(
                name = "Bitcode",
                description = "LLVM Bitcode generation",
                lower = contextLLVMSetupPhase then
                        returnsInsertionPhase then
                        lto then
                        RTTIPhase then
                        generateDebugInfoHeader then
                        escapeAnalysisPhase then
                        codegenPhase then
                        finalizeDebugInfoPhase then
                        cStubsPhase
        )
    }

    private fun requiresLto() = config.optimizationsEnabled

    private fun shouldContainDebugInfo() = config.debug
    private fun shouldContainLocationDebugInfo() = shouldContainDebugInfo() || config.lightDebug
    private fun shouldContainAnyDebugInfo() = shouldContainDebugInfo() || shouldContainLocationDebugInfo()
}