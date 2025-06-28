/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.driver

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.Fir2IrOutput
import org.jetbrains.kotlin.backend.FirOutput
import org.jetbrains.kotlin.backend.FrontendContext
import org.jetbrains.kotlin.backend.SerializerOutput
import org.jetbrains.kotlin.backend.fir2IrSerializer
import org.jetbrains.kotlin.backend.firSerializer

data class FirSerializerInput(
        val firToIrOutput: Fir2IrOutput,
        val produceHeaderKlib: Boolean = false,
)

val FirSerializerPhase = createSimpleNamedCompilerPhase(
        "FirSerializer",
        outputIfNotEnabled = { _, _, _, _ -> SerializerOutput(null, null, listOf()) }
) { context: FrontendContext, input: FirOutput ->
    context.firSerializer(input)
}

val Fir2IrSerializerPhase = createSimpleNamedCompilerPhase(
        "Fir2IrSerializer",
        outputIfNotEnabled = { _, _, _, _ -> SerializerOutput(null, null, listOf()) }
) { context: FrontendContext, input: FirSerializerInput ->
    context.fir2IrSerializer(input)
}

fun <T : FrontendContext> PhaseEngine<T>.runFirSerializer(
        firOutput: FirOutput
): SerializerOutput? {
    return this.runPhase(FirSerializerPhase, firOutput)
}

fun <T : FrontendContext> PhaseEngine<T>.runFir2IrSerializer(
        firSerializerInput: FirSerializerInput
): SerializerOutput {
    return this.runPhase(Fir2IrSerializerPhase, firSerializerInput)
}