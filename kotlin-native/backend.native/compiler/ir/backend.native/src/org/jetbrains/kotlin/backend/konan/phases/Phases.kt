/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.phases

import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.GlobalHierarchyAnalysis
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.ir.FunctionsWithoutBoundCheckGenerator
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.optimizations.*
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.OperatorNameConventions

internal object Phases {
    fun buildFrontendPhase(): NamedCompilerPhase<FrontendContext, Boolean> {
        // We don't need boolean input, but phasing machinery is too complex, so keep this hack for now.
        val frontendPhase = NamedCompilerPhase<FrontendContext, Boolean>(
                name = "FrontEnd",
                description = "Frontend",
                lower = object : SameTypeCompilerPhase<FrontendContext, Boolean> {
                    override fun invoke(
                            phaseConfig: PhaseConfig,
                            phaserState: PhaserState<Boolean>,
                            context: FrontendContext,
                            input: Boolean
                    ): Boolean {
                        return context.frontendPhase()
                    }
                },
        )
        return frontendPhase
    }

    fun buildSerializerPhase(): NamedCompilerPhase<KlibProducingContext, Unit> = namedUnitPhase(
            lower = myLower { context, _ ->
                val config = context.config
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
                        exportKDoc = config.checks.shouldExportKDoc(),
                        !expectActualLinker, includeOnlyModuleContent = true)
                context.serializedMetadata = serializer.serializeModule(context.moduleDescriptor)
            },
            name = "Serializer",
            description = "Serialize descriptor tree and inline IR bodies",
            prerequisite = setOf()
    )

    fun buildCreateSymbolTablePhase(): NamedCompilerPhase<PsiToIrContext, Unit> = myLower2(
            op = { context, _ ->
                context.symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), IrFactoryImpl)
            },
            name = "CreateSymbolTable",
            description = "Create SymbolTable",
            prerequisite = emptySet()
    )

    fun buildDestroySymbolTablePhase(createSymbolTablePhase: NamedCompilerPhase<PsiToIrContext, Unit>): NamedCompilerPhase<PsiToIrContext, Unit> = myLower2(
            op = { context, _ ->
                context.symbolTable = null // TODO: invalidate symbolTable itself.
            },
            name = "DestroySymbolTable",
            description = "Destroy SymbolTable",
            prerequisite = setOf(createSymbolTablePhase)
    )

    fun buildTranslatePsiToIrPhase(
            isProducingLibrary: Boolean,
            createSymbolTablePhase: NamedCompilerPhase<PsiToIrContext, Unit>,
    ): NamedCompilerPhase<PsiToIrContext, Unit> = myLower2(
            op = { context, _ ->
                psiToIr(context,
                        context.config,
                        context.symbolTable!!,
                        isProducingLibrary,
                        useLinkerWhenProducingLibrary = false)
            },
            name = "Psi2Ir",
            description = "Psi to IR conversion and klib linkage",
            prerequisite = setOf(createSymbolTablePhase)
    )

    fun buildBuildAdditionalCacheInfoPhase(psiToIrPhase: NamedCompilerPhase<PsiToIrContext, Unit>): NamedCompilerPhase<MiddleEndContext, Unit> = myLower2(
            op = { ctx, _ ->
                ctx.irModules.values.single().let { module ->
                    val moduleDeserializer = ctx.irLinker.moduleDeserializers[module.descriptor]
                    if (moduleDeserializer == null) {
                        require(module.descriptor.isFromInteropLibrary()) { "No module deserializer for ${module.descriptor}" }
                    } else {
                        CacheInfoBuilder(ctx, moduleDeserializer).build()
                    }
                }
            },
            name = "BuildAdditionalCacheInfo",
            description = "Build additional cache info (inline functions bodies and fields of classes) $psiToIrPhase",
            prerequisite = setOf()//setOf(psiToIrPhase)
    )

    fun buildSaveAdditionalCacheInfoPhase(): NamedCompilerPhase<CacheAwareContext, Unit> = myLower2(
            op = { ctx, _ ->
                CacheStorage(ctx.config, ctx.llvmImports, ctx.inlineFunctionBodies, ctx.classFields).saveAdditionalCacheInfo()
            },
            name = "SaveAdditionalCacheInfo",
            description = "Save additional cache info (inline functions bodies and fields of classes)"
    )

    fun buildProduceKlibPhase(): NamedCompilerPhase<KlibProducingContext, Unit> = namedUnitPhase(
            name = "ProduceOutput",
            description = "Produce output",
            lower = myLower { context, _ ->
                produceKlib(context, context.config)
            },
            prerequisite = setOf()
    )

    fun buildObjCExportPhase(createSymbolTablePhase: NamedCompilerPhase<PsiToIrContext, Unit>): NamedCompilerPhase<ObjCExportContext, Unit> = myLower2<ObjCExportContext, Unit>(
            op = { context, _ ->
                context.objCExport = ObjCExport(context, context.symbolTable!!, context.config)
            },
            name = "ObjCExport",
            description = "Objective-C header generation",
            // This dependency is actually optional in case of -Xomit-framework-binary.
            prerequisite = setOf(createSymbolTablePhase)
    )

    fun buildProduceFrameworkInterfacePhase(): NamedCompilerPhase<ObjCExportContext, Unit> = myLower2(
            op = { ctx, _ -> produceFrameworkInterface(ctx.objCExport) },
            name = "ProduceFrameworkInterface",
            description = "Create Apple Framework without binary"
    )

    fun buildSpecialBackendChecksPhase(): NamedCompilerPhase<MiddleEndContext, Unit> = myLower2(
            op = { ctx, _ -> ctx.irModule!!.files.forEach { SpecialBackendChecksTraversal(ctx).lower(it) } },
            name = "SpecialBackendChecks",
            description = "Special backend checks"
    )

    fun buildCopyDefaultValuesToActualPhase(): NamedCompilerPhase<MiddleEndContext, Unit> = myLower2(
            op = { ctx, _ -> ExpectToActualDefaultValueCopier(ctx.irModule!!).process() },
            name = "CopyDefaultValuesToActual",
            description = "Copy default values from expect to actual declarations"
    )

    fun buildEntryPointPhase(): NamedCompilerPhase<MiddleEndContext, Unit> = myLower2(
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

    fun buildWriteLlvmModule(): NamedCompilerPhase<LlvmCodegenContext, Unit> = myLower2(
            name = "WriteLlvm",
            description = "Write LLVM module to file",
            op = { context, _ ->
                val output = context.config.tempFiles.nativeBinaryFileName
                context.bitcodeFileName = output
                // Insert `_main` after pipeline so we won't worry about optimizations
                // corrupting entry point.
                insertAliasToEntryPoint(context.llvmModule!!, context.config)
                LLVMWriteBitcodeToFile(context.llvmModule!!, output)
            }
    )

    fun getBuildDFGPhase(): NamedCompilerPhase<LtoContext, IrModuleFragment> = makeKonanModuleOpPhase<LtoContext>(
            name = "BuildDFG",
            description = "Data flow graph building",
            op = { context, irModule ->
                context.moduleDFG = ModuleDFGBuilder(context, irModule).build()
            }
    )

    fun getDevirtualizationAnalysisPhase(dfgPhase: NamedCompilerPhase<LtoContext, IrModuleFragment>): NamedCompilerPhase<LtoContext, IrModuleFragment> = makeKonanModuleOpPhase<LtoContext>(
            name = "DevirtualizationAnalysis",
            description = "Devirtualization analysis",
            prerequisite = setOf(dfgPhase),
            op = { context, _ ->
                context.devirtualizationAnalysisResult = DevirtualizationAnalysis.run(
                        context, context.moduleDFG!!, ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())
                )
            }
    )

    fun getRemoveRedundantCallsToFileInitializersPhase(devirtualizationAnalysisPhase: NamedCompilerPhase<LtoContext, IrModuleFragment>) = makeKonanModuleOpPhase(
            name = "RemoveRedundantCallsToFileInitializersPhase",
            description = "Redundant file initializers calls removal",
            prerequisite = setOf(devirtualizationAnalysisPhase),
            op = { context, _ ->
                val moduleDFG = context.moduleDFG!!
                val externalModulesDFG = ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())

                val callGraph = CallGraphBuilder(
                        context, moduleDFG,
                        externalModulesDFG,
                        context.devirtualizationAnalysisResult!!,
                        nonDevirtualizedCallSitesUnfoldFactor = Int.MAX_VALUE
                ).build()

                val rootSet = DevirtualizationAnalysis.computeRootSet(context, moduleDFG, externalModulesDFG)
                        .mapNotNull { it.irFunction }
                        .toSet()

                FileInitializersOptimization.removeRedundantCalls(context, callGraph, rootSet)
            }
    )

    fun buildEscapeAnalysisPhase(
            dfgPhase: NamedCompilerPhase<LtoContext, IrModuleFragment>,
            devirtualizationAnalysisPhase: NamedCompilerPhase<LtoContext, IrModuleFragment>,
    ) = makeKonanModuleOpPhase<LtoContext>(
            name = "EscapeAnalysis",
            description = "Escape analysis",
            prerequisite = setOf(dfgPhase, devirtualizationAnalysisPhase),
            op = { context, _ ->
                val entryPoint = context.ir.symbols.entryPoint?.owner
                val externalModulesDFG = ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())
                val nonDevirtualizedCallSitesUnfoldFactor =
                        if (entryPoint != null) {
                            // For a final program it can be safely assumed that what classes we see is what we got,
                            // so can take those. In theory we can always unfold call sites using type hierarchy, but
                            // the analysis might converge much, much slower, so take only reasonably small for now.
                            5
                        } else {
                            // Can't tolerate any non-devirtualized call site for a library.
                            // TODO: What about private virtual functions?
                            // Note: 0 is also bad - this means that there're no inheritors in the current source set,
                            // but there might be some provided by the users of the library being produced.
                            -1
                        }
                val callGraph = CallGraphBuilder(
                        context, context.moduleDFG!!,
                        externalModulesDFG,
                        context.devirtualizationAnalysisResult!!,
                        nonDevirtualizedCallSitesUnfoldFactor
                ).build()
                EscapeAnalysis.computeLifetimes(
                        context, context.moduleDFG!!, externalModulesDFG, callGraph, context.lifetimes
                )
            }
    )

    fun buildFunctionsWithoutBoundCheck(): NamedCompilerPhase<MiddleEndContext, Unit> = myLower2(
            name = "FunctionsWithoutBoundCheckGenerator",
            description = "Functions without bounds check generation",
            op = { context, _ ->
                FunctionsWithoutBoundCheckGenerator(context).generate()
            }
    )

    fun buildRemoveExpectDeclarationsPhase() = makeKonanFileLoweringPhase(
            ::ExpectDeclarationsRemoving,
            name = "RemoveExpectDeclarations",
            description = "Expect declarations removing"
    )

    fun buildForLoopsLowering() = makeKonanFileOpPhase(
            { context, irFile ->
                ForLoopsLowering(context, KonanBCEForLoopBodyTransformer()).lower(irFile)
            },
            name = "ForLoops",
            description = "For loops lowering",
            prerequisite = setOf() // setOf(functionsWithoutBoundCheck)
    )

    fun buildAllLoweringsPhase(): NamedCompilerPhase<MiddleEndContext, IrModuleFragment> {
        val removeExpectDeclarationsPhase = buildRemoveExpectDeclarationsPhase()
//        val forLoopsLowering = buildForLoopsLowering()
        return NamedCompilerPhase(
                name = "IrLowering",
                description = "IR Lowering",
                // TODO: The lowerings before inlinePhase should be aligned with [NativeInlineFunctionResolver.kt]
                lower = performByIrFile(
                        name = "IrLowerByFile",
                        description = "IR Lowering by file",
                        lower = listOf(
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
                                //                        Disabled for now because it leads to problems with Double.NaN and Float.NaN on macOS AArch 64.
                                //                        constantInliningPhase,
                                fileInitializersPhase,
                                bridgesPhase,
                                exportInternalAbiPhase,
                                useInternalAbiPhase,
                                autoboxPhase,
                        )
                ),
                actions = setOf(defaultDumper, ::moduleValidationCallback)
        )
    }

    fun buildObjectFilesPhase(bitcodeFile: BitcodeFile) = myLower2<ObjectFilesContext, Unit>(
            op = { ctx, _ ->
                ctx.compilerOutput = BitcodeCompiler(ctx.config, ctx as LoggingContext).makeObjectFiles(bitcodeFile)
            },
            name = "ObjectFiles",
            description = "Bitcode to object file"
    )

    fun buildLinkerPhase(objectFiles: List<ObjectFile>) = myLower2<LinkerContext, Unit>(
            op = { ctx, _ ->
                Linker(
                        ctx.necessaryLlvmParts,
                        ctx.llvmModuleSpecification,
                        ctx.coverage,
                        ctx.config,
                        ctx as PhaseContext,
                ).link(objectFiles)
            },
            name = "Linker",
            description = "Linker"
    )

    private fun <C : LoggingContext, Input, Output> myLower(op: (C, Input) -> Output) = object : CompilerPhase<C, Input, Output> {
        override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: C, input: Input): Output {
            return op(context, input)
        }
    }

    private fun <C : LoggingContext, Data> myLower2(
            op: (C, Data) -> Data,
            description: String,
            name: String,
            prerequisite: Set<NamedCompilerPhase<*, *>> = emptySet()
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

    fun getDcePhase(devirtualizationAnalysisPhase: NamedCompilerPhase<LtoContext, IrModuleFragment>) = makeKonanModuleOpPhase<LtoContext>(
            name = "DCEPhase",
            description = "Dead code elimination",
            prerequisite = setOf(devirtualizationAnalysisPhase),
            op = { context, _ ->
                val externalModulesDFG = ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())

                val callGraph = CallGraphBuilder(
                        context, context.moduleDFG!!,
                        externalModulesDFG,
                        context.devirtualizationAnalysisResult!!,
                        // For DCE we don't wanna miss any potentially reachable function.
                        nonDevirtualizedCallSitesUnfoldFactor = Int.MAX_VALUE
                ).build()

                val referencedFunctions = mutableSetOf<IrFunction>()
                callGraph.rootExternalFunctions.forEach {
                    if (!it.isTopLevelFieldInitializer)
                        referencedFunctions.add(it.irFunction ?: error("No IR for: $it"))
                }
                for (node in callGraph.directEdges.values) {
                    if (!node.symbol.isTopLevelFieldInitializer)
                        referencedFunctions.add(node.symbol.irFunction ?: error("No IR for: ${node.symbol}"))
                    node.callSites.forEach {
                        assert(!it.isVirtual) { "There should be no virtual calls in the call graph, but was: ${it.actualCallee}" }
                        referencedFunctions.add(it.actualCallee.irFunction ?: error("No IR for: ${it.actualCallee}"))
                    }
                }

                context.irModule!!.acceptChildrenVoid(object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    override fun visitFunction(declaration: IrFunction) {
                        // TODO: Generalize somehow, not that graceful.
                        if (declaration.name == OperatorNameConventions.INVOKE
                                && declaration.parent.let { it is IrClass && it.defaultType.isFunction() }) {
                            referencedFunctions.add(declaration)
                        }
                        super.visitFunction(declaration)
                    }

                    override fun visitConstructor(declaration: IrConstructor) {
                        // TODO: NativePointed is the only inline class for which the field's type and
                        //       the constructor parameter's type are different.
                        //       Thus we need to conserve the constructor no matter if it was actually referenced somehow or not.
                        //       See [IrTypeInlineClassesSupport.getInlinedClassUnderlyingType] why.
                        if (declaration.parentAsClass.name.asString() == InteropFqNames.nativePointedName && declaration.isPrimary)
                            referencedFunctions.add(declaration)
                        super.visitConstructor(declaration)
                    }
                })

                context.irModule!!.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitFile(declaration: IrFile): IrFile {
                        declaration.declarations.removeAll {
                            (it is IrFunction && !referencedFunctions.contains(it))
                        }
                        return super.visitFile(declaration)
                    }

                    override fun visitClass(declaration: IrClass): IrStatement {
                        if (declaration == context.ir.symbols.nativePointed)
                            return super.visitClass(declaration)
                        declaration.declarations.removeAll {
                            (it is IrFunction && it.isReal && !referencedFunctions.contains(it))
                        }
                        return super.visitClass(declaration)
                    }

                    override fun visitProperty(declaration: IrProperty): IrStatement {
                        if (declaration.getter.let { it != null && it.isReal && !referencedFunctions.contains(it) }) {
                            declaration.getter = null
                        }
                        if (declaration.setter.let { it != null && it.isReal && !referencedFunctions.contains(it) }) {
                            declaration.setter = null
                        }
                        return super.visitProperty(declaration)
                    }
                })

                context.referencedFunctions = referencedFunctions
            }
    )

    fun getDevirtualizationPhase(
            dfgPhase: NamedCompilerPhase<LtoContext, IrModuleFragment>,
            devirtualizationAnalysisPhase: NamedCompilerPhase<LtoContext, IrModuleFragment>,
    ) = makeKonanModuleOpPhase<LtoContext>(
            name = "Devirtualization",
            description = "Devirtualization",
            prerequisite = setOf(dfgPhase, devirtualizationAnalysisPhase),
            op = { context, irModule ->
                val devirtualizedCallSites =
                        context.devirtualizationAnalysisResult!!.devirtualizedCallSites
                                .asSequence()
                                .filter { it.key.irCallSite != null }
                                .associate { it.key.irCallSite!! to it.value }
                DevirtualizationAnalysis.devirtualize(irModule, context,
                        ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap()), devirtualizedCallSites)
            }
    )

    fun getPropertyAccessorInlinePhase() = makeKonanModuleLoweringPhase<BackendPhaseContext>(
            ::PropertyAccessorInlineLowering,
            name = "PropertyAccessorInline",
            description = "Property accessor inline lowering"
    )

    fun getInlineClassPropertyAccessorsPhase(): NamedCompilerPhase<KonanBackendContext, IrModuleFragment> = makeKonanModuleOpPhase<KonanBackendContext>(
            name = "InlineClassPropertyAccessorsLowering",
            description = "Inline class property accessors",
            op = { context, irModule -> irModule.files.forEach { InlineClassPropertyAccessorsLowering(context).lower(it) } }
    )

    fun getUnboxInlinePhase() = makeKonanModuleLoweringPhase<BitcodegenContext>(
            ::UnboxInlineLowering,
            name = "UnboxInline",
            description = "Unbox functions inline lowering",
            prerequisite = setOf()//setOf(autoboxPhase, redundantCoercionsCleaningPhase) // Different context types
    )

    fun getGhaPhase() = makeKonanModuleOpPhase<LtoContext>(
            name = "GHAPhase",
            description = "Global hierarchy analysis",
            op = { context, irModule -> GlobalHierarchyAnalysis(context, irModule).run() }
    )
}