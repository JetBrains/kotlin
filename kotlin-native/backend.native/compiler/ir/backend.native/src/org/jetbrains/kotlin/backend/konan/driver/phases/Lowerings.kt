/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.FunctionInlining
import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.ImportCachesAbiTransformer
import org.jetbrains.kotlin.backend.konan.lower.InlineClassPropertyAccessorsLowering
import org.jetbrains.kotlin.backend.konan.lower.RedundantCoercionsCleaner
import org.jetbrains.kotlin.backend.konan.lower.ReturnsInsertionLowering
import org.jetbrains.kotlin.backend.konan.lower.UnboxInlineLowering

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

private val validateAll = false

private val fileLoweringActions: Set<Action<IrFile, NativeGenerationState>> = setOfNotNull(
        nativeStateDumper,
        nativeStateIrValidator.takeIf { validateAll }
)

internal val modulePhaseActions: Set<Action<IrModuleFragment, NativeGenerationState>> = setOfNotNull(
        nativeStateDumper,
        nativeStateIrValidator.takeIf { validateAll }
)

internal val ReturnsInsertionPhase = createSimpleNamedCompilerPhase(
        name = "ReturnsInsertion",
        description = "Returns insertion for Unit functions",
        postactions = fileLoweringActions,
        //prerequisite = setOf(autoboxPhase, coroutinesPhase, enumClassPhase), TODO: if there are no files in the module, this requirement fails.
        op = { context, irFile -> ReturnsInsertionLowering(context.context).lower(irFile) }
)

internal val InlineClassPropertyAccessorsPhase = createSimpleNamedCompilerPhase(
        name = "InlineClassPropertyAccessorsLowering",
        description = "Inline class property accessors",
        postactions = fileLoweringActions,
        op = { context, irFile -> InlineClassPropertyAccessorsLowering(context.context).lower(irFile) }
)

internal val RedundantCoercionsCleaningPhase = createSimpleNamedCompilerPhase(
        name = "RedundantCoercionsCleaning",
        description = "Redundant coercions cleaning",
        postactions = fileLoweringActions,
        op = { context, irFile -> RedundantCoercionsCleaner(context.context).lower(irFile) }
)

internal val PropertyAccessorInlinePhase = createSimpleNamedCompilerPhase(
        name = "PropertyAccessorInline",
        description = "Property accessor inline lowering",
        postactions = fileLoweringActions
) { context, irFile ->
    PropertyAccessorInlineLowering(context.context).lower(irFile)
}

internal val UnboxInlinePhase = createSimpleNamedCompilerPhase(
        name = "UnboxInline",
        description = "Unbox functions inline lowering",
        postactions = fileLoweringActions
) { context, irFile ->
    UnboxInlineLowering(context.context).lower(irFile)
}

private val InlinePhase = createFileLoweringPhase(
        lowering = { context ->
            object : FileLoweringPass {
                override fun lower(irFile: IrFile) {
                    FunctionInlining(context.context, NativeInlineFunctionResolver(context.context, context)).lower(irFile)
                }
            }
        },
        name = "Inline",
        description = "Functions inlining",
//        prerequisite = setOf(lowerBeforeInlinePhase, arrayConstructorPhase, extractLocalClassesFromInlineBodies)
)

private val DelegationPhase = createFileLoweringPhase(
        lowering = ::PropertyDelegationLowering,
        name = "Delegation",
        description = "Delegation lowering"
//        prerequisite = setOf(volatilePhase)
)

private val FunctionReferencePhase = createFileLoweringPhase(
        lowering = ::FunctionReferenceLowering,
        name = "FunctionReference",
        description = "Function references lowering",
//        prerequisite = setOf(delegationPhase, localFunctionsPhase) // TODO: make weak dependency on `testProcessorPhase`
)

private val InventNamesForLocalClasses = createFileLoweringPhase(
        lowering = ::NativeInventNamesForLocalClasses,
        name = "InventNamesForLocalClasses",
        description = "Invent names for local classes and anonymous objects",
)

private val UseInternalAbiPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile, IrFile>(
        name = "UseInternalAbi",
        description = "Use internal ABI functions to access private entities",
        outputIfNotEnabled = { _, _, _, irFile -> irFile },
) { context, file ->
    ImportCachesAbiTransformer(context).lower(file)
    file
}


private val ObjectClassesPhase = createFileLoweringPhase(
        lowering = ::ObjectClassLowering,
        name = "ObjectClasses",
        description = "Object classes lowering"
)

private val CoroutinesPhase = createFileLoweringPhase(
        lowering = { context ->
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
//        prerequisite = setOf(localFunctionsPhase, finallyBlocksPhase, kotlinNothingValueExceptionPhase)
)

private val InteropPhase = createFileLoweringPhase(
        lowering = ::InteropLowering,
        name = "Interop",
        description = "Interop lowering",
//        prerequisite = setOf(inlinePhase, localFunctionsPhase, functionReferencePhase)
)

private fun PhaseEngine<NativeGenerationState>.getAllLowerings() = listOf<AbstractNamedCompilerPhase<NativeGenerationState, IrFile, IrFile>>(
        convertToNativeGeneration(removeExpectDeclarationsPhase),
        convertToNativeGeneration(stripTypeAliasDeclarationsPhase),
        convertToNativeGeneration(lowerBeforeInlinePhase),
        convertToNativeGeneration(arrayConstructorPhase),
        convertToNativeGeneration(lateinitPhase),
        convertToNativeGeneration(sharedVariablesPhase),
        InventNamesForLocalClasses,
        convertToNativeGeneration(extractLocalClassesFromInlineBodies),
        convertToNativeGeneration(wrapInlineDeclarationsWithReifiedTypeParametersLowering),
        InlinePhase,
        convertToNativeGeneration(provisionalFunctionExpressionPhase),
        convertToNativeGeneration(postInlinePhase),
        convertToNativeGeneration(contractsDslRemovePhase),
        convertToNativeGeneration(annotationImplementationPhase),
        convertToNativeGeneration(rangeContainsLoweringPhase),
        convertToNativeGeneration(forLoopsPhase),
        convertToNativeGeneration(flattenStringConcatenationPhase),
        convertToNativeGeneration(foldConstantLoweringPhase),
        convertToNativeGeneration(computeStringTrimPhase),
        convertToNativeGeneration(stringConcatenationPhase),
        convertToNativeGeneration(stringConcatenationTypeNarrowingPhase),
        convertToNativeGeneration(enumConstructorsPhase),
        convertToNativeGeneration(initializersPhase),
        convertToNativeGeneration(localFunctionsPhase),
        convertToNativeGeneration(volatilePhase),
        convertToNativeGeneration(tailrecPhase),
        convertToNativeGeneration(defaultParameterExtentPhase),
        convertToNativeGeneration(innerClassPhase),
        convertToNativeGeneration(dataClassesPhase),
        convertToNativeGeneration(ifNullExpressionsFusionPhase),
        convertToNativeGeneration(testProcessorPhase),
        DelegationPhase,
        FunctionReferencePhase,
        convertToNativeGeneration(singleAbstractMethodPhase),
        convertToNativeGeneration(enumWhenPhase),
        convertToNativeGeneration(builtinOperatorPhase),
        convertToNativeGeneration(finallyBlocksPhase),
        convertToNativeGeneration(enumClassPhase),
        convertToNativeGeneration(enumUsagePhase),
        InteropPhase,
        convertToNativeGeneration(varargPhase),
        convertToNativeGeneration(kotlinNothingValueExceptionPhase),
        CoroutinesPhase,
        convertToNativeGeneration(typeOperatorPhase),
        convertToNativeGeneration(expressionBodyTransformPhase),
        ObjectClassesPhase,
        convertToNativeGeneration(constantInliningPhase),
        convertToNativeGeneration(staticInitializersPhase),
        convertToNativeGeneration(bridgesPhase),
        convertToNativeGeneration(exportInternalAbiPhase),
        UseInternalAbiPhase,
        convertToNativeGeneration(autoboxPhase),
)

/**
 * Simplify porting of lowerings from static driver (where lowerings were executed in one big Context)
 * to dynamic with NativeGenerationState. It can be done manual rewriting, but it is an enormous work.
 */
private fun PhaseEngine<NativeGenerationState>.convertToNativeGeneration(
        phase: SameTypeNamedCompilerPhase<Context, IrFile>,
): SimpleNamedCompilerPhase<NativeGenerationState, IrFile, IrFile> {
    return createSimpleNamedCompilerPhase(
            phase.name,
            phase.description,
            postactions = fileLoweringActions,
            outputIfNotEnabled = { _, _, _, irFile -> irFile },
            op = { context, irFile -> phase.phaseBody(phaseConfig, phaserState.changePhaserStateType(), context.context, irFile) }
    )
}

private fun createFileLoweringPhase(
        name: String,
        description: String,
        lowering: (NativeGenerationState) -> FileLoweringPass
): SimpleNamedCompilerPhase<NativeGenerationState, IrFile, IrFile> = createSimpleNamedCompilerPhase(
        name,
        description,
        postactions = fileLoweringActions,
        outputIfNotEnabled = { _, _, _, irFile -> irFile },
        op = { context, irFile ->
            lowering(context).lower(irFile)
            irFile
        }
)

