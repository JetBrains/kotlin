/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.common.wrapWithCompilationException
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.InitializersLowering
import org.jetbrains.kotlin.backend.konan.optimizations.NativeForLoopsLowering
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.config.phaser.AnyNamedPhase
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.inline.*
import org.jetbrains.kotlin.backend.konan.lower.NativeAssertionWrapperLowering
import org.jetbrains.kotlin.backend.konan.optimizations.CastsOptimization
import org.jetbrains.kotlin.backend.konan.optimizations.ComputeTypesPass
import org.jetbrains.kotlin.konan.config.NativeConfigurationKeys
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasureDynamicPhaseTime
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

internal typealias LoweringList = List<NamedCompilerPhase<NativeGenerationState, IrFile, IrFile>>
internal typealias ModuleLowering = NamedCompilerPhase<NativeGenerationState, IrModuleFragment, IrModuleFragment>

internal fun PhaseEngine<NativeGenerationState>.runLowerings(
        lowerings: LoweringList,
        module: IrModuleFragment,
) = runLowerings(lowerings, listOf(module))

internal fun PhaseEngine<NativeGenerationState>.runLowerings(
        lowerings: LoweringList,
        modules: List<IrModuleFragment>,
) {
    for (module in modules) {
        for (file in module.files) {
            context.fileLowerState = FileLowerState()
            lowerings.fold(file) { loweredFile, lowering ->
                context.performanceManager.tryMeasureDynamicPhaseTime(lowering.name, PhaseType.IrLowering) {
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

internal fun <Context : NativeLoweringContext> PhaseEngine<Context>.runModuleWisePhase(
        lowering: NamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment>,
        modules: List<IrModuleFragment>,
) {
    context.performanceManager.tryMeasureDynamicPhaseTime(lowering.name, PhaseType.IrLowering) {
        for (module in modules) {
            runPhase(lowering, module)
        }
    }
}

internal fun <Context : NativeLoweringContext> createNativePhases(vararg phases: ((Context) -> FileLoweringPass)?) =
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

internal fun NativeSecondStageCompilationConfig.getLoweringsAfterInlining() = getLoweringsAfterInlining(
        generateTestDumper = configuration.getNotNull(NativeConfigurationKeys.GENERATE_TEST_RUNNER) != TestRunnerKind.NONE,
        optimizationsEnabled = optimizationsEnabled,
        genericSafeCasts = genericSafeCasts,
        isCache = produce.isCache,
)

private fun getLoweringsAfterInlining(
        generateTestDumper: Boolean,
        optimizationsEnabled: Boolean,
        genericSafeCasts: Boolean,
        isCache: Boolean,
) = createNativePhases(
        ::ConstEvaluationLowering,
        ::ReifiedFunctionLowering,
        ::TypeOfProcessingLowering,
        ::NativeSharedVariablesPrimitiveBoxSpecializationLowering,
        ::InteropLowering,
        ::SpecialInteropIntrinsicsLowering,
        ::TestsInitializer,
        ::TestsDumper.takeIf { generateTestDumper },
        ::ExpectDeclarationsRemoveLowering,
        ::StripTypeAliasDeclarationsLowering,
        ::NativeAssertionRemoverLowering,
        ::VolatileFieldsLowering,
        ::DelegatedPropertyOptimizationLowering,
        ::PropertyReferenceLowering,
        ::NativeFunctionReferenceLowering,
        ::NativeSingleAbstractMethodLowering,
        ::PostInlineLowering,
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
        ::StringConcatenationTypeNarrowing.takeIf { optimizationsEnabled },

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
        ::CastsOptimization.takeIf { genericSafeCasts },

        ::TypeOperatorLowering,
        ::BuiltinOperatorLowering,

        ::BridgesBuilding,
        ::WorkersBridgesBuilding,

        ::ExportCachesAbiVisitor.takeIf { isCache },
        ::ImportCachesAbiTransformer,

        ::GenericCallsReturnTypeEraser,
        ::Autoboxing,
        ::ConstructorsLowering,
        ::ReturnsInsertionLowering,
        ::CastsLowering.takeUnless { optimizationsEnabled },
)

@TestOnly
internal fun getNativeLoweringPhaseListsForTests(
        generateTestDumper: Boolean,
        optimizationsEnabled: Boolean,
        genericSafeCasts: Boolean,
        isCache: Boolean,
): List<List<AnyNamedPhase>> = listOf(
        getLoweringsUpToAndIncludingSyntheticAccessors(),
        // Note: `inlineAllFunctionsPhase` is a physical barrier between the two lists (see how everything is called in TopLevelPhases.kt)
        // Thus there are no prerequisites on it.
        getLoweringsAfterInlining(
                generateTestDumper = generateTestDumper,
                optimizationsEnabled = optimizationsEnabled,
                genericSafeCasts = genericSafeCasts,
                isCache = isCache,
        ),
)
