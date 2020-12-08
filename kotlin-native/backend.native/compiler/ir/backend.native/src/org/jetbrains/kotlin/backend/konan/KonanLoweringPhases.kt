package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.inline.FunctionInlining
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesExtractionFromInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.lower.optimizations.FoldConstantLowering
import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.FinallyBlocksLowering
import org.jetbrains.kotlin.backend.konan.lower.InitializersLowering
import org.jetbrains.kotlin.backend.konan.lower.StringConcatenationLowering
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

private val validateAll = false
private val filePhaseActions = if (validateAll) setOf(defaultDumper, ::fileValidationCallback) else setOf(defaultDumper)
private val modulePhaseActions = if (validateAll) setOf(defaultDumper, ::moduleValidationCallback) else setOf(defaultDumper)

private fun makeKonanFileLoweringPhase(
        lowering: (Context) -> FileLoweringPass,
        name: String,
        description: String,
        prerequisite: Set<NamedCompilerPhase<Context, *>> = emptySet()
) = makeIrFilePhase(lowering, name, description, prerequisite, actions = filePhaseActions)

private fun makeKonanModuleLoweringPhase(
        lowering: (Context) -> FileLoweringPass,
        name: String,
        description: String,
        prerequisite: Set<NamedCompilerPhase<Context, *>> = emptySet()
) = makeIrModulePhase(lowering, name, description, prerequisite, actions = modulePhaseActions)

internal fun makeKonanFileOpPhase(
        op: (Context, IrFile) -> Unit,
        name: String,
        description: String,
        prerequisite: Set<NamedCompilerPhase<Context, *>> = emptySet()
) = NamedCompilerPhase(
        name, description, prerequisite, nlevels = 0,
        lower = object : SameTypeCompilerPhase<Context, IrFile> {
            override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<IrFile>, context: Context, input: IrFile): IrFile {
                op(context, input)
                return input
            }
        },
        actions = filePhaseActions
)

internal fun makeKonanModuleOpPhase(
        op: (Context, IrModuleFragment) -> Unit,
        name: String,
        description: String,
        prerequisite: Set<NamedCompilerPhase<Context, *>> = emptySet()
) = NamedCompilerPhase(
        name, description, prerequisite, nlevels = 0,
        lower = object : SameTypeCompilerPhase<Context, IrModuleFragment> {
            override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment): IrModuleFragment {
                op(context, input)
                return input
            }
        },
        actions = modulePhaseActions
)

internal val specialBackendChecksPhase = konanUnitPhase(
        op = { irModule!!.files.forEach { SpecialBackendChecksTraversal(this).lower(it) } },
        name = "SpecialBackendChecks",
        description = "Special backend checks"
)

internal val removeExpectDeclarationsPhase = makeKonanModuleLoweringPhase(
        ::ExpectDeclarationsRemoving,
        name = "RemoveExpectDeclarations",
        description = "Expect declarations removing"
)

internal val stripTypeAliasDeclarationsPhase = makeKonanModuleLoweringPhase(
        { StripTypeAliasDeclarationsLowering() },
        name = "StripTypeAliasDeclarations",
        description = "Strip typealias declarations"
)

internal val lowerBeforeInlinePhase = makeKonanModuleLoweringPhase(
        ::PreInlineLowering,
        name = "LowerBeforeInline",
        description = "Special operations processing before inlining"
)

internal val arrayConstructorPhase = makeKonanModuleLoweringPhase(
        ::ArrayConstructorLowering,
        name = "ArrayConstructor",
        description = "Transform `Array(size) { index -> value }` into a loop"
)

internal val lateinitPhase = makeKonanModuleOpPhase(
        { context, irModule ->
            NullableFieldsForLateinitCreationLowering(context).lower(irModule)
            NullableFieldsDeclarationLowering(context).lower(irModule)
            LateinitUsageLowering(context).lower(irModule)
        },
        name = "Lateinit",
        description = "Lateinit properties lowering"
)

internal val propertyAccessorInlinePhase = makeKonanModuleLoweringPhase(
        ::PropertyAccessorInlineLowering,
        name = "PropertyAccessorInline",
        description = "Property accessor inline lowering"
)

internal val sharedVariablesPhase = makeKonanModuleLoweringPhase(
        ::SharedVariablesLowering,
        name = "SharedVariables",
        description = "Shared variable lowering",
        prerequisite = setOf(lateinitPhase)
)

internal val extractLocalClassesFromInlineBodies = NamedCompilerPhase(
        lower = object : SameTypeCompilerPhase<Context, IrModuleFragment> {
            override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment): IrModuleFragment {
                LocalClassesInInlineLambdasLowering(context).run {
                    input.files.forEach { lower(it) }
                }
                LocalClassesInInlineFunctionsLowering(context).run {
                    input.files.forEach { lower(it) }
                }
                LocalClassesExtractionFromInlineFunctionsLowering(context).run {
                    input.files.forEach { lower(it) }
                }
                return input
            }
        },
        name = "ExtractLocalClassesFromInlineBodies",
        description = "Extraction of local classes from inline bodies",
        prerequisite = setOf(sharedVariablesPhase),
        nlevels = 0,
        actions = modulePhaseActions
)

internal val inlinePhase = NamedCompilerPhase(
        lower = object : SameTypeCompilerPhase<Context, IrModuleFragment> {
            override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment): IrModuleFragment {
                FunctionInlining(context, NativeInlineFunctionResolver(context)).run {
                    input.files.forEach { lower(it) }
                }
                return input
            }
        },
        name = "Inline",
        description = "Functions inlining",
        prerequisite = setOf(lowerBeforeInlinePhase, arrayConstructorPhase, extractLocalClassesFromInlineBodies),
        nlevels = 0,
        actions = modulePhaseActions
)

internal val lowerAfterInlinePhase = makeKonanModuleOpPhase(
        { context, irModule ->
            irModule.files.forEach(PostInlineLowering(context)::lower)
            // TODO: Seems like this should be deleted in PsiToIR.
            irModule.files.forEach(ContractsDslRemover(context)::lower)
        },
        name = "LowerAfterInline",
        description = "Special operations processing after inlining"
)

/* IrFile phases */

// TODO make all lambda-related stuff work with IrFunctionExpression and drop this phase (see kotlin: dd3f8ecaacd)
internal val provisionalFunctionExpressionPhase = makeKonanModuleLoweringPhase(
    { ProvisionalFunctionExpressionLowering() },
    name = "FunctionExpression-before-inliner",
    description = "Transform IrFunctionExpression to a local function reference"
)

internal val flattenStringConcatenationPhase = makeKonanFileLoweringPhase(
        ::FlattenStringConcatenationLowering,
        name = "FlattenStringConcatenationLowering",
        description = "Flatten nested string concatenation expressions into a single IrStringConcatenation"
)

internal val stringConcatenationPhase = makeKonanFileLoweringPhase(
        ::StringConcatenationLowering,
        name = "StringConcatenation",
        description = "String concatenation lowering"
)

internal val kotlinNothingValueExceptionPhase = makeKonanFileLoweringPhase(
        ::KotlinNothingValueExceptionLowering,
        name = "KotlinNothingValueException",
        description = "Throw proper exception for calls returning value of type 'kotlin.Nothing'"
)

internal val enumConstructorsPhase = makeKonanFileLoweringPhase(
        ::EnumConstructorsLowering,
        name = "EnumConstructors",
        description = "Enum constructors lowering"
)

internal val initializersPhase = makeKonanFileLoweringPhase(
        ::InitializersLowering,
        name = "Initializers",
        description = "Initializers lowering",
        prerequisite = setOf(enumConstructorsPhase)
)

internal val localFunctionsPhase = makeKonanFileOpPhase(
        op = { context, irFile ->
            LocalDelegatedPropertiesLowering().lower(irFile)
            LocalDeclarationsLowering(context).lower(irFile)
            LocalClassPopupLowering(context).lower(irFile)
        },
        name = "LocalFunctions",
        description = "Local function lowering",
        prerequisite = setOf(sharedVariablesPhase)
)

internal val tailrecPhase = makeKonanFileLoweringPhase(
        ::TailrecLowering,
        name = "Tailrec",
        description = "Tailrec lowering",
        prerequisite = setOf(localFunctionsPhase)
)

internal val defaultParameterExtentPhase = makeKonanFileOpPhase(
        { context, irFile ->
            KonanDefaultArgumentStubGenerator(context).lower(irFile)
            DefaultParameterCleaner(context, replaceDefaultValuesWithStubs = true).lower(irFile)
            KonanDefaultParameterInjector(context).lower(irFile)
        },
        name = "DefaultParameterExtent",
        description = "Default parameter extent lowering",
        prerequisite = setOf(tailrecPhase, enumConstructorsPhase)
)

internal val innerClassPhase = makeKonanFileLoweringPhase(
        ::InnerClassLowering,
        name = "InnerClasses",
        description = "Inner classes lowering",
        prerequisite = setOf(defaultParameterExtentPhase)
)

internal val rangeContainsLoweringPhase = makeKonanFileLoweringPhase(
        ::RangeContainsLowering,
        name = "RangeContains",
        description = "Optimizes calls to contains() for ClosedRanges"
)

internal val forLoopsPhase = makeKonanFileLoweringPhase(
        ::ForLoopsLowering,
        name = "ForLoops",
        description = "For loops lowering"
)

internal val dataClassesPhase = makeKonanFileLoweringPhase(
        ::DataClassOperatorsLowering,
        name = "DataClasses",
        description = "Data classes lowering"
)

internal val finallyBlocksPhase = makeKonanFileLoweringPhase(
        ::FinallyBlocksLowering,
        name = "FinallyBlocks",
        description = "Finally blocks lowering",
        prerequisite = setOf(initializersPhase, localFunctionsPhase, tailrecPhase)
)

internal val testProcessorPhase = makeKonanFileOpPhase(
        { context, irFile -> TestProcessor(context).process(irFile) },
        name = "TestProcessor",
        description = "Unit test processor"
)

internal val delegationPhase = makeKonanFileLoweringPhase(
        ::PropertyDelegationLowering,
        name = "Delegation",
        description = "Delegation lowering"
)

internal val functionReferencePhase = makeKonanFileLoweringPhase(
        ::FunctionReferenceLowering,
        name = "FunctionReference",
        description = "Function references lowering",
        prerequisite = setOf(delegationPhase, localFunctionsPhase) // TODO: make weak dependency on `testProcessorPhase`
)

internal val enumClassPhase = makeKonanFileOpPhase(
        { context, irFile -> EnumClassLowering(context).run(irFile) },
        name = "Enums",
        description = "Enum classes lowering",
        prerequisite = setOf(enumConstructorsPhase, functionReferencePhase) // TODO: make weak dependency on `testProcessorPhase`
)

internal val singleAbstractMethodPhase = makeKonanFileLoweringPhase(
        ::NativeSingleAbstractMethodLowering,
        name = "SingleAbstractMethod",
        description = "Replace SAM conversions with instances of interface-implementing classes",
        prerequisite = setOf(functionReferencePhase)
)

internal val builtinOperatorPhase = makeKonanFileLoweringPhase(
        ::BuiltinOperatorLowering,
        name = "BuiltinOperators",
        description = "BuiltIn operators lowering",
        prerequisite = setOf(defaultParameterExtentPhase, singleAbstractMethodPhase)
)

internal val interopPhase = makeKonanFileLoweringPhase(
        ::InteropLowering,
        name = "Interop",
        description = "Interop lowering",
        prerequisite = setOf(inlinePhase, localFunctionsPhase, functionReferencePhase)
)

internal val varargPhase = makeKonanFileLoweringPhase(
        ::VarargInjectionLowering,
        name = "Vararg",
        description = "Vararg lowering",
        prerequisite = setOf(functionReferencePhase, defaultParameterExtentPhase, interopPhase)
)

internal val compileTimeEvaluatePhase = makeKonanFileLoweringPhase(
        ::CompileTimeEvaluateLowering,
        name = "CompileTimeEvaluate",
        description = "Compile time evaluation lowering",
        prerequisite = setOf(varargPhase)
)

internal val coroutinesPhase = makeKonanFileLoweringPhase(
        ::NativeSuspendFunctionsLowering,
        name = "Coroutines",
        description = "Coroutines lowering",
        prerequisite = setOf(localFunctionsPhase, finallyBlocksPhase, kotlinNothingValueExceptionPhase)
)

internal val typeOperatorPhase = makeKonanFileLoweringPhase(
        ::TypeOperatorLowering,
        name = "TypeOperators",
        description = "Type operators lowering",
        prerequisite = setOf(coroutinesPhase)
)

internal val bridgesPhase = makeKonanFileOpPhase(
        { context, irFile ->
            BridgesBuilding(context).runOnFilePostfix(irFile)
            WorkersBridgesBuilding(context).lower(irFile)
        },
        name = "Bridges",
        description = "Bridges building",
        prerequisite = setOf(coroutinesPhase)
)

internal val autoboxPhase = makeKonanFileLoweringPhase(
        ::Autoboxing,
        name = "Autobox",
        description = "Autoboxing of primitive types",
        prerequisite = setOf(bridgesPhase, coroutinesPhase)
)

internal val returnsInsertionPhase = makeKonanFileLoweringPhase(
        ::ReturnsInsertionLowering,
        name = "ReturnsInsertion",
        description = "Returns insertion for Unit functions",
        prerequisite = setOf(autoboxPhase, coroutinesPhase, enumClassPhase)
)

internal val ifNullExpressionsFusionPhase = makeKonanFileLoweringPhase(
        ::IfNullExpressionsFusionLowering,
        name = "IfNullExpressionsFusionLowering",
        description = "Simplify '?.' and '?:' operator chains"
)

internal val foldConstantLoweringPhase = makeKonanFileOpPhase(
        { context, irFile -> FoldConstantLowering(context).lower(irFile) },
        name = "FoldConstantLowering",
        description = "Constant Folding",
        prerequisite = setOf(flattenStringConcatenationPhase)
)