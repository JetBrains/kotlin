/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.common.serialization.SerializerOutput
import org.jetbrains.kotlin.native.FirOutput
import org.jetbrains.kotlin.native.FirSerializerInput
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.native.fir2IrSerializer
import org.jetbrains.kotlin.native.firSerializer

internal val FirSerializerPhase: NamedCompilerPhase<PhaseContext, FirOutput, SerializerOutput<KonanLibrary>?> = createSimpleNamedCompilerPhase(
        "FirSerializer",
        outputIfNotEnabled = { _, _, _, _ -> SerializerOutput(null, null, listOf()) }
) { context: PhaseContext, input: FirOutput ->
    context.firSerializer(input)
}

internal val Fir2IrSerializerPhase: NamedCompilerPhase<PhaseContext, FirSerializerInput, SerializerOutput<KonanLibrary>> = createSimpleNamedCompilerPhase(
        "Fir2IrSerializer",
        outputIfNotEnabled = { _, _, _, _ -> SerializerOutput(null, null, listOf()) }
) { context: PhaseContext, input: FirSerializerInput ->
    context.fir2IrSerializer(input)
}

internal fun <T : PhaseContext> PhaseEngine<T>.runFirSerializer(
        firOutput: FirOutput
): org.jetbrains.kotlin.backend.konan.serialization.SerializerOutput? {
    return this.runPhase(FirSerializerPhase, firOutput)
}

internal fun <T : PhaseContext> PhaseEngine<T>.runFir2IrSerializer(
        firSerializerInput: FirSerializerInput
): org.jetbrains.kotlin.backend.konan.serialization.SerializerOutput {
    return this.runPhase(Fir2IrSerializerPhase, firSerializerInput)
}