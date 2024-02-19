/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesExtractionFromInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.lower.optimizations.LivenessAnalysis
import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.ir.FunctionsWithoutBoundCheckGenerator
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.ImportCachesAbiTransformer
import org.jetbrains.kotlin.backend.konan.lower.InitializersLowering
import org.jetbrains.kotlin.backend.konan.lower.InlineClassPropertyAccessorsLowering
import org.jetbrains.kotlin.backend.konan.lower.RedundantCoercionsCleaner
import org.jetbrains.kotlin.backend.konan.lower.ReturnsInsertionLowering
import org.jetbrains.kotlin.backend.konan.lower.UnboxInlineLowering
import org.jetbrains.kotlin.backend.konan.optimizations.KonanBCEForLoopBodyTransformer
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Run whole IR lowering pipeline over [irModuleFragment].
 */
internal fun PhaseEngine<NativeGenerationState>.runAllLowerings(irModuleFragment: IrModuleFragment) {
    val lowerings = getAllLowerings()
    irModuleFragment.files.forEach { file ->
        context.fileLowerState = FileLowerState()
        lowerings.fold(file) { loweredFile, lowering ->
            runPhase(lowering, loweredFile)
        }
    }
}

internal val functionsWithoutBoundCheck = createSimpleNamedCompilerPhase<Context, Unit>(
        name = "FunctionsWithoutBoundCheckGenerator",
        description = "Functions without bounds check generation",
        op = { context, _ -> FunctionsWithoutBoundCheckGenerator(context).generate() }
)

private val removeExpectDeclarationsPhase = createFileLoweringPhase(
        ::ExpectDeclarationsRemoving,
        name = "RemoveExpectDeclarations",
        description = "Expect declarations removing"
)

private val stripTypeAliasDeclarationsPhase = createFileLoweringPhase(
        { _: Context -> StripTypeAliasDeclarationsLowering() },
        name = "StripTypeAliasDeclarations",
        description = "Strip typealias declarations"
)

private val annotationImplementationPhase = createFileLoweringPhase(
        { context -> AnnotationImplementationLowering { NativeAnnotationImplementationTransformer(context, it) } },
        name = "AnnotationImplementation",
        description = "Create synthetic annotations implementations and use them in annotations constructor calls"
)

private val lowerBeforeInlinePhase = createFileLoweringPhase(
        ::TypeOfLowering,
        name = "LowerBeforeInline",
        description = "Special operations processing before inlining"
)

private val arrayConstructorPhase = createFileLoweringPhase(
        ::ArrayConstructorLowering,
        name = "ArrayConstructor",
        description = "Transform `Array(size) { index -> value }` into a loop"
)

private val lateinitPhase = createFileLoweringPhase(
        { context, irFile ->
            NullableFieldsForLateinitCreationLowering(context).lower(irFile)
            NullableFieldsDeclarationLowering(context).lower(irFile)
            LateinitUsageLowering(context).lower(irFile)
        },
        name = "Lateinit",
        description = "Lateinit properties lowering"
)

private val sharedVariablesPhase = createFileLoweringPhase(
        ::SharedVariablesLowering,
        name = "SharedVariables",
        description = "Shared variable lowering",
        prerequisite = setOf(lateinitPhase)
)

private val lowerOuterThisInInlineFunctionsPhase = createFileLoweringPhase(
        { context, irFile ->
            irFile.acceptChildrenVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunction(declaration: IrFunction) {
                    declaration.acceptChildrenVoid(this)

                    if (declaration.isInline)
                        OuterThisLowering(context).lower(declaration)
                }
            })
        },
        name = "LowerOuterThisInInlineFunctions",
        description = "Lower outer this in inline functions"
)

private val extractLocalClassesFromInlineBodies = createFileLoweringPhase(
        { context, irFile ->
            LocalClassesInInlineLambdasLowering(context).lower(irFile)
            if (!context.config.produce.isCache) {
                LocalClassesInInlineFunctionsLowering(context).lower(irFile)
                LocalClassesExtractionFromInlineFunctionsLowering(context).lower(irFile)
            }
        },
        name = "ExtractLocalClassesFromInlineBodies",
        description = "Extraction of local classes from inline bodies",
        prerequisite = setOf(sharedVariablesPhase),
)

private val wrapInlineDeclarationsWithReifiedTypeParametersLowering = createFileLoweringPhase(
        ::WrapInlineDeclarationsWithReifiedTypeParametersLowering,
        name = "WrapInlineDeclarationsWithReifiedTypeParameters",
        description = "Wrap inline declarations with reified type parameters"
)

private val postInlinePhase = createFileLoweringPhase(
        { context: Context -> PostInlineLowering(context) },
        name = "PostInline",
        description = "Post-processing after inlining"
)

private val contractsDslRemovePhase = createFileLoweringPhase(
        { context: Context -> ContractsDslRemover(context) },
        name = "RemoveContractsDsl",
        description = "Contracts dsl removing"
)

// TODO make all lambda-related stuff work with IrFunctionExpression and drop this phase (see kotlin: dd3f8ecaacd)
private val provisionalFunctionExpressionPhase = createFileLoweringPhase(
        { _: Context -> ProvisionalFunctionExpressionLowering() },
        name = "FunctionExpression",
        description = "Transform IrFunctionExpression to a local function reference"
)

private val flattenStringConcatenationPhase = createFileLoweringPhase(
        ::FlattenStringConcatenationLowering,
        name = "FlattenStringConcatenationLowering",
        description = "Flatten nested string concatenation expressions into a single IrStringConcatenation"
)

private val stringConcatenationPhase = createFileLoweringPhase(
        ::StringConcatenationLowering,
        name = "StringConcatenation",
        description = "String concatenation lowering"
)

private val stringConcatenationTypeNarrowingPhase = createFileLoweringPhase(
        ::StringConcatenationTypeNarrowing,
        name = "StringConcatenationTypeNarrowing",
        description = "String concatenation type narrowing",
        prerequisite = setOf(stringConcatenationPhase)
)

private val kotlinNothingValueExceptionPhase = createFileLoweringPhase(
        ::KotlinNothingValueExceptionLowering,
        name = "KotlinNothingValueException",
        description = "Throw proper exception for calls returning value of type 'kotlin.Nothing'"
)

private val enumConstructorsPhase = createFileLoweringPhase(
        ::EnumConstructorsLowering,
        name = "EnumConstructors",
        description = "Enum constructors lowering"
)

private val initializersPhase = createFileLoweringPhase(
        ::InitializersLowering,
        name = "Initializers",
        description = "Initializers lowering",
        prerequisite = setOf(enumConstructorsPhase)
)

private val localFunctionsPhase = createFileLoweringPhase(
        op = { context, irFile ->
            LocalDelegatedPropertiesLowering().lower(irFile)
            LocalDeclarationsLowering(context).lower(irFile)
            LocalClassPopupLowering(context).lower(irFile)
        },
        name = "LocalFunctions",
        description = "Local function lowering",
        prerequisite = setOf(sharedVariablesPhase) // TODO: add "soft" dependency on inventNamesForLocalClasses
)

private val tailrecPhase = createFileLoweringPhase(
        ::TailrecLowering,
        name = "Tailrec",
        description = "Tailrec lowering",
        prerequisite = setOf(localFunctionsPhase)
)

private val volatilePhase = createFileLoweringPhase(
        ::VolatileFieldsLowering,
        name = "VolatileFields",
        description = "Volatile fields processing",
        prerequisite = setOf(localFunctionsPhase)
)

private val defaultParameterExtentPhase = createFileLoweringPhase(
        { context, irFile ->
            NativeDefaultArgumentStubGenerator(context).lower(irFile)
            DefaultParameterCleaner(context, replaceDefaultValuesWithStubs = true).lower(irFile)
            NativeDefaultParameterInjector(context).lower(irFile)
        },
        name = "DefaultParameterExtent",
        description = "Default parameter extent lowering",
        prerequisite = setOf(tailrecPhase, enumConstructorsPhase)
)

private val innerClassPhase = createFileLoweringPhase(
        ::InnerClassLowering,
        name = "InnerClasses",
        description = "Inner classes lowering",
        prerequisite = setOf(defaultParameterExtentPhase)
)

private val rangeContainsLoweringPhase = createFileLoweringPhase(
        ::RangeContainsLowering,
        name = "RangeContains",
        description = "Optimizes calls to contains() for ClosedRanges"
)

private val forLoopsPhase = createFileLoweringPhase(
        { context, irFile ->
            ForLoopsLowering(context, KonanBCEForLoopBodyTransformer()).lower(irFile)
        },
        name = "ForLoops",
        description = "For loops lowering",
        prerequisite = setOf(functionsWithoutBoundCheck)
)

private val dataClassesPhase = createFileLoweringPhase(
        ::DataClassOperatorsLowering,
        name = "DataClasses",
        description = "Data classes lowering"
)

private val finallyBlocksPhase = createFileLoweringPhase(
        { context, irFile -> FinallyBlocksLowering(context, context.irBuiltIns.throwableType).lower(irFile) },
        name = "FinallyBlocks",
        description = "Finally blocks lowering",
        prerequisite = setOf(initializersPhase, localFunctionsPhase, tailrecPhase)
)

private val testProcessorPhase = createFileLoweringPhase(
        { context, irFile -> TestProcessor(context).process(irFile) },
        name = "TestProcessor",
        description = "Unit test processor"
)

private val delegationPhase = createFileLoweringPhase(
        lowering = ::PropertyDelegationLowering,
        name = "Delegation",
        description = "Delegation lowering",
        prerequisite = setOf(volatilePhase)
)

private val functionReferencePhase = createFileLoweringPhase(
        lowering = ::FunctionReferenceLowering,
        name = "FunctionReference",
        description = "Function references lowering",
        prerequisite = setOf(delegationPhase, localFunctionsPhase) // TODO: make weak dependency on `testProcessorPhase`
)

private val enumWhenPhase = createFileLoweringPhase(
        ::NativeEnumWhenLowering,
        name = "EnumWhen",
        description = "Enum when lowering",
        prerequisite = setOf(enumConstructorsPhase, functionReferencePhase)
)

private val enumClassPhase = createFileLoweringPhase(
        ::EnumClassLowering,
        name = "Enums",
        description = "Enum classes lowering",
        prerequisite = setOf(enumConstructorsPhase, functionReferencePhase, enumWhenPhase) // TODO: make weak dependency on `testProcessorPhase`
)

private val enumUsagePhase = createFileLoweringPhase(
        ::EnumUsageLowering,
        name = "EnumUsage",
        description = "Enum usage lowering",
        prerequisite = setOf(enumConstructorsPhase, functionReferencePhase, enumClassPhase)
)


private val singleAbstractMethodPhase = createFileLoweringPhase(
        ::NativeSingleAbstractMethodLowering,
        name = "SingleAbstractMethod",
        description = "Replace SAM conversions with instances of interface-implementing classes",
        prerequisite = setOf(functionReferencePhase)
)

private val builtinOperatorPhase = createFileLoweringPhase(
        ::BuiltinOperatorLowering,
        name = "BuiltinOperators",
        description = "BuiltIn operators lowering",
        prerequisite = setOf(defaultParameterExtentPhase, singleAbstractMethodPhase, enumWhenPhase)
)

private val inlinePhase = createFileLoweringPhase(
        lowering = { context: NativeGenerationState ->
            object : FileLoweringPass {
                override fun lower(irFile: IrFile) {
                    irFile.acceptChildrenVoid(object : IrElementVisitorVoid {
                        override fun visitElement(element: IrElement) {
                            element.acceptChildrenVoid(this)
                        }

                        override fun visitFunction(declaration: IrFunction) {
                            if (declaration.isInline)
                                context.context.inlineFunctionsSupport.savePartiallyLoweredInlineFunction(declaration)
                            declaration.acceptChildrenVoid(this)
                        }
                    })

                    FunctionInlining(
                            context.context,
                            NativeInlineFunctionResolver(context.context, context),
                            alwaysCreateTemporaryVariablesForArguments = context.shouldContainDebugInfo()
                    ).lower(irFile)
                }
            }
        },
        name = "Inline",
        description = "Functions inlining",
        prerequisite = setOf(lowerBeforeInlinePhase, arrayConstructorPhase, extractLocalClassesFromInlineBodies)
)

private val interopPhase = createFileLoweringPhase(
        lowering = ::InteropLowering,
        name = "Interop",
        description = "Interop lowering",
        prerequisite = setOf(inlinePhase, localFunctionsPhase, functionReferencePhase)
)

private val varargPhase = createFileLoweringPhase(
        ::VarargInjectionLowering,
        name = "Vararg",
        description = "Vararg lowering",
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
        description = "Coroutines lowering",
        prerequisite = setOf(localFunctionsPhase, finallyBlocksPhase, kotlinNothingValueExceptionPhase)
)

private val coroutinesLivenessAnalysisFallbackPhase = createFileLoweringPhase(
        lowering = ::CoroutinesLivenessAnalysisFallback,
        name = "CoroutinesLivenessAnalysisFallback",
        description = "Compute visible variables at suspension points",
        prerequisite = setOf(coroutinesPhase)
)

private val coroutinesLivenessAnalysisPhase = createFileLoweringPhase(
        lowering = { context: NativeGenerationState ->
            object : BodyLoweringPass {
                override fun lower(irBody: IrBody, container: IrDeclaration) {
                    val liveVariablesAtSuspensionPoints = context.liveVariablesAtSuspensionPoints
                    LivenessAnalysis.run(irBody) { it is IrSuspensionPoint }
                            .forEach { (irElement, liveVariables) ->
                                liveVariablesAtSuspensionPoints[irElement as IrSuspensionPoint] = liveVariables
                            }
                }
            }
        },
        name = "CoroutinesLivenessAnalysis",
        description = "Run liveness analysis for coroutines",
        prerequisite = setOf(coroutinesPhase)
)

private val coroutinesVarSpillingPhase = createFileLoweringPhase(
        lowering = ::CoroutinesVarSpillingLowering,
        name = "CoroutinesVarSpilling",
        description = "Save/restore coroutines variables before/after suspension",
        prerequisite = setOf(coroutinesPhase)
)

private val typeOperatorPhase = createFileLoweringPhase(
        ::TypeOperatorLowering,
        name = "TypeOperators",
        description = "Type operators lowering",
        prerequisite = setOf(coroutinesPhase)
)

private val bridgesPhase = createFileLoweringPhase(
        { context, irFile ->
            BridgesBuilding(context).runOnFilePostfix(irFile)
            WorkersBridgesBuilding(context).lower(irFile)
        },
        name = "Bridges",
        description = "Bridges building",
        prerequisite = setOf(coroutinesPhase)
)

private val autoboxPhase = createFileLoweringPhase(
        ::Autoboxing,
        name = "Autobox",
        description = "Autoboxing of primitive types",
        prerequisite = setOf(bridgesPhase, coroutinesPhase)
)

private val expressionBodyTransformPhase = createFileLoweringPhase(
        ::ExpressionBodyTransformer,
        name = "ExpressionBodyTransformer",
        description = "Replace IrExpressionBody with IrBlockBody"
)

private val staticInitializersPhase = createFileLoweringPhase(
        ::StaticInitializersLowering,
        name = "StaticInitializers",
        description = "Add calls to static initializers",
        prerequisite = setOf(expressionBodyTransformPhase)
)

private val ifNullExpressionsFusionPhase = createFileLoweringPhase(
        ::IfNullExpressionsFusionLowering,
        name = "IfNullExpressionsFusionLowering",
        description = "Simplify '?.' and '?:' operator chains"
)

private val exportInternalAbiPhase = createFileLoweringPhase(
        ::ExportCachesAbiVisitor,
        name = "ExportInternalAbi",
        description = "Add accessors to private entities"
)

internal val ReturnsInsertionPhase = createFileLoweringPhase(
        name = "ReturnsInsertion",
        description = "Returns insertion for Unit functions",
        prerequisite = setOf(autoboxPhase, coroutinesPhase, enumClassPhase),
        lowering = ::ReturnsInsertionLowering,
)

internal val InlineClassPropertyAccessorsPhase = createFileLoweringPhase(
        name = "InlineClassPropertyAccessorsLowering",
        description = "Inline class property accessors",
        lowering = ::InlineClassPropertyAccessorsLowering,
)

internal val RedundantCoercionsCleaningPhase = createFileLoweringPhase(
        name = "RedundantCoercionsCleaning",
        description = "Redundant coercions cleaning",
        lowering = ::RedundantCoercionsCleaner,
)

internal val PropertyAccessorInlinePhase = createFileLoweringPhase(
        name = "PropertyAccessorInline",
        description = "Property accessor inline lowering",
        lowering = ::PropertyAccessorInlineLowering,
)

internal val UnboxInlinePhase = createFileLoweringPhase(
        name = "UnboxInline",
        description = "Unbox functions inline lowering",
        lowering = ::UnboxInlineLowering,
)

private val inventNamesForLocalClasses = createFileLoweringPhase(
        lowering = ::NativeInventNamesForLocalClasses,
        name = "InventNamesForLocalClasses",
        description = "Invent names for local classes and anonymous objects",
)

private val useInternalAbiPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile, IrFile>(
        name = "UseInternalAbi",
        description = "Use internal ABI functions to access private entities",
        outputIfNotEnabled = { _, _, _, irFile -> irFile },
) { context, file ->
    ImportCachesAbiTransformer(context).lower(file)
    file
}


private val objectClassesPhase = createFileLoweringPhase(
        lowering = ::ObjectClassLowering,
        name = "ObjectClasses",
        description = "Object classes lowering"
)

private val assertsRemovalPhase = createFileLoweringPhase(
        lowering = ::AssertRemovalLowering,
        name = "AssertsRemoval",
        description = "Asserts removal"
)

private val constEvaluationPhase = createFileLoweringPhase(
        lowering = { context: Context ->
            val configuration = IrInterpreterConfiguration(printOnlyExceptionMessage = true)
            ConstEvaluationLowering(context, configuration = configuration)
        },
        name = "ConstEvaluationLowering",
        description = "Evaluate functions that are marked as `IntrinsicConstEvaluation`",
        prerequisite = setOf(inlinePhase)
)

private fun PhaseEngine<NativeGenerationState>.getAllLowerings() = listOfNotNull<AbstractNamedCompilerPhase<NativeGenerationState, IrFile, IrFile>>(
        lowerBeforeInlinePhase,
        arrayConstructorPhase,
        lateinitPhase,
        sharedVariablesPhase,
        lowerOuterThisInInlineFunctionsPhase,
        extractLocalClassesFromInlineBodies,
        wrapInlineDeclarationsWithReifiedTypeParametersLowering,
        inlinePhase,
        removeExpectDeclarationsPhase,
        stripTypeAliasDeclarationsPhase,
        assertsRemovalPhase.takeUnless { context.config.assertsEnabled },
        constEvaluationPhase,
        provisionalFunctionExpressionPhase,
        postInlinePhase,
        contractsDslRemovePhase,
        annotationImplementationPhase,
        rangeContainsLoweringPhase,
        forLoopsPhase,
        flattenStringConcatenationPhase,
        stringConcatenationPhase,
        stringConcatenationTypeNarrowingPhase.takeIf { context.config.optimizationsEnabled },
        enumConstructorsPhase,
        initializersPhase,
        inventNamesForLocalClasses,
        localFunctionsPhase,
        volatilePhase,
        tailrecPhase,
        defaultParameterExtentPhase,
        innerClassPhase,
        dataClassesPhase,
        ifNullExpressionsFusionPhase,
        testProcessorPhase.takeIf { context.config.configuration.getNotNull(KonanConfigKeys.GENERATE_TEST_RUNNER) != TestRunnerKind.NONE },
        delegationPhase,
        functionReferencePhase,
        singleAbstractMethodPhase,
        enumWhenPhase,
        finallyBlocksPhase,
        enumClassPhase,
        enumUsagePhase,
        interopPhase,
        varargPhase,
        kotlinNothingValueExceptionPhase,
        coroutinesPhase,
        // Either of these could be turned off without losing correctness.
        coroutinesLivenessAnalysisPhase, // This is more optimal
        coroutinesLivenessAnalysisFallbackPhase, // While this is simple
        coroutinesVarSpillingPhase,
        typeOperatorPhase,
        expressionBodyTransformPhase,
        objectClassesPhase,
        staticInitializersPhase,
        builtinOperatorPhase,
        bridgesPhase,
        exportInternalAbiPhase.takeIf { context.config.produce.isCache },
        useInternalAbiPhase,
        autoboxPhase,
)

private fun createFileLoweringPhase(
        name: String,
        description: String,
        lowering: (NativeGenerationState) -> FileLoweringPass,
        prerequisite: Set<AbstractNamedCompilerPhase<*, *, *>> = emptySet(),
): SimpleNamedCompilerPhase<NativeGenerationState, IrFile, IrFile> = createSimpleNamedCompilerPhase(
        name,
        description,
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
        description: String,
        prerequisite: Set<AbstractNamedCompilerPhase<*, *, *>> = emptySet(),
): SimpleNamedCompilerPhase<NativeGenerationState, IrFile, IrFile> = createSimpleNamedCompilerPhase(
        name,
        description,
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
        description: String,
        prerequisite: Set<AbstractNamedCompilerPhase<*, *, *>> = emptySet(),
): SimpleNamedCompilerPhase<NativeGenerationState, IrFile, IrFile> = createSimpleNamedCompilerPhase(
        name,
        description,
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        prerequisite = prerequisite,
        outputIfNotEnabled = { _, _, _, irFile -> irFile },
        op = { context, irFile ->
            op(context.context, irFile)
            irFile
        }
)

