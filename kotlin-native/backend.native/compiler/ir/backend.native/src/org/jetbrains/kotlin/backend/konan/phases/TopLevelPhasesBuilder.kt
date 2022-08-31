/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.phases

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

class TopLevelPhasesBuilder(
        private val config: KonanConfig,
        private val arguments: K2NativeCompilerArguments,
) {


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
        override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: Context, input: Input) : Output =
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

    fun <C: CommonBackendContext, Input, Output> myLower(op: (C, Input) -> Output) = object : CompilerPhase<C, Input, Output> {
        override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: C, input: Input): Output {
            return op(context, input)
        }
    }

    private fun <C: CommonBackendContext, Data> myLower2(
            op: (C, Data) -> Data,
            description: String,
            name: String,
            prerequisite: Set<NamedCompilerPhase<C, *>> = emptySet()
    ): NamedCompilerPhase<C, Data> =
            NamedCompilerPhase(
                    name = name,
                    description = description,
                    prerequisite = prerequisite,
                    lower = object : SameTypeCompilerPhase<C, Data> {
                        override fun invoke(
                                phaseConfig: PhaseConfig,
                                phaserState: PhaserState<Data>,
                                context: C,
                                input: Data
                        ): Data {
                            return op(context, input)
                        }
                    },
                    actions = setOf()
            )

    fun buildKlib(config: KonanConfig, environment: KotlinCoreEnvironment) {

        // We don't need boolean input, but phasing machinery is too complex, so keep this hack for now.
        val frontendPhase = NamedCompilerPhase<FrontendContext, Boolean>(
                "FrontEnd", "Frontend",
                lower = object : SameTypeCompilerPhase<FrontendContext, Boolean> {
                    override fun invoke(
                            phaseConfig: PhaseConfig,
                            phaserState: PhaserState<Boolean>,
                            context: FrontendContext,
                            input: Boolean
                    ): Boolean {
                        return context.frontendPhase(config)
                    }
                },
        )

        val createSymbolTablePhase = myLower2<PsiToIrContext, Unit>(
                op = { context, _ ->
                    context.symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), IrFactoryImpl)
                },
                name = "CreateSymbolTable",
                description = "Create SymbolTable",
                prerequisite = emptySet()
        )
        val psiToIrPhase = myLower2<PsiToIrContext, Unit>(
                op = { context, _ ->
                    psiToIr(context,
                            config,
                            context.symbolTable!!,
                            isProducingLibrary = true,
                            useLinkerWhenProducingLibrary = false)
                },
                name = "Psi2Ir",
                description = "Psi to IR conversion and klib linkage",
                prerequisite = setOf(createSymbolTablePhase)
        )

        val destroySymbolTablePhase = myLower2<PsiToIrContext, Unit>(
                op = { context, _ ->
                    context.symbolTable = null // TODO: invalidate symbolTable itself.
                },
                name = "DestroySymbolTable",
                description = "Destroy SymbolTable",
                prerequisite = setOf(createSymbolTablePhase)
        )


        val serializerPhase = namedUnitPhase<KlibProducingContext>(
                lower = myLower { context, _ ->
                    val expectActualLinker = config.configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER) ?: false
                    val messageLogger = config.configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None
                    val relativePathBase = config.configuration.get(CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES) ?: emptyList()
                    val normalizeAbsolutePaths = config.configuration.get(CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH) ?: false

                    context.serializedIr = context.irModule?.let { ir ->
                        KonanIrModuleSerializer(
                                messageLogger, ir.irBuiltins, context.expectDescriptorToSymbol,
                                skipExpects = !expectActualLinker,
                                compatibilityMode = CompatibilityMode.CURRENT,
                                normalizeAbsolutePaths = normalizeAbsolutePaths,
                                sourceBaseDirs = relativePathBase,
                        ).serializedIrModule(ir)
                    }

                    val serializer = KlibMetadataMonolithicSerializer(
                            config.configuration.languageVersionSettings,
                            config.configuration.get(CommonConfigurationKeys.METADATA_VERSION)!!,
                            config.project,
                            exportKDoc = ConfigChecks(config).shouldExportKDoc(),
                            !expectActualLinker, includeOnlyModuleContent = true)
                    context.serializedMetadata = serializer.serializeModule(context.moduleDescriptor)
                },
                name = "Serializer",
                description = "Serialize descriptor tree and inline IR bodies",
                prerequisite = setOf()
        )

        val produceKlibPhase = namedUnitPhase<KlibProducingContext>(
                name = "ProduceOutput",
                description = "Produce output",
                lower = myLower { context, _ ->
                    produceKlib(context, context.config)
                },
                prerequisite = setOf()
        )

        val irGen = NamedCompilerPhase<PsiToIrContext, Unit>(
                "IRGen",
                "IR generation",
                nlevels = 1,
                lower = createSymbolTablePhase then
                        psiToIrPhase then
                        destroySymbolTablePhase
        )

        val generateKlib = NamedCompilerPhase<KlibProducingContext, Unit>(
                "GenerateKlib",
                "Library serialization",
                nlevels = 1,
                lower = serializerPhase then
                        produceKlibPhase
        )

        val context = Context(config)
        val messageCollector = config.configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY) ?: MessageCollector.NONE

        val frontendContext: FrontendContext = context
        frontendContext.environment = environment
        frontendPhase.invokeToplevel(createPhaseConfig(frontendPhase, arguments, messageCollector), frontendContext, true)

        val psiToIrContext: PsiToIrContext = context
        irGen.invokeToplevel(createPhaseConfig(irGen, arguments, messageCollector), psiToIrContext, Unit)

        // TODO: Create new specialized context instead
        val klibProducingContext: KlibProducingContext = context
        generateKlib.invokeToplevel(createPhaseConfig(generateKlib, arguments, messageCollector), klibProducingContext, Unit)

        val time = frontendPhase.time + irGen.time + generateKlib.time
        println("It took ${time}")
    }

    fun buildFramework(config: KonanConfig, environment: KotlinCoreEnvironment) {

        var time = 0L

        // We don't need boolean input, but phasing machinery is too complex, so keep this hack for now.
        val frontendPhase = NamedCompilerPhase<FrontendContext, Boolean>(
                "FrontEnd", "Frontend",
                lower = object : SameTypeCompilerPhase<FrontendContext, Boolean> {
                    override fun invoke(
                            phaseConfig: PhaseConfig,
                            phaserState: PhaserState<Boolean>,
                            context: FrontendContext,
                            input: Boolean
                    ): Boolean {
                        return context.frontendPhase(config)
                    }
                },
        )

        val createSymbolTablePhase = myLower2<PsiToIrContext, Unit>(
                op = { context, _ ->
                    context.symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), IrFactoryImpl)
                },
                name = "CreateSymbolTable",
                description = "Create SymbolTable",
                prerequisite = emptySet()
        )

        val objCExportPhase = myLower2<ObjCExportContext, Unit>(
                op = { context, _ ->
                    context.objCExport = ObjCExport(context, context, context.symbolTable!!, context.config)
                },
                name = "ObjCExport",
                description = "Objective-C header generation",
                prerequisite = setOf(createSymbolTablePhase)
        )

        val psiToIrPhase = myLower2<PsiToIrContext, Unit>(
                op = { context, _ ->
                    psiToIr(context,
                            config,
                            context.symbolTable!!,
                            isProducingLibrary = true,
                            useLinkerWhenProducingLibrary = false)
                },
                name = "Psi2Ir",
                description = "Psi to IR conversion and klib linkage",
                prerequisite = setOf(createSymbolTablePhase)
        )

        val destroySymbolTablePhase = myLower2<PsiToIrContext, Unit>(
                op = { context, _ ->
                    context.symbolTable = null // TODO: invalidate symbolTable itself.
                },
                name = "DestroySymbolTable",
                description = "Destroy SymbolTable",
                prerequisite = setOf(createSymbolTablePhase)
        )

        val context = Context(config)
        val messageCollector = config.configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY) ?: MessageCollector.NONE

        val frontendContext: FrontendContext = context
        frontendContext.environment = environment
        frontendPhase.invokeToplevel(createPhaseConfig(frontendPhase, arguments, messageCollector), frontendContext, true)
        time += frontendPhase.time
        if (config.omitFrameworkBinary) {
            val irGen = NamedCompilerPhase<ObjCExportContext, Unit>(
                    "IRGen",
                    "IR generation",
                    nlevels = 1,
                    lower = createSymbolTablePhase then
                            objCExportPhase then
                            destroySymbolTablePhase
            )
            val produceFramework = myLower2<ObjCExportContext, Unit>(
                    op = { ctx, _ ->
                        produceFrameworkInterface(ctx.objCExport)
                    },
                    name = "ProduceFramework",
                    description = "Create Apple Framework"
            )
            val objCExportContext: ObjCExportContext = context
            irGen.invokeToplevel(createPhaseConfig(irGen, arguments, messageCollector), objCExportContext, Unit)
            time += irGen.time
            produceFramework.invokeToplevel(createPhaseConfig(produceFramework, arguments, messageCollector), objCExportContext, Unit)
            time += produceFramework.time
        } else {
            val irGen = NamedCompilerPhase<ObjCExportContext, Unit>(
                    "IRGen",
                    "IR generation",
                    nlevels = 1,
                    lower = createSymbolTablePhase then
                            objCExportPhase then
                            psiToIrPhase then
                            destroySymbolTablePhase
            )
            val psiToIrContext: ObjCExportContext = context
            irGen.invokeToplevel(createPhaseConfig(irGen, arguments, messageCollector), psiToIrContext, Unit)
            time += irGen.time
        }

        println("It took ${time}")
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