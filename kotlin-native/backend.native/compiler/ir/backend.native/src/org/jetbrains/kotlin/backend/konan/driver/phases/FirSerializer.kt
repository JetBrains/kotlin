/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.fir2IrSerializer
import org.jetbrains.kotlin.backend.konan.firSerializer

internal data class FirSerializerInput(
    val firToIrOutput: Fir2IrOutput,
    val produceHeaderKlib: Boolean = false,
)

internal val FirSerializerPhase = createSimpleNamedCompilerPhase<PhaseContext, FirOutput, SerializerOutput?>(
        "FirSerializer",
        outputIfNotEnabled = { _, _, _, _ -> SerializerOutput(null, null, listOf()) }
) { context: PhaseContext, input: FirOutput ->
    context.firSerializer(input)
}

internal val Fir2IrSerializerPhase = createSimpleNamedCompilerPhase<PhaseContext, FirSerializerInput, SerializerOutput>(
        "Fir2IrSerializer",
        outputIfNotEnabled = { _, _, _, _ -> SerializerOutput(null, null, listOf()) }
) { context: PhaseContext, input: FirSerializerInput ->
    context.fir2IrSerializer(input)
}

internal fun <T : PhaseContext> PhaseEngine<T>.runFirSerializer(
        firOutput: FirOutput
): SerializerOutput? {
    return this.runPhase(FirSerializerPhase, firOutput)
}

internal fun <T : PhaseContext> PhaseEngine<T>.runFir2IrSerializer(
        firSerializerInput: FirSerializerInput
): SerializerOutput {
    return this.runPhase(Fir2IrSerializerPhase, firSerializerInput)
}