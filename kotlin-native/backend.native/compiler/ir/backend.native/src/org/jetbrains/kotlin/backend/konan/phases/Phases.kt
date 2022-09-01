/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.phases

import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.ir.FunctionsWithoutBoundCheckGenerator
import org.jetbrains.kotlin.backend.konan.lower.ExpectToActualDefaultValueCopier
import org.jetbrains.kotlin.backend.konan.lower.SpecialBackendChecksTraversal
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.name.FqName

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
                context.objCExport = ObjCExport(context, context, context.symbolTable!!, context.config)
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
            name = "SpecialBackendChecks",
            description = "Special backend checks"
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

    fun buildWriteLlvmModule() = NamedCompilerPhase<LlvmCodegenContext, Unit> = myLower2(
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

    fun buildFunctionsWithoutBoundCheck(): NamedCompilerPhase<MiddleEndContext, Unit> = myLower2(
            name = "FunctionsWithoutBoundCheckGenerator",
            description = "Functions without bounds check generation",
            op = { context, _ ->
                FunctionsWithoutBoundCheckGenerator(context).generate()
            }
    )

    fun buildAllLoweringsPhase(): NamedCompilerPhase<MiddleEndContext, IrModuleFragment> = NamedCompilerPhase(
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

    fun buildObjectFilesPhase(bitcodeFile: BitcodeFile) = myLower2<ObjectFilesContext, Unit>(
            op = { ctx, _ ->
                ctx.compilerOutput = BitcodeCompiler(ctx.config, ctx as LoggingContext).makeObjectFiles(bitcodeFile)
            },
            name = "ObjectFiles",
            description = "Bitcode to object file"
    )

    fun buildLinkerPhase(objectFiles: List<ObjectFile>) = myLower2<ObjectFilesContext, Unit>(
            op = { ctx, _ -> Linker(
                    ctx.necessaryLlvmParts,
                    ctx.llvmModuleSpecification,
                    ctx.coverage,
                    ctx.config,
                    ctx as LoggingContext,
                    ctx as ErrorReportingContext
            ).link(objectFiles) },
            name = "Linker",
            description = "Linker"
    )

    private fun <C : CommonBackendContext, Input, Output> myLower(op: (C, Input) -> Output) = object : CompilerPhase<C, Input, Output> {
        override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: C, input: Input): Output {
            return op(context, input)
        }
    }

    private fun <C : CommonBackendContext, Data> myLower2(
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
}