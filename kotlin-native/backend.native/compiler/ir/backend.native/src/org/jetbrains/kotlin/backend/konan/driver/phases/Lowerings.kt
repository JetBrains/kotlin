/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.AbstractNamedCompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.SameTypeNamedCompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.changePhaserStateType
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.backend.konan.*

/**
 * Run whole IR lowering pipeline over [irModuleFragment].
 */
internal fun PhaseEngine<NativeGenerationState>.runAllLowerings(irModuleFragment: IrModuleFragment) {
    val lowerings = getAllLowerings()
    irModuleFragment.files.forEach { file ->
        context.fileLowerState = FileLowerState()
        lowerings.forEach {
            runPhase(it, file)
        }
    }
}

private fun PhaseEngine<NativeGenerationState>.getAllLowerings() = listOf<AbstractNamedCompilerPhase<NativeGenerationState, IrFile, Unit>>(
        convertToNativeGeneration(removeExpectDeclarationsPhase),
        convertToNativeGeneration(stripTypeAliasDeclarationsPhase),
        convertToNativeGeneration(lowerBeforeInlinePhase),
        convertToNativeGeneration(arrayConstructorPhase),
        convertToNativeGeneration(lateinitPhase),
        convertToNativeGeneration(sharedVariablesPhase),
        convertToNativeGeneration(inventNamesForLocalClasses),
        convertToNativeGeneration(extractLocalClassesFromInlineBodies),
        convertToNativeGeneration(wrapInlineDeclarationsWithReifiedTypeParametersLowering),
        convertToNativeGeneration(inlinePhase),
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
        convertToNativeGeneration(tailrecPhase),
        convertToNativeGeneration(defaultParameterExtentPhase),
        convertToNativeGeneration(innerClassPhase),
        convertToNativeGeneration(dataClassesPhase),
        convertToNativeGeneration(ifNullExpressionsFusionPhase),
        convertToNativeGeneration(testProcessorPhase),
        convertToNativeGeneration(delegationPhase),
        convertToNativeGeneration(functionReferencePhase),
        convertToNativeGeneration(singleAbstractMethodPhase),
        convertToNativeGeneration(enumWhenPhase),
        convertToNativeGeneration(builtinOperatorPhase),
        convertToNativeGeneration(finallyBlocksPhase),
        convertToNativeGeneration(enumClassPhase),
        convertToNativeGeneration(enumUsagePhase),
        convertToNativeGeneration(interopPhase),
        convertToNativeGeneration(varargPhase),
        convertToNativeGeneration(kotlinNothingValueExceptionPhase),
        convertToNativeGeneration(coroutinesPhase),
        convertToNativeGeneration(typeOperatorPhase),
        convertToNativeGeneration(expressionBodyTransformPhase),
        convertToNativeGeneration(objectClassesPhase),
        convertToNativeGeneration(constantInliningPhase),
        convertToNativeGeneration(staticInitializersPhase),
        convertToNativeGeneration(bridgesPhase),
        convertToNativeGeneration(exportInternalAbiPhase),
        convertToNativeGeneration(useInternalAbiPhase),
        convertToNativeGeneration(autoboxPhase),
)

/**
 * Simplify porting of lowerings from static driver (where lowerings were executed in one big Context)
 * to dynamic with NativeGenerationState. It can be done manual rewriting, but it is an enormous work.
 */
private fun PhaseEngine<NativeGenerationState>.convertToNativeGeneration(
        phase: SameTypeNamedCompilerPhase<Context, IrFile>
): SimpleNamedCompilerPhase<NativeGenerationState, IrFile, Unit> {
    return createSimpleNamedCompilerPhase(
            phase.name,
            phase.description,
            op = { context, irFile -> phase.phaseBody(phaseConfig, phaserState.changePhaserStateType(), context.context, irFile) }
    )
}

