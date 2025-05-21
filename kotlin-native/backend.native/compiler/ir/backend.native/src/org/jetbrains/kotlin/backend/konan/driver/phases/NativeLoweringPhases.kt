/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.ir.isReifiable
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.backend.common.lower.optimizations.LivenessAnalysis
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.util.isReifiedTypeParameter
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.ir.FunctionsWithoutBoundCheckGenerator
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.InitializersLowering
import org.jetbrains.kotlin.backend.konan.optimizations.NativeForLoopsLowering
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.inline.*
import org.jetbrains.kotlin.backend.konan.lower.NativeAssertionWrapperLowering
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration

internal typealias LoweringList = List<NamedCompilerPhase<NativeGenerationState, IrFile, IrFile>>
internal typealias ModuleLowering = NamedCompilerPhase<NativeGenerationState, IrModuleFragment, Unit>

internal fun PhaseEngine<NativeGenerationState>.runLowerings(lowerings: LoweringList, modules: List<IrModuleFragment>) {
    for (module in modules) {
        for (file in module.files) {
            context.fileLowerState = FileLowerState()
            lowerings.fold(file) { loweredFile, lowering ->
                runPhase(lowering, loweredFile)
            }
        }
    }
}

internal fun PhaseEngine<NativeGenerationState>.runModuleWisePhase(
        lowering: ModuleLowering,
        modules: List<IrModuleFragment>
) {
    for (module in modules) {
        runPhase(lowering, module)
    }
}

internal val validateIrBeforeLowering = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "ValidateIrBeforeLowering",
        op = { context, module -> IrValidationBeforeLoweringPhase(context.context).lower(module) }
)

internal val validateIrAfterInliningOnlyPrivateFunctions = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "ValidateIrAfterInliningOnlyPrivateFunctions",
        op = { context, module ->
            IrValidationAfterInliningOnlyPrivateFunctionsPhase(
                    context = context.context,
                    checkInlineFunctionCallSites = { inlineFunctionUseSite ->
                        val inlineFunction = inlineFunctionUseSite.symbol.owner
                        when {
                            // TODO: remove this condition after the fix of KT-69457:
                            inlineFunctionUseSite is IrFunctionReference && !inlineFunction.isReifiable() -> true // temporarily permitted

                            // Call sites of non-private functions are allowed at this stage.
                            else -> !inlineFunctionUseSite.symbol.isConsideredAsPrivateForInlining()
                        }
                    }
            ).lower(module)
        }
)

internal val validateIrAfterInliningAllFunctions = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "ValidateIrAfterInliningAllFunctions",
        op = { context, module ->
            IrValidationAfterInliningAllFunctionsPhase(
                    context = context.context,
                    checkInlineFunctionCallSites = { inlineFunctionUseSite ->
                        // No inline function call sites should remain at this stage.
                        val inlineFunction = inlineFunctionUseSite.symbol.owner
                        when {
                            // TODO: remove this condition after the fix of KT-66734:
                            inlineFunction.isExternal -> true // temporarily permitted

                            // TODO: remove this condition after the fix of KT-69457:
                            inlineFunctionUseSite is IrFunctionReference && !inlineFunction.isReifiable() -> true // temporarily permitted

                            // it's fine to have typeOf<T> with reified T, it would be correctly handled by inliner on inlining to next use-sites.
                            // maybe it should be replaced by separate node to avoid this special case and simplify detection code - KT-70360
                            Symbols.isTypeOfIntrinsic(inlineFunction.symbol) && inlineFunctionUseSite.typeArguments[0]?.isReifiedTypeParameter == true -> true

                            else -> false // forbidden
                        }
                    }
            ).lower(module)
        }
)

internal val validateIrAfterLowering = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "ValidateIrAfterLowering",
        op = { context, module -> IrValidationAfterLoweringPhase(context.context).lower(module) }
)

internal val functionsWithoutBoundCheck = createSimpleNamedCompilerPhase<Context, Unit>(
        name = "FunctionsWithoutBoundCheckGenerator",
        op = { context, _ -> FunctionsWithoutBoundCheckGenerator(context).generate() }
)

private val removeExpectDeclarationsPhase = createFileLoweringPhase(
        ::ExpectDeclarationsRemoveLowering,
        name = "RemoveExpectDeclarations",
)

private val stripTypeAliasDeclarationsPhase = createFileLoweringPhase(
        { _: Context -> StripTypeAliasDeclarationsLowering() },
        name = "StripTypeAliasDeclarations",
)

private val annotationImplementationPhase = createFileLoweringPhase(
        ::NativeAnnotationImplementationLowering,
        name = "AnnotationImplementation",
)


private val upgradeCallableReferencesPhase = createFileLoweringPhase(
        ::UpgradeCallableReferences,
        name = "UpgradeCallableReferences",
)

private val arrayConstructorPhase = createFileLoweringPhase(
        ::ArrayConstructorLowering,
        name = "ArrayConstructor",
        prerequisite = setOf(upgradeCallableReferencesPhase)
)

private val lateinitPhase = createFileLoweringPhase(
        ::LateinitLowering,
        name = "Lateinit",
)

private val sharedVariablesPhase = createFileLoweringPhase(
        ::SharedVariablesLowering,
        name = "SharedVariables",
        prerequisite = setOf(lateinitPhase)
)

private val extractLocalClassesFromInlineBodies = createFileLoweringPhase(
        ::LocalClassesInInlineLambdasLowering,
        name = "ExtractLocalClassesFromInlineBodies",
        prerequisite = setOf(sharedVariablesPhase),
)

private val postInlinePhase = createFileLoweringPhase(
        { context: Context -> PostInlineLowering(context) },
        name = "PostInline",
)

private val contractsDslRemovePhase = createFileLoweringPhase(
        { context: Context -> ContractsDslRemover(context) },
        name = "RemoveContractsDsl",
)

private val flattenStringConcatenationPhase = createFileLoweringPhase(
        ::FlattenStringConcatenationLowering,
        name = "FlattenStringConcatenationLowering",
)

private val stringConcatenationPhase = createFileLoweringPhase(
        ::StringConcatenationLowering,
        name = "StringConcatenation",
)

private val stringConcatenationTypeNarrowingPhase = createFileLoweringPhase(
        ::StringConcatenationTypeNarrowing,
        name = "StringConcatenationTypeNarrowing",
        prerequisite = setOf(stringConcatenationPhase)
)

private val kotlinNothingValueExceptionPhase = createFileLoweringPhase(
        ::KotlinNothingValueExceptionLowering,
        name = "KotlinNothingValueException",
)

private val enumConstructorsPhase = createFileLoweringPhase(
        ::EnumConstructorsLowering,
        name = "EnumConstructors",
)

private val initializersPhase = createFileLoweringPhase(
        ::InitializersLowering,
        name = "Initializers",
        prerequisite = setOf(enumConstructorsPhase)
)

private val localFunctionsPhase = createFileLoweringPhase(
        op = { context, irFile ->
            LocalDelegatedPropertiesLowering().lower(irFile)
            LocalDeclarationsLowering(context).lower(irFile)
            LocalClassPopupLowering(context).lower(irFile)
        },
        name = "LocalFunctions",
        prerequisite = setOf(sharedVariablesPhase) // TODO: add "soft" dependency on inventNamesForLocalClasses
)

private val tailrecPhase = createFileLoweringPhase(
        ::TailrecLowering,
        name = "Tailrec",
        prerequisite = setOf(localFunctionsPhase)
)

private val volatilePhase = createFileLoweringPhase(
        ::VolatileFieldsLowering,
        name = "VolatileFields",
        prerequisite = setOf()
)

private val defaultParameterExtentPhase = createFileLoweringPhase(
        { context, irFile ->
            NativeDefaultArgumentStubGenerator(context).lower(irFile)
            DefaultParameterCleaner(context, replaceDefaultValuesWithStubs = true).lower(irFile)
            NativeDefaultParameterInjector(context).lower(irFile)
        },
        name = "DefaultParameterExtent",
        prerequisite = setOf(tailrecPhase, enumConstructorsPhase)
)

private val innerClassPhase = createFileLoweringPhase(
        ::InnerClassLowering,
        name = "InnerClasses",
        prerequisite = setOf(defaultParameterExtentPhase)
)

private val rangeContainsLoweringPhase = createFileLoweringPhase(
        ::RangeContainsLowering,
        name = "RangeContains",
)

private val forLoopsPhase = createFileLoweringPhase(
        ::NativeForLoopsLowering,
        name = "ForLoops",
        prerequisite = setOf(functionsWithoutBoundCheck)
)

private val dataClassesPhase = createFileLoweringPhase(
        ::DataClassOperatorsLowering,
        name = "DataClasses",
)

private val finallyBlocksPhase = createFileLoweringPhase(
        { context, irFile -> FinallyBlocksLowering(context, context.irBuiltIns.throwableType).lower(irFile) },
        name = "FinallyBlocks",
        prerequisite = setOf(initializersPhase, localFunctionsPhase, tailrecPhase)
)

private val testProcessorPhase = createFileLoweringPhase(
        lowering = ::TestProcessor,
        name = "TestProcessor",
)

private val dumpTestsPhase = createFileLoweringPhase(
        lowering = ::TestsDumper,
        name = "TestsDumper",
)

private val delegatedPropertyOptimizationPhase = createFileLoweringPhase(
        lowering = ::DelegatedPropertyOptimizationLowering,
        name = "DelegatedPropertyOptimization",
        prerequisite = setOf()
)

private val propertyReferencePhase = createFileLoweringPhase(
        lowering = ::PropertyReferenceLowering,
        name = "PropertyReference",
        prerequisite = setOf(volatilePhase, delegatedPropertyOptimizationPhase)
)


private val functionReferencePhase = createFileLoweringPhase(
        lowering = ::NativeFunctionReferenceLowering,
        name = "FunctionReference",
)

private val staticCallableReferenceOptimizationPhase = createFileLoweringPhase(
        lowering = ::StaticCallableReferenceOptimization,
        name = "StaticCallableReferenceOptimization",
        prerequisite = setOf(functionReferencePhase, propertyReferencePhase)
)

private val enumWhenPhase = createFileLoweringPhase(
        ::NativeEnumWhenLowering,
        name = "EnumWhen",
        prerequisite = setOf(enumConstructorsPhase, functionReferencePhase)
)

private val enumClassPhase = createFileLoweringPhase(
        ::EnumClassLowering,
        name = "Enums",
        prerequisite = setOf(enumConstructorsPhase, functionReferencePhase, enumWhenPhase) // TODO: make weak dependency on `testProcessorPhase`
)

private val enumUsagePhase = createFileLoweringPhase(
        ::EnumUsageLowering,
        name = "EnumUsage",
        prerequisite = setOf(enumConstructorsPhase, functionReferencePhase, enumClassPhase)
)


private val singleAbstractMethodPhase = createFileLoweringPhase(
        ::NativeSingleAbstractMethodLowering,
        name = "SingleAbstractMethod",
        prerequisite = setOf(functionReferencePhase)
)

private val builtinOperatorPhase = createFileLoweringPhase(
        ::BuiltinOperatorLowering,
        name = "BuiltinOperators",
        prerequisite = setOf(defaultParameterExtentPhase, singleAbstractMethodPhase, enumWhenPhase)
)

/**
 * The first phase of inlining (inline only private functions).
 */
private val inlineOnlyPrivateFunctionsPhase = createFileLoweringPhase(
        lowering = { generationState: NativeGenerationState ->
            NativeIrInliner(generationState, inlineMode = InlineMode.PRIVATE_INLINE_FUNCTIONS)
        },
        name = "InlineOnlyPrivateFunctions",
)

private val outerThisSpecialAccessorInInlineFunctionsPhase = createFileLoweringPhase(
        ::OuterThisInInlineFunctionsSpecialAccessorLowering,
        name = "OuterThisInInlineFunctionsSpecialAccessorLowering",
        prerequisite = setOf(inlineOnlyPrivateFunctionsPhase)
)

private val syntheticAccessorGenerationPhase = createFileLoweringPhase(
        lowering = ::SyntheticAccessorLowering,
        name = "SyntheticAccessorGeneration",
        prerequisite = setOf(outerThisSpecialAccessorInInlineFunctionsPhase),
)

/**
 * The second phase of inlining (inline all functions).
 */
internal val inlineAllFunctionsPhase = createFileLoweringPhase(
        lowering = { generationState: NativeGenerationState ->
            NativeIrInliner(generationState, inlineMode = InlineMode.ALL_INLINE_FUNCTIONS)
        },
        name = "InlineAllFunctions",
)

private val interopPhase = createFileLoweringPhase(
        lowering = ::InteropLowering,
        name = "Interop",
)

private val specialInteropIntrinsicsPhase = createFileLoweringPhase(
        lowering = ::SpecialInteropIntrinsicsLowering,
        name = "SpecialInteropIntrinsics",
        prerequisite = setOf(inlineAllFunctionsPhase)
)

internal val specialObjCValidationPhase = createFileLoweringPhase(
        lowering = ::SpecialObjCValidationLowering,
        name = "SpecialObjCValidation",
        prerequisite = setOf(inlineAllFunctionsPhase)
)

private val varargPhase = createFileLoweringPhase(
        ::VarargInjectionLowering,
        name = "Vararg",
        prerequisite = setOf(functionReferencePhase, defaultParameterExtentPhase, interopPhase, functionsWithoutBoundCheck)
)

private val coroutinesPhase = createFileLoweringPhase(
        lowering = { context: NativeGenerationState ->
            object : FileLoweringPass {
                override fun lower(irFile: IrFile) {
                    NativeSuspendFunctionsLowering(context).lower(irFile)
                    AddContinuationToNonLocalSuspendFunctionsLowering(context.context).lower(irFile)
                    NativeAddContinuationToFunctionCallsLowering(context.context).lower(irFile)
                    AddFunctionSupertypeToSuspendFunctionLowering(context.context).lower(irFile)
                }
            }
        },
        name = "Coroutines",
        prerequisite = setOf(localFunctionsPhase, finallyBlocksPhase, kotlinNothingValueExceptionPhase)
)

private val coroutinesLivenessAnalysisFallbackPhase = createFileLoweringPhase(
        lowering = ::CoroutinesLivenessAnalysisFallback,
        name = "CoroutinesLivenessAnalysisFallback",
        prerequisite = setOf(coroutinesPhase)
)

private val coroutinesLivenessAnalysisPhase = createFileLoweringPhase(
        lowering = { context: NativeGenerationState ->
            object : BodyLoweringPass {
                override fun lower(irBody: IrBody, container: IrDeclaration) {
                    LivenessAnalysis.run(irBody) { it is IrSuspensionPoint }
                            .forEach { (irElement, liveVariables) ->
                                (irElement as IrSuspensionPoint).liveVariablesAtSuspensionPoint = liveVariables
                            }
                    context.coroutinesLivenessAnalysisPhasePerformed = true
                }
            }
        },
        name = "CoroutinesLivenessAnalysis",
        prerequisite = setOf(coroutinesPhase)
)

internal val CoroutinesVarSpillingPhase = createFileLoweringPhase(
        lowering = ::CoroutinesVarSpillingLowering,
        name = "CoroutinesVarSpilling",
        prerequisite = setOf(coroutinesPhase)
)

private val typeOperatorPhase = createFileLoweringPhase(
        ::TypeOperatorLowering,
        name = "TypeOperators",
        prerequisite = setOf(coroutinesPhase)
)

private val bridgesPhase = createFileLoweringPhase(
        { context, irFile ->
            BridgesBuilding(context).runOnFilePostfix(irFile)
            WorkersBridgesBuilding(context).lower(irFile)
        },
        name = "Bridges",
        prerequisite = setOf(coroutinesPhase)
)

private val eraseGenericCallsReturnTypesPhase = createFileLoweringPhase(
        name = "EraseGenericCallsReturnTypesPhase",
        lowering = ::GenericCallsReturnTypeEraser,
)

private val autoboxPhase = createFileLoweringPhase(
        ::Autoboxing,
        name = "Autobox",
        prerequisite = setOf(bridgesPhase, coroutinesPhase, eraseGenericCallsReturnTypesPhase)
)

private val constructorsLoweringPhase = createFileLoweringPhase(
    name = "ConstructorsLowering",
    lowering = ::ConstructorsLowering,
)

private val expressionBodyTransformPhase = createFileLoweringPhase(
        ::ExpressionBodyTransformer,
        name = "ExpressionBodyTransformer",
)

private val staticInitializersPhase = createFileLoweringPhase(
        ::StaticInitializersLowering,
        name = "StaticInitializers",
        prerequisite = setOf(expressionBodyTransformPhase)
)

private val ifNullExpressionsFusionPhase = createFileLoweringPhase(
        ::IfNullExpressionsFusionLowering,
        name = "IfNullExpressionsFusionLowering",
)

private val exportInternalAbiPhase = createFileLoweringPhase(
        ::ExportCachesAbiVisitor,
        name = "ExportInternalAbi",
)

internal val ReturnsInsertionPhase = createFileLoweringPhase(
        name = "ReturnsInsertion",
        prerequisite = setOf(autoboxPhase, coroutinesPhase, enumClassPhase),
        lowering = ::ReturnsInsertionLowering,
)

internal val InlineClassPropertyAccessorsPhase = createFileLoweringPhase(
        name = "InlineClassPropertyAccessorsLowering",
        lowering = ::InlineClassPropertyAccessorsLowering,
)

internal val RedundantCoercionsCleaningPhase = createFileLoweringPhase(
        name = "RedundantCoercionsCleaning",
        lowering = ::RedundantCoercionsCleaner,
)

internal val PropertyAccessorInlinePhase = createFileLoweringPhase(
        name = "PropertyAccessorInline",
        lowering = ::PropertyAccessorInlineLowering,
)

internal val UnboxInlinePhase = createFileLoweringPhase(
        name = "UnboxInline",
        lowering = ::UnboxInlineLowering,
)

private val inventNamesForLocalClasses = createFileLoweringPhase(
        lowering = ::NativeInventNamesForLocalClasses,
        name = "InventNamesForLocalClasses",
)

private val useInternalAbiPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile, IrFile>(
        name = "UseInternalAbi",
        outputIfNotEnabled = { _, _, _, irFile -> irFile },
) { context, file ->
    ImportCachesAbiTransformer(context).lower(file)
    file
}


private val objectClassesPhase = createFileLoweringPhase(
        lowering = ::ObjectClassLowering,
        name = "ObjectClasses",
)

private val assertionWrapperPhase = createFileLoweringPhase(
        lowering = ::NativeAssertionWrapperLowering,
        name = "AssertionWrapperLowering",
)

private val assertionRemoverPhase = createFileLoweringPhase(
        lowering = ::NativeAssertionRemoverLowering,
        name = "AssertionRemoverLowering",
        prerequisite = setOf(assertionWrapperPhase),
)

internal val constEvaluationPhase = createFileLoweringPhase(
        lowering = { context: Context ->
            val configuration = IrInterpreterConfiguration(printOnlyExceptionMessage = true)
            ConstEvaluationLowering(context, configuration = configuration)
        },
        name = "ConstEvaluationLowering",
        prerequisite = setOf(inlineAllFunctionsPhase)
)

internal fun getLoweringsUpToAndIncludingSyntheticAccessors(): LoweringList = listOfNotNull(
        testProcessorPhase,
        upgradeCallableReferencesPhase,
        assertionWrapperPhase,
        lateinitPhase,
        sharedVariablesPhase,
        extractLocalClassesFromInlineBodies,
        arrayConstructorPhase,
        inlineOnlyPrivateFunctionsPhase,
        outerThisSpecialAccessorInInlineFunctionsPhase,
        syntheticAccessorGenerationPhase,
)

internal fun KonanConfig.getLoweringsAfterInlining(): LoweringList = listOfNotNull(
        interopPhase,
        specialInteropIntrinsicsPhase,
        dumpTestsPhase.takeIf { this.configuration.getNotNull(KonanConfigKeys.GENERATE_TEST_RUNNER) != TestRunnerKind.NONE },
        removeExpectDeclarationsPhase,
        stripTypeAliasDeclarationsPhase,
        assertionRemoverPhase,
        volatilePhase,
        delegatedPropertyOptimizationPhase,
        propertyReferencePhase,
        functionReferencePhase,
        postInlinePhase,
        contractsDslRemovePhase,
        annotationImplementationPhase,
        rangeContainsLoweringPhase,
        forLoopsPhase,
        flattenStringConcatenationPhase,
        stringConcatenationPhase,
        stringConcatenationTypeNarrowingPhase.takeIf { this.optimizationsEnabled },
        enumConstructorsPhase,
        initializersPhase,
        inventNamesForLocalClasses,
        localFunctionsPhase,
        tailrecPhase,
        defaultParameterExtentPhase,
        innerClassPhase,
        dataClassesPhase,
        ifNullExpressionsFusionPhase,
        staticCallableReferenceOptimizationPhase,
        singleAbstractMethodPhase,
        enumWhenPhase,
        finallyBlocksPhase,
        enumClassPhase,
        enumUsagePhase,
        varargPhase,
        kotlinNothingValueExceptionPhase,
        coroutinesPhase,
        // Either of these could be turned off without losing correctness.
        coroutinesLivenessAnalysisPhase, // This is more optimal
        coroutinesLivenessAnalysisFallbackPhase, // While this is simple
        typeOperatorPhase,
        expressionBodyTransformPhase,
        objectClassesPhase,
        staticInitializersPhase,
        builtinOperatorPhase,
        bridgesPhase,
        exportInternalAbiPhase.takeIf { this.produce.isCache },
        useInternalAbiPhase,
        eraseGenericCallsReturnTypesPhase,
        autoboxPhase,
        constructorsLoweringPhase,
)

private fun createFileLoweringPhase(
        name: String,
        lowering: (NativeGenerationState) -> FileLoweringPass,
        prerequisite: Set<NamedCompilerPhase<*, *, *>> = emptySet(),
): NamedCompilerPhase<NativeGenerationState, IrFile, IrFile> = createSimpleNamedCompilerPhase(
        name,
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        prerequisite = prerequisite,
        outputIfNotEnabled = { _, _, _, irFile -> irFile },
        op = { context, irFile ->
            lowering(context).lower(irFile)
            irFile
        }
)

private fun createFileLoweringPhase(
        lowering: (Context) -> FileLoweringPass,
        name: String,
        prerequisite: Set<NamedCompilerPhase<*, *, *>> = emptySet(),
): NamedCompilerPhase<NativeGenerationState, IrFile, IrFile> = createSimpleNamedCompilerPhase(
        name,
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        prerequisite = prerequisite,
        outputIfNotEnabled = { _, _, _, irFile -> irFile },
        op = { context, irFile ->
            lowering(context.context).lower(irFile)
            irFile
        }
)

private fun createFileLoweringPhase(
        op: (context: Context, irFile: IrFile) -> Unit,
        name: String,
        prerequisite: Set<NamedCompilerPhase<*, *, *>> = emptySet(),
): NamedCompilerPhase<NativeGenerationState, IrFile, IrFile> = createSimpleNamedCompilerPhase(
        name,
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        prerequisite = prerequisite,
        outputIfNotEnabled = { _, _, _, irFile -> irFile },
        op = { context, irFile ->
            op(context.context, irFile)
            irFile
        }
)

