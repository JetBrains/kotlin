/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.util.isTypeOfIntrinsic
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.InlineCallCycleCheckerLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.common.phaser.IrValidationBeforeLoweringsKlibSecondStagePhase
import org.jetbrains.kotlin.backend.common.wrapWithCompilationException
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.ir.FunctionsWithoutBoundCheckGenerator
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.InitializersLowering
import org.jetbrains.kotlin.backend.konan.optimizations.NativeForLoopsLowering
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.inline.*
import org.jetbrains.kotlin.backend.konan.lower.NativeAssertionWrapperLowering
import org.jetbrains.kotlin.backend.konan.optimizations.CastsOptimization
import org.jetbrains.kotlin.backend.konan.optimizations.ComputeTypesPass
import org.jetbrains.kotlin.konan.config.NativeConfigurationKeys
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasureDynamicPhaseTime
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

internal typealias LoweringList = List<NamedCompilerPhase<NativeGenerationState, IrFile, IrFile>>
internal typealias ModuleLowering = NamedCompilerPhase<NativeGenerationState, IrModuleFragment, Unit>

internal fun PhaseEngine<NativeGenerationState>.runLowerings(
        lowerings: LoweringList,
        modules: List<IrModuleFragment>,
        performanceManager: PerformanceManager?,
) {
    for (module in modules) {
        for (file in module.files) {
            context.fileLowerState = FileLowerState()
            lowerings.fold(file) { loweredFile, lowering ->
                performanceManager.tryMeasureDynamicPhaseTime(lowering.name, PhaseType.IrLowering) {
                    try {
                        runPhase(lowering, loweredFile)
                    } catch (e: CompilationException) {
                        e.initializeFileDetails(loweredFile)
                        throw e
                    } catch (e: KotlinExceptionWithAttachments) {
                        throw e
                    } catch (e: Throwable) {
                        throw e.wrapWithCompilationException("Internal error in file lowering", loweredFile, null)
                    }
                }
            }
        }
    }
}

internal fun PhaseEngine<NativeGenerationState>.runModuleWisePhase(
        lowering: ModuleLowering,
        modules: List<IrModuleFragment>,
        performanceManager: PerformanceManager?,
) {
    performanceManager.tryMeasureDynamicPhaseTime(lowering.name, PhaseType.IrLowering) {
        for (module in modules) {
            runPhase(lowering, module)
        }
    }
}

internal val validateIrBeforeLowering = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "ValidateIrBeforeLowering",
        op = { context, module -> IrValidationBeforeLoweringsKlibSecondStagePhase(context.context).lower(module) }
)

internal val checkInlineCallCyclesPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "InlineCallCycleChecker",
        op = { context, module -> InlineCallCycleCheckerLowering(context.context).lower(module) }
)


internal val validateIrAfterInliningOnlyPrivateFunctions = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "ValidateIrAfterInliningOnlyPrivateFunctions",
        op = { context, module ->
            IrValidationAfterInliningPrivateFunctionsKlibPhase(
                    context = context.context,
                    checkInlineFunctionCallSites = { inlineFunctionUseSite ->
                        // Call sites of only non-private functions are allowed at this stage.
                        !inlineFunctionUseSite.symbol.isConsideredAsPrivateForInlining()
                    }
            ).lower(module)
        }
)

internal val validateIrAfterInliningAllFunctions = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "ValidateIrAfterInliningAllFunctions",
        op = { context, module ->
            IrValidationAfterInliningAllFunctionsKlibSecondStagePhase(
                    context = context.context,
                    checkInlineFunctionCallSites = check@{ inlineFunctionUseSite ->
                        // No inline function call sites should remain at this stage.
                        val inlineFunction = inlineFunctionUseSite.symbol.owner
                        // it's fine to have typeOf<T>, it would be ignored by inliner and handled on the second stage of compilation
                        if (inlineFunction.symbol.isTypeOfIntrinsic()) return@check true
                        return@check inlineFunction.body == null
                    }
            ).lower(module)
        }
)

internal val validateIrAfterLowering = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "ValidateIrAfterLowering",
        op = { context, module -> IrValidationAfterLoweringsSecondStagePhase(context.context).lower(module) }
)

internal val functionsWithoutBoundCheck = createSimpleNamedCompilerPhase<NativeBackendContext, Unit>(
        name = "FunctionsWithoutBoundCheckGenerator",
        op = { context, _ -> FunctionsWithoutBoundCheckGenerator(context).generate() }
)

/**
 * The second phase of inlining (inline all functions).
 */
internal val inlineAllFunctionsPhase = createFileLoweringPhase(
        lowering = ::NativeAllFunctionInlining,
        name = "InlineAllFunctions",
)

internal val CoroutinesVarSpillingPhase = createFileLoweringPhase(
        lowering = ::CoroutinesVarSpillingLowering,
        name = "CoroutinesVarSpilling",
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
        lowering = { context: NativeGenerationState -> PropertyAccessorInlineLowering(context) },
)

internal val UnboxInlinePhase = createFileLoweringPhase(
        name = "UnboxInline",
        lowering = { context: NativeGenerationState -> UnboxInlineLowering(context) },
)

internal fun createNativePhases(vararg phases: ((NativeGenerationState) -> FileLoweringPass)?) =
        createFilePhases(*phases, actions = getDefaultIrActions())

internal fun getLoweringsUpToAndIncludingSyntheticAccessors() = createNativePhases(
        ::TestProcessor,
        ::UpgradeCallableReferences,
        ::NativeAssertionWrapperLowering,
        ::LateinitLowering,
        ::SharedVariablesLowering,
        ::LocalClassesInInlineLambdasLowering,
        ::ArrayConstructorLowering,
        ::NativePrivateFunctionInlining,
        ::OuterThisInInlineFunctionsSpecialAccessorLowering,
        ::SyntheticAccessorLowering,
)

internal fun NativeSecondStageCompilationConfig.getLoweringsAfterInlining() = createNativePhases(
        ::ConstEvaluationLowering,
        ::ReifiedFunctionLowering,
        ::TypeOfProcessingLowering,
        ::NativeSharedVariablesPrimitiveBoxSpecializationLowering,
        ::InteropLowering,
        ::SpecialInteropIntrinsicsLowering,
        ::TestsInitializer,
        ::TestsDumper.takeIf { this.configuration.getNotNull(NativeConfigurationKeys.GENERATE_TEST_RUNNER) != TestRunnerKind.NONE },
        ::ExpectDeclarationsRemoveLowering,
        ::StripTypeAliasDeclarationsLowering,
        ::NativeAssertionRemoverLowering,
        ::VolatileFieldsLowering,
        ::DelegatedPropertyOptimizationLowering,
        ::PropertyReferenceLowering,
        ::NativeFunctionReferenceLowering,
        ::NativeSingleAbstractMethodLowering,
        ::PostInlineLowering,
        ::ExportedBridgeNonVirtualLowering,
        ::ContractsDslRemover,
        ::NativeAnnotationImplementationLowering,
        ::RangeContainsLowering,
        ::EnumConstructorsLowering,
        ::InitializersLowering,

        ::InteropBridgesNameInventor,
        ::NativeInventNamesForLocalClasses,
        ::NativeKlibInventNamesForLocalFunctions,

        ::LocalDelegatedPropertiesLowering,
        ::NativeLocalDeclarationsLowering,
        ::LocalDeclarationPopupLowering,

        ::NativeTailrecLowering,
        ::NativeFinallyBlocksLowering,
        ::ComputeTypesPass, // Inliner erases generics. Trying to restore some of the information and simplify IR.
        ::NativeForLoopsLowering, // TODO: depends on FunctionsWithoutBoundCheckGenerator

        ::FlattenStringConcatenationLowering,
        ::StringConcatenationLowering,
        ::StringConcatenationTypeNarrowing.takeIf { this.optimizationsEnabled },

        ::NativeDefaultArgumentStubGenerator,
        ::NativeDefaultParameterCleaner,
        ::NativeDefaultParameterInjector,

        ::InnerClassLowering,
        ::DataClassOperatorsLowering,
        ::IfNullExpressionsFusionLowering,
        ::StaticCallableReferenceOptimization,

        ::NativeEnumWhenLowering,
        ::EnumClassLowering,
        ::EnumUsageLowering,

        ::VarargInjectionLowering, // TODO: depends on FunctionsWithoutBoundCheckGenerator
        ::KotlinNothingValueExceptionLowering,

        ::NativeSuspendFunctionsLowering,
        ::AddContinuationToNonLocalSuspendFunctionsLowering,
        ::NativeAddContinuationToFunctionCallsLowering,
        ::AddFunctionSupertypeToSuspendFunctionLowering,
        // Either of these could be turned off without losing correctness.
        ::CoroutinesLivenessAnalysis, // This is more optimal
        ::CoroutinesLivenessAnalysisFallback, // While this is simple

        ::ExpressionBodyTransformer,
        ::ObjectClassLowering,
        ::StaticInitializersLowering,

        // Running 2nd time not only helps the following heavy analysis but also corrects some lowerings' inaccuracies in IR types.
        ::ComputeTypesPass,
        ::RemoveCastsFromNothingLowering,
        ::CastsOptimization.takeIf { this.genericSafeCasts },

        ::TypeOperatorLowering,
        ::BuiltinOperatorLowering,

        ::BridgesBuilding,
        ::WorkersBridgesBuilding,

        ::ExportCachesAbiVisitor.takeIf { this.produce.isCache },
        ::ImportCachesAbiTransformer,

        ::GenericCallsReturnTypeEraser,
        ::Autoboxing,
        ::ConstructorsLowering,
        ::ReturnsInsertionLowering,
        ::CastsLowering.takeUnless { this.optimizationsEnabled },
)

private fun createFileLoweringPhase(
        name: String,
        lowering: (NativeGenerationState) -> FileLoweringPass,
        prerequisite: Set<NamedCompilerPhase<*, *, *>> = emptySet(),
) = createFileLoweringPhaseImpl(
        name,
        prerequisite
) { context, irFile ->
    lowering(context).lower(irFile)
}

private fun createFileLoweringPhase(
        lowering: (NativeBackendContext) -> FileLoweringPass,
        name: String,
        prerequisite: Set<NamedCompilerPhase<*, *, *>> = emptySet(),
) = createFileLoweringPhaseImpl(
        name,
        prerequisite
) { context, irFile ->
    lowering(context.context).lower(irFile)
}

private fun createFileLoweringPhaseImpl(
        name: String,
        prerequisite: Set<NamedCompilerPhase<*, *, *>>,
        op: (NativeGenerationState, IrFile) -> Unit
): NamedCompilerPhase<NativeGenerationState, IrFile, IrFile> = createSimpleNamedCompilerPhase(
        name,
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        prerequisite = prerequisite,
        outputIfNotEnabled = { _, _, _, irFile -> irFile },
        op = { context, irFile ->
            try {
                op(context, irFile)
            } catch (e: CompilationException) {
                e.initializeFileDetails(irFile)
                throw e
            } catch (e: KotlinExceptionWithAttachments) {
                throw e
            } catch (e: Throwable) {
                throw e.wrapWithCompilationException("Internal error in file lowering", irFile, null)
            }

            irFile
        }
)
