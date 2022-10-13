/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.IrValidator
import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.backend.common.checkDeclarationParents
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.lower.CacheInfoBuilder
import org.jetbrains.kotlin.backend.konan.lower.ExpectToActualDefaultValueCopier
import org.jetbrains.kotlin.backend.konan.lower.SamSuperTypesChecker
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.objcexport.createCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.produceObjCExportInterface
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.name.FqName

internal fun moduleValidationCallback(state: ActionState, module: IrModuleFragment, context: Context) {
    if (!context.config.needVerifyIr) return

    val validatorConfig = IrValidatorConfig(
        abortOnError = false,
        ensureAllNodesAreDifferent = true,
        checkTypes = true,
        checkDescriptors = false
    )
    try {
        module.accept(IrValidator(context, validatorConfig), null)
        module.checkDeclarationParents()
    } catch (t: Throwable) {
        // TODO: Add reference to source.
        if (validatorConfig.abortOnError)
            throw IllegalStateException("Failed IR validation ${state.beforeOrAfter} ${state.phase}", t)
        else context.reportCompilationWarning("[IR VALIDATION] ${state.beforeOrAfter} ${state.phase}: ${t.message}")
    }
}

internal fun fileValidationCallback(state: ActionState, irFile: IrFile, context: Context) {
    val validatorConfig = IrValidatorConfig(
        abortOnError = false,
        ensureAllNodesAreDifferent = true,
        checkTypes = true,
        checkDescriptors = false
    )
    try {
        irFile.accept(IrValidator(context, validatorConfig), null)
        irFile.checkDeclarationParents()
    } catch (t: Throwable) {
        // TODO: Add reference to source.
        if (validatorConfig.abortOnError)
            throw IllegalStateException("Failed IR validation ${state.beforeOrAfter} ${state.phase}", t)
        else context.reportCompilationWarning("[IR VALIDATION] ${state.beforeOrAfter} ${state.phase}: ${t.message}")
    }
}

internal fun konanUnitPhase(
        name: String,
        description: String,
        prerequisite: Set<AbstractNamedCompilerPhase<Context, *, *>> = emptySet(),
        op: Context.() -> Unit
) = namedOpUnitPhase(name, description, prerequisite, op)

/**
 * Valid from [createSymbolTablePhase] until [destroySymbolTablePhase].
 */
private var Context.symbolTable: SymbolTable? by Context.nullValue()

internal val createSymbolTablePhase = konanUnitPhase(
        op = {
            this.symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), IrFactoryImpl)
        },
        name = "CreateSymbolTable",
        description = "Create SymbolTable"
)

internal val objCExportPhase = konanUnitPhase(
        op = {
            val objcInterface = when {
                !config.target.family.isAppleFamily -> null
                config.produce != CompilerOutputKind.FRAMEWORK -> null
                else -> produceObjCExportInterface(this)
            }
            val codeSpec = objcInterface?.createCodeSpec(symbolTable!!)
            objCExport = ObjCExport(this, objcInterface, codeSpec)
        },
        name = "ObjCExport",
        description = "Objective-C header generation",
        prerequisite = setOf(createSymbolTablePhase)
)

internal val buildCExportsPhase = konanUnitPhase(
        op = {
            this.cAdapterGenerator = CAdapterGenerator(this).also {
                it.buildExports(this.symbolTable!!)
            }
        },
        name = "BuildCExports",
        description = "Build C exports",
        prerequisite = setOf(createSymbolTablePhase)
)

internal val psiToIrPhase = konanUnitPhase(
        op = {
            this.psiToIr(symbolTable!!,
                    isProducingLibrary = config.produce == CompilerOutputKind.LIBRARY,
                    useLinkerWhenProducingLibrary = false)
        },
        name = "Psi2Ir",
        description = "Psi to IR conversion and klib linkage",
        prerequisite = setOf(createSymbolTablePhase)
)

internal val buildAdditionalCacheInfoPhase = konanUnitPhase(
        op = {
            irModules.values.single().let { module ->
                val moduleDeserializer = irLinker.moduleDeserializers[module.descriptor]
                if (moduleDeserializer == null) {
                    require(module.descriptor.isFromInteropLibrary()) { "No module deserializer for ${module.descriptor}" }
                } else {
                    CacheInfoBuilder(this, moduleDeserializer).build()
                }
            }
        },
        name = "BuildAdditionalCacheInfo",
        description = "Build additional cache info (inline functions bodies and fields of classes)",
        prerequisite = setOf(psiToIrPhase)
)

internal val destroySymbolTablePhase = konanUnitPhase(
        op = {
            this.symbolTable = null // TODO: invalidate symbolTable itself.
        },
        name = "DestroySymbolTable",
        description = "Destroy SymbolTable",
        prerequisite = setOf(createSymbolTablePhase)
)

// TODO: We copy default value expressions from expects to actuals before IR serialization,
// because the current infrastructure doesn't allow us to get them at deserialization stage.
// That requires some design and implementation work.
internal val copyDefaultValuesToActualPhase = konanUnitPhase(
        op = {
            ExpectToActualDefaultValueCopier(irModule!!).process()
        },
        name = "CopyDefaultValuesToActual",
        description = "Copy default values from expect to actual declarations"
)

/*
 * Sometimes psi2ir produces IR with non-trivial variance in super types of SAM conversions (this is a language design issue).
 * Earlier this was solved with just erasing all such variances but this might lead to some other hard to debug problems,
 * so after handling the majority of corner cases correctly in psi2ir it is safe to assume that such cases don't get here and
 * even if they do, then it's better to throw an error right away than to dig out weird crashes down the pipeline or even at runtime.
 * We explicitly check this, also fixing older klibs built with previous compiler versions by applying the same trick as before.
 */
internal val checkSamSuperTypesPhase = konanUnitPhase(
        op = {
            // Handling types in current module not recursively:
            // psi2ir can produce SAM conversions with variances in type arguments of type arguments.
            // See https://youtrack.jetbrains.com/issue/KT-49384.
            // So don't go deeper than top-level arguments to avoid the compiler emitting false-positive errors.
            // Lowerings can handle this.
            // Also such variances are allowed in the language for manual implementations of interfaces.
            irModule!!.files
                    .forEach { SamSuperTypesChecker(this, it, mode = SamSuperTypesChecker.Mode.THROW, recurse = false).run() }
            // TODO: This is temporary for handling klibs produced with earlier compiler versions.
            // Handling types in dependencies recursively, just to be extra safe: don't change something that works.
            irModules.values
                    .flatMap { it.files }
                    .forEach { SamSuperTypesChecker(this, it, mode = SamSuperTypesChecker.Mode.ERASE, recurse = true).run() }
        },
        name = "CheckSamSuperTypes",
        description = "Check SAM conversions super types"
)

internal val serializerPhase = konanUnitPhase(
        op = {
            val expectActualLinker = config.configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER) ?: false
            val messageLogger = config.configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None
            val relativePathBase = config.configuration.get(CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES) ?: emptyList()
            val normalizeAbsolutePaths = config.configuration.get(CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH) ?: false

            serializedIr = irModule?.let { ir ->
                KonanIrModuleSerializer(
                    messageLogger, ir.irBuiltins, expectDescriptorToSymbol, skipExpects = !expectActualLinker, compatibilityMode = CompatibilityMode.CURRENT, normalizeAbsolutePaths = normalizeAbsolutePaths, sourceBaseDirs = relativePathBase
                ).serializedIrModule(ir)
            }

            val serializer = KlibMetadataMonolithicSerializer(
                this.config.configuration.languageVersionSettings,
                config.configuration.get(CommonConfigurationKeys.METADATA_VERSION)!!,
                config.project,
                exportKDoc = this.shouldExportKDoc(),
                !expectActualLinker, includeOnlyModuleContent = true)
            serializedMetadata = serializer.serializeModule(moduleDescriptor)
        },
        name = "Serializer",
        description = "Serialize descriptor tree and inline IR bodies"
)

internal val saveAdditionalCacheInfoPhase = konanUnitPhase(
        op = { CacheStorage(this).saveAdditionalCacheInfo() },
        name = "SaveAdditionalCacheInfo",
        description = "Save additional cache info (inline functions bodies and fields of classes)"
)

internal val objectFilesPhase = konanUnitPhase(
        op = { compilerOutput = BitcodeCompiler(this).makeObjectFiles(bitcodeFileName) },
        name = "ObjectFiles",
        description = "Bitcode to object file"
)

internal val linkerPhase = konanUnitPhase(
        op = { Linker(this).link(compilerOutput) },
        name = "Linker",
        description = "Linker"
)

internal val finalizeCachePhase = konanUnitPhase(
        op = { CacheStorage(this).renameOutput() },
        name = "FinalizeCache",
        description = "Finalize cache (rename temp to the final dist)"
)

internal val allLoweringsPhase = SameTypeNamedCompilerPhase(
        name = "IrLowering",
        description = "IR Lowering",
        // TODO: The lowerings before inlinePhase should be aligned with [NativeInlineFunctionResolver.kt]
        lower = performByIrFile(
                name = "IrLowerByFile",
                description = "IR Lowering by file",
                lower = listOf(
                        createFileLowerStatePhase,
                        removeExpectDeclarationsPhase,
                        stripTypeAliasDeclarationsPhase,
                        lowerBeforeInlinePhase,
                        arrayConstructorPhase,
                        lateinitPhase,
                        sharedVariablesPhase,
                        inventNamesForLocalClasses,
                        extractLocalClassesFromInlineBodies,
                        wrapInlineDeclarationsWithReifiedTypeParametersLowering,
                        inlinePhase,
                        provisionalFunctionExpressionPhase,
                        postInlinePhase,
                        contractsDslRemovePhase,
                        annotationImplementationPhase,
                        rangeContainsLoweringPhase,
                        forLoopsPhase,
                        flattenStringConcatenationPhase,
                        foldConstantLoweringPhase,
                        computeStringTrimPhase,
                        stringConcatenationPhase,
                        stringConcatenationTypeNarrowingPhase,
                        enumConstructorsPhase,
                        initializersPhase,
                        localFunctionsPhase,
                        tailrecPhase,
                        defaultParameterExtentPhase,
                        innerClassPhase,
                        dataClassesPhase,
                        ifNullExpressionsFusionPhase,
                        testProcessorPhase,
                        delegationPhase,
                        functionReferencePhase,
                        singleAbstractMethodPhase,
                        enumWhenPhase,
                        builtinOperatorPhase,
                        finallyBlocksPhase,
                        enumClassPhase,
                        enumUsagePhase,
                        interopPhase,
                        varargPhase,
                        kotlinNothingValueExceptionPhase,
                        coroutinesPhase,
                        typeOperatorPhase,
                        expressionBodyTransformPhase,
                        constantInliningPhase,
                        fileInitializersPhase,
                        bridgesPhase,
                        exportInternalAbiPhase,
                        useInternalAbiPhase,
                        autoboxPhase,
                )
        ),
        actions = setOf(defaultDumper, ::moduleValidationCallback)
)

internal val dependenciesLowerPhase = SameTypeNamedCompilerPhase(
        name = "LowerLibIR",
        description = "Lower library's IR",
        prerequisite = emptySet(),
        lower = object : CompilerPhase<Context, IrModuleFragment, IrModuleFragment> {
            override fun invoke(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment): IrModuleFragment {
                val files = mutableListOf<IrFile>()
                files += input.files
                input.files.clear()

                // TODO: KonanLibraryResolver.TopologicalLibraryOrder actually returns libraries in the reverse topological order.
                context.librariesWithDependencies
                        .reversed()
                        .forEach {
                            val libModule = context.irModules[it.libraryName]
                                    ?: return@forEach

                            input.files += libModule.files
                            allLoweringsPhase.invoke(phaseConfig, phaserState, context, input)

                            input.files.clear()
                        }

                // Save all files for codegen in reverse topological order.
                // This guarantees that libraries initializers are emitted in correct order.
                context.librariesWithDependencies
                        .forEach {
                            val libModule = context.irModules[it.libraryName]
                                    ?: return@forEach
                            input.files += libModule.files
                        }

                input.files += files

                return input
            }
        })

internal val umbrellaCompilation = SameTypeNamedCompilerPhase(
        name = "UmbrellaCompilation",
        description = "A batched compilation with shared FE and ME phases",
        prerequisite = emptySet(),
        lower = object : CompilerPhase<Context, Unit, Unit> {
            override fun invoke(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Unit>, context: Context, input: Unit) {
                val module = context.irModules.values.single()

                val files = module.files.toList()
                module.files.clear()
                val functionInterfaceFiles = files.filter { it.isFunctionInterfaceFile }

                for (file in files) {
                    if (file.isFunctionInterfaceFile) continue

                    // Pretend we're about to create a single file cache.
                    context.config.cacheSupport.libraryToCache!!.strategy =
                            CacheDeserializationStrategy.SingleFile(file.path, file.fqName.asString())

                    module.files += file
                    if (context.shouldDefineFunctionClasses)
                        module.files += functionInterfaceFiles

                    entireBackend.invoke(phaseConfig, phaserState, context, Unit)

                    module.files.clear()
                    context.irModule!!.files.clear() // [dependenciesLowerPhase] puts all files to [context.irModule] for codegen.
                }

                module.files += files
            }
        })

internal val entryPointPhase = makeCustomPhase<Context, IrModuleFragment>(
        name = "addEntryPoint",
        description = "Add entry point for program",
        prerequisite = emptySet(),
        op = { context, _ ->
            require(context.config.produce == CompilerOutputKind.PROGRAM)

            val entryPoint = context.ir.symbols.entryPoint!!.owner
            val file = if (context.llvmModuleSpecification.containsDeclaration(entryPoint)) {
                entryPoint.file
            } else {
                // `main` function is compiled to other LLVM module.
                // For example, test running support uses `main` defined in stdlib.
                context.irModule!!.addFile(NaiveSourceBasedFileEntryImpl("entryPointOwner"), FqName("kotlin.native.internal.abi"))
            }

            file.addChild(makeEntryPoint(context))
        }
)

internal val bitcodePhase = SameTypeNamedCompilerPhase(
        name = "Bitcode",
        description = "LLVM Bitcode generation",
        lower = returnsInsertionPhase then
                buildDFGPhase then
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
                escapeAnalysisPhase then
                codegenPhase then
                cStubsPhase
)

private val bitcodePostprocessingPhase = SameTypeNamedCompilerPhase(
        name = "BitcodePostprocessing",
        description = "Optimize and rewrite bitcode",
        lower = checkExternalCallsPhase then
                bitcodeOptimizationPhase then
                coveragePhase then
                removeRedundantSafepointsPhase then
                optimizeTLSDataLoadsPhase then
                rewriteExternalCallsCheckerGlobals
)

private val backendCodegen = SameTypeNamedCompilerPhase(
        name = "Backend codegen",
        description = "Backend code generation",
        lower = entryPointPhase then
                allLoweringsPhase then // Lower current module first.
                dependenciesLowerPhase then // Then lower all libraries in topological order.
                                            // With that we guarantee that inline functions are unlowered while being inlined.
                bitcodePhase then
                verifyBitcodePhase then
                printBitcodePhase then
                linkBitcodeDependenciesPhase then
                bitcodePostprocessingPhase
)

internal val createGenerationStatePhase = namedUnitPhase(
        name = "CreateGenerationState",
        description = "Create generation state",
        lower = object : CompilerPhase<Context, Unit, Unit> {
            override fun invoke(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Unit>, context: Context, input: Unit) {
                context.generationState = NativeGenerationState(context)
            }
        }
)

internal val disposeGenerationStatePhase = namedUnitPhase(
        name = "DisposeGenerationState",
        description = "Dispose generation state",
        lower = object : CompilerPhase<Context, Unit, Unit> {
            override fun invoke(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Unit>, context: Context, input: Unit) {
                context.disposeGenerationState()
            }
        }
)

private val phasesOverMainModule = SameTypeNamedCompilerPhase(
        name = "PhasesOverMainModule",
        description = "Phases over main module",
        lower = takeFromContext<Context, Unit, IrModuleFragment> { it.irModule!! } then
                specialBackendChecksPhase then
                backendCodegen then
                unitSink(),
        prerequisite = setOf(psiToIrPhase)
)

private val entireBackend = SameTypeNamedCompilerPhase(
        name = "EntireBackend",
        description = "Entire backend",
        lower = createGenerationStatePhase then
                buildAdditionalCacheInfoPhase then
                phasesOverMainModule then
                saveAdditionalCacheInfoPhase then
                produceOutputPhase then
                objectFilesPhase then
                linkerPhase then
                finalizeCachePhase then
                disposeGenerationStatePhase
)

private val middleEnd = SameTypeNamedCompilerPhase(
        name = "MiddleEnd",
        description = "Build and prepare IR for back end",
        lower = createSymbolTablePhase then
                objCExportPhase then
                buildCExportsPhase then
                psiToIrPhase then
                destroySymbolTablePhase then
                copyDefaultValuesToActualPhase then
                checkSamSuperTypesPhase then
                serializerPhase then
                functionsWithoutBoundCheck
)

private val singleCompilation = SameTypeNamedCompilerPhase(
        name = "SingleCompilation",
        description = "Single compilation",
        lower = entireBackend
)

internal val toplevelPhase: CompilerPhase<Context, Unit, Unit> = namedUnitPhase(
        name = "Compiler",
        description = "The whole compilation process",
        lower = middleEnd then
                singleCompilation then
                umbrellaCompilation
)

// Have to hide Context as type parameter in order to expose toplevelPhase outside of this module.
val toplevelPhaseErased: CompilerPhase<*, Unit, Unit>
    get() = toplevelPhase

internal fun PhaseConfig.disableIf(phase: AnyNamedPhase, condition: Boolean) {
    if (condition) disable(phase)
}

internal fun PhaseConfig.disableUnless(phase: AnyNamedPhase, condition: Boolean) {
    if (!condition) disable(phase)
}

internal fun PhaseConfig.konanPhasesConfig(config: KonanConfig) {
    with(config.configuration) {
        // The original comment around [checkSamSuperTypesPhase] still holds, but in order to be on par with JVM_IR
        // (which doesn't report error for these corner cases), we turn off the checker for now (the problem with variances
        // is workarounded in [FunctionReferenceLowering] by taking erasure of SAM conversion type).
        // Also see https://youtrack.jetbrains.com/issue/KT-50399 for more details.
        disable(checkSamSuperTypesPhase)

        disableIf(singleCompilation, config.producePerFileCache)
        disableUnless(umbrellaCompilation, config.producePerFileCache)

        // Don't serialize anything to a final executable.
        disableUnless(serializerPhase, config.produce == CompilerOutputKind.LIBRARY)
        disableUnless(entryPointPhase, config.produce == CompilerOutputKind.PROGRAM)
        disableUnless(buildAdditionalCacheInfoPhase, config.produce.isCache)
        disableUnless(saveAdditionalCacheInfoPhase, config.produce.isCache)
        disableUnless(finalizeCachePhase, config.produce.isCache)
        disableUnless(exportInternalAbiPhase, config.produce.isCache)
        disableUnless(buildCExportsPhase, config.produce.isNativeLibrary)
        disableUnless(functionsWithoutBoundCheck, config.involvesCodegen)
        disableUnless(backendCodegen, config.involvesCodegen)
        disableUnless(checkExternalCallsPhase, getBoolean(KonanConfigKeys.CHECK_EXTERNAL_CALLS))
        disableUnless(rewriteExternalCallsCheckerGlobals, getBoolean(KonanConfigKeys.CHECK_EXTERNAL_CALLS))
        disableUnless(stringConcatenationTypeNarrowingPhase, config.optimizationsEnabled)
        disableUnless(optimizeTLSDataLoadsPhase, config.optimizationsEnabled)
        if (!config.involvesLinkStage) {
            disable(bitcodePostprocessingPhase)
            disable(linkBitcodeDependenciesPhase)
            disable(objectFilesPhase)
            disable(linkerPhase)
        }
        disableIf(testProcessorPhase, getNotNull(KonanConfigKeys.GENERATE_TEST_RUNNER) == TestRunnerKind.NONE)
        if (!config.optimizationsEnabled) {
            disable(buildDFGPhase)
            disable(devirtualizationAnalysisPhase)
            disable(devirtualizationPhase)
            disable(escapeAnalysisPhase)
            // Inline accessors only in optimized builds due to separate compilation and possibility to get broken
            // debug information.
            disable(propertyAccessorInlinePhase)
            disable(unboxInlinePhase)
            disable(inlineClassPropertyAccessorsPhase)
            disable(dcePhase)
            disable(removeRedundantCallsToFileInitializersPhase)
            disable(ghaPhase)
        }
        disableUnless(verifyBitcodePhase, config.needCompilerVerification || getBoolean(KonanConfigKeys.VERIFY_BITCODE))

        disableUnless(fileInitializersPhase, config.propertyLazyInitialization)
        disableUnless(removeRedundantCallsToFileInitializersPhase, config.propertyLazyInitialization)

        disableUnless(removeRedundantSafepointsPhase, config.memoryModel == MemoryModel.EXPERIMENTAL)

        if (config.metadataKlib || config.omitFrameworkBinary) {
            disable(psiToIrPhase)
            disable(copyDefaultValuesToActualPhase)
            disable(phasesOverMainModule)
            disable(checkSamSuperTypesPhase)
        }
    }
}
