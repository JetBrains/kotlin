package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.checkDeclarationParents
import org.jetbrains.kotlin.backend.common.IrValidator
import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.lower.ExpectToActualDefaultValueCopier
import org.jetbrains.kotlin.backend.konan.lower.SamSuperTypesChecker
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

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
        prerequisite: Set<AnyNamedPhase> = emptySet(),
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
            objCExport = ObjCExport(this, symbolTable!!)
        },
        name = "ObjCExport",
        description = "Objective-C header generation",
        prerequisite = setOf(createSymbolTablePhase)
)

internal val buildCExportsPhase = konanUnitPhase(
        op = {
            if (this.isNativeLibrary) {
                this.cAdapterGenerator = CAdapterGenerator(this).also {
                    it.buildExports(this.symbolTable!!)
                }
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
                val moduleDeserializer = irLinker.nonCachedLibraryModuleDeserializers[module.descriptor]
                if (moduleDeserializer == null) {
                    require(module.descriptor.isFromInteropLibrary()) { "No module deserializer for ${module.descriptor}" }
                } else {
                    val compatibleMode = CompatibilityMode(moduleDeserializer.libraryAbiVersion).oldSignatures
                    module.acceptChildrenVoid(object : IrElementVisitorVoid {
                        override fun visitElement(element: IrElement) {
                            element.acceptChildrenVoid(this)
                        }

                        override fun visitClass(declaration: IrClass) {
                            declaration.acceptChildrenVoid(this)

                            if (!declaration.isInterface && declaration.visibility != DescriptorVisibilities.LOCAL
                                    && declaration.isExported && declaration.origin != DECLARATION_ORIGIN_FUNCTION_CLASS)
                                classFields.add(moduleDeserializer.buildClassFields(declaration, getLayoutBuilder(declaration).getDeclaredFields()))
                        }

                        override fun visitFunction(declaration: IrFunction) {
                            declaration.acceptChildrenVoid(this)

                            if (declaration.isFakeOverride || !declaration.isExportedInlineFunction) return
                            inlineFunctionBodies.add(moduleDeserializer.buildInlineFunctionReference(declaration))
                        }

                        private val IrClass.isExported
                            get() = with(KonanManglerIr) { isExported(compatibleMode) }

                        private val IrFunction.isExportedInlineFunction
                            get() = isInline && with(KonanManglerIr) { isExported(compatibleMode) }
                    })
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

internal val allLoweringsPhase = NamedCompilerPhase(
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
                        fileInitializersPhase,
                        bridgesPhase,
                        autoboxPhase,
                )
        ),
        actions = setOf(defaultDumper, ::moduleValidationCallback)
)

internal val dependenciesLowerPhase = NamedCompilerPhase(
        name = "LowerLibIR",
        description = "Lower library's IR",
        prerequisite = emptySet(),
        lower = object : CompilerPhase<Context, IrModuleFragment, IrModuleFragment> {
            override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment): IrModuleFragment {
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

internal val dumpTestsPhase = makeCustomPhase<Context, IrModuleFragment>(
        name = "dumpTestsPhase",
        description = "Dump the list of all available tests",
        op = { context, _ ->
            val testDumpFile = context.config.testDumpFile
            requireNotNull(testDumpFile)

            if (context.testCasesToDump.isEmpty()) {
                testDumpFile.writeText("")
                return@makeCustomPhase
            }

            testDumpFile.writeText(
                    context.testCasesToDump.asSequence()
                            .flatMap { (suiteClassId, functionNames) ->
                                val suiteName = suiteClassId.asString()
                                functionNames.asSequence().map { "$suiteName:$it" }
                            }
                            .sorted()
                            .joinToString(separator = "\n")
            )
        }
)

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
                context.irModule!!.addFile(NaiveSourceBasedFileEntryImpl("entryPointOwner"), FqName("kotlin.native.caches.abi"))
            }

            file.addChild(makeEntryPoint(context))
        }
)

internal val exportInternalAbiPhase = makeKonanModuleOpPhase(
        name = "exportInternalAbi",
        description = "Add accessors to private entities",
        prerequisite = emptySet(),
        op = { context, module ->
            val visitor = object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitClass(declaration: IrClass) {
                    declaration.acceptChildrenVoid(this)
                    if (declaration.isCompanion) {
                        val function = context.irFactory.buildFun {
                            name = InternalAbi.getCompanionObjectAccessorName(declaration)
                            origin = InternalAbi.INTERNAL_ABI_ORIGIN
                            returnType = declaration.defaultType
                        }
                        context.createIrBuilder(function.symbol).apply {
                            function.body = irBlockBody {
                                +irReturn(irGetObjectValue(declaration.defaultType, declaration.symbol))
                            }
                        }
                        context.internalAbi.declare(function, declaration.module)
                    }

                    if (declaration.isInner) {
                        val function = context.irFactory.buildFun {
                            name = InternalAbi.getInnerClassOuterThisAccessorName(declaration)
                            origin = InternalAbi.INTERNAL_ABI_ORIGIN
                            returnType = declaration.parentAsClass.defaultType
                        }
                        function.addValueParameter {
                            name = Name.identifier("innerClass")
                            origin = InternalAbi.INTERNAL_ABI_ORIGIN
                            type = declaration.defaultType
                        }

                        context.createIrBuilder(function.symbol).apply {
                            function.body = irBlockBody {
                                +irReturn(irGetField(
                                        irGet(function.valueParameters[0]),
                                        context.specialDeclarationsFactory.getOuterThisField(declaration))
                                )
                            }
                        }
                        context.internalAbi.declare(function, declaration.module)
                    }
                }
            }
            module.acceptChildrenVoid(visitor)
        }
)

internal val useInternalAbiPhase = makeKonanModuleOpPhase(
        name = "useInternalAbi",
        description = "Use internal ABI functions to access private entities",
        prerequisite = emptySet(),
        op = { context, module ->
            val companionObjectAccessors = mutableMapOf<IrClass, IrSimpleFunction>()
            val outerThisAccessors = mutableMapOf<IrClass, IrSimpleFunction>()
            val transformer = object : IrElementTransformerVoid() {
                override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
                    val irClass = expression.symbol.owner
                    if (!irClass.isCompanion || context.llvmModuleSpecification.containsDeclaration(irClass)) {
                        return expression
                    }
                    val parent = irClass.parentAsClass
                    if (parent.isObjCClass()) {
                        // Access to Obj-C metaclass is done via intrinsic.
                        return expression
                    }
                    val accessor = companionObjectAccessors.getOrPut(irClass) {
                        context.irFactory.buildFun {
                            name = InternalAbi.getCompanionObjectAccessorName(irClass)
                            returnType = irClass.defaultType
                            origin = InternalAbi.INTERNAL_ABI_ORIGIN
                            isExternal = true
                        }.also {
                            context.internalAbi.reference(it, irClass.module)
                        }
                    }
                    return IrCallImpl(expression.startOffset, expression.endOffset, expression.type, accessor.symbol, accessor.typeParameters.size, accessor.valueParameters.size)
                }

                override fun visitGetField(expression: IrGetField): IrExpression {
                    val field = expression.symbol.owner
                    val irClass = field.parentClassOrNull ?: return expression
                    if (!irClass.isInner || context.llvmModuleSpecification.containsDeclaration(irClass)
                            || context.specialDeclarationsFactory.getOuterThisField(irClass) != field
                    ) {
                        return expression
                    }
                    val accessor = outerThisAccessors.getOrPut(irClass) {
                        context.irFactory.buildFun {
                            name = InternalAbi.getInnerClassOuterThisAccessorName(irClass)
                            returnType = irClass.parentAsClass.defaultType
                            origin = InternalAbi.INTERNAL_ABI_ORIGIN
                            isExternal = true
                        }.also { function ->
                            context.internalAbi.reference(function, irClass.module)

                            function.addValueParameter {
                                name = Name.identifier("innerClass")
                                origin = InternalAbi.INTERNAL_ABI_ORIGIN
                                type = irClass.defaultType
                            }
                        }
                    }
                    return IrCallImpl(
                            expression.startOffset, expression.endOffset,
                            expression.type, accessor.symbol,
                            accessor.typeParameters.size, accessor.valueParameters.size
                    ).apply {
                        putValueArgument(0, expression.receiver)
                    }
                }
            }
            module.transformChildrenVoid(transformer)
        }
)

internal val bitcodePhase = NamedCompilerPhase(
        name = "Bitcode",
        description = "LLVM Bitcode generation",
        lower = contextLLVMSetupPhase then
                returnsInsertionPhase then
                buildDFGPhase then
                devirtualizationAnalysisPhase then
                dcePhase then
                removeRedundantCallsToFileInitializersPhase then
                devirtualizationPhase then
                propertyAccessorInlinePhase then // Have to run after link dependencies phase, because fields
                                                 // from dependencies can be changed during lowerings.
                inlineClassPropertyAccessorsPhase then
                redundantCoercionsCleaningPhase then
                createLLVMDeclarationsPhase then
                ghaPhase then
                RTTIPhase then
                generateDebugInfoHeaderPhase then
                escapeAnalysisPhase then
                localEscapeAnalysisPhase then
                codegenPhase then
                finalizeDebugInfoPhase then
                cStubsPhase
)

private val bitcodePostprocessingPhase = NamedCompilerPhase(
        name = "BitcodePostprocessing",
        description = "Optimize and rewrite bitcode",
        lower = checkExternalCallsPhase then
                bitcodeOptimizationPhase then
                coveragePhase then
                optimizeTLSDataLoadsPhase then
                rewriteExternalCallsCheckerGlobals
)

private val backendCodegen = namedUnitPhase(
        name = "Backend codegen",
        description = "Backend code generation",
        lower = takeFromContext<Context, Unit, IrModuleFragment> { it.irModule!! } then
                functionsWithoutBoundCheck then
                allLoweringsPhase then // Lower current module first.
                dependenciesLowerPhase then // Then lower all libraries in topological order.
                                            // With that we guarantee that inline functions are unlowered while being inlined.
                dumpTestsPhase then
                entryPointPhase then
                exportInternalAbiPhase then
                useInternalAbiPhase then
                bitcodePhase then
                verifyBitcodePhase then
                printBitcodePhase then
                linkBitcodeDependenciesPhase then
                bitcodePostprocessingPhase then
                unitSink()
)

// Have to hide Context as type parameter in order to expose toplevelPhase outside of this module.
val toplevelPhase: CompilerPhase<*, Unit, Unit> = namedUnitPhase(
        name = "Compiler",
        description = "The whole compilation process",
        lower = createSymbolTablePhase then
                objCExportPhase then
                buildCExportsPhase then
                psiToIrPhase then
                buildAdditionalCacheInfoPhase then
                destroySymbolTablePhase then
                copyDefaultValuesToActualPhase then
                checkSamSuperTypesPhase then
                serializerPhase then
                specialBackendChecksPhase then
                namedUnitPhase(
                        name = "Backend",
                        description = "All backend",
                        lower = backendCodegen then
                                produceOutputPhase then
                                disposeLLVMPhase then
                                unitSink()
                ) then
                objectFilesPhase then
                linkerPhase
)

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

        disable(localEscapeAnalysisPhase)

        // Don't serialize anything to a final executable.
        disableUnless(serializerPhase, config.produce == CompilerOutputKind.LIBRARY)
        disableUnless(entryPointPhase, config.produce == CompilerOutputKind.PROGRAM)
        disableUnless(buildAdditionalCacheInfoPhase, config.produce.isCache && config.lazyIrForCaches)
        disableUnless(exportInternalAbiPhase, config.produce.isCache)
        disableIf(backendCodegen, config.produce == CompilerOutputKind.LIBRARY)
        disableUnless(bitcodePostprocessingPhase, config.produce.involvesLinkStage)
        disableUnless(linkBitcodeDependenciesPhase, config.produce.involvesLinkStage)
        disableUnless(checkExternalCallsPhase, getBoolean(KonanConfigKeys.CHECK_EXTERNAL_CALLS))
        disableUnless(rewriteExternalCallsCheckerGlobals, getBoolean(KonanConfigKeys.CHECK_EXTERNAL_CALLS))
        disableUnless(optimizeTLSDataLoadsPhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(objectFilesPhase, config.produce.involvesLinkStage)
        disableUnless(linkerPhase, config.produce.involvesLinkStage)
        disableIf(testProcessorPhase, getNotNull(KonanConfigKeys.GENERATE_TEST_RUNNER) == TestRunnerKind.NONE)
        disableIf(dumpTestsPhase, getNotNull(KonanConfigKeys.GENERATE_TEST_RUNNER) == TestRunnerKind.NONE || config.testDumpFile == null)
        disableUnless(buildDFGPhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(devirtualizationAnalysisPhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(devirtualizationPhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(escapeAnalysisPhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        // Inline accessors only in optimized builds due to separate compilation and possibility to get broken
        // debug information.
        disableUnless(propertyAccessorInlinePhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(inlineClassPropertyAccessorsPhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(dcePhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(removeRedundantCallsToFileInitializersPhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(ghaPhase, getBoolean(KonanConfigKeys.OPTIMIZATION))
        disableUnless(verifyBitcodePhase, config.needCompilerVerification || getBoolean(KonanConfigKeys.VERIFY_BITCODE))

        disableUnless(fileInitializersPhase, getBoolean(KonanConfigKeys.PROPERTY_LAZY_INITIALIZATION))
        disableUnless(removeRedundantCallsToFileInitializersPhase, getBoolean(KonanConfigKeys.PROPERTY_LAZY_INITIALIZATION))

        val isDescriptorsOnlyLibrary = config.metadataKlib == true
        disableIf(psiToIrPhase, isDescriptorsOnlyLibrary)
        disableIf(destroySymbolTablePhase, isDescriptorsOnlyLibrary)
        disableIf(copyDefaultValuesToActualPhase, isDescriptorsOnlyLibrary)
        disableIf(specialBackendChecksPhase, isDescriptorsOnlyLibrary)
        disableIf(checkSamSuperTypesPhase, isDescriptorsOnlyLibrary)
    }
}
