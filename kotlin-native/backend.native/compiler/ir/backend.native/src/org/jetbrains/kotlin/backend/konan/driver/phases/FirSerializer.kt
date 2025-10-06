/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.common.serialization.SerializerOutput
import org.jetbrains.kotlin.backend.konan.FirOutput
import org.jetbrains.kotlin.backend.konan.FirSerializerInput
import org.jetbrains.kotlin.backend.konan.driver.LightPhaseContext
import org.jetbrains.kotlin.backend.konan.fir2IrSerializer
import org.jetbrains.kotlin.backend.konan.firSerializer
import org.jetbrains.kotlin.konan.library.KonanLibrary

internal val FirSerializerPhase = createSimpleNamedCompilerPhase(
        "FirSerializer",
        outputIfNotEnabled = { _, _, _, _ -> SerializerOutput<KonanLibrary>(null, null, listOf()) }
) { context: LightPhaseContext, input: FirOutput ->
    context.firSerializer(input)
}

internal val Fir2IrSerializerPhase = createSimpleNamedCompilerPhase(
        "Fir2IrSerializer",
        outputIfNotEnabled = { _, _, _, _ -> SerializerOutput<KonanLibrary>(null, null, listOf()) }
) { context: LightPhaseContext, input: FirSerializerInput ->
    context.fir2IrSerializer(input)
}

internal fun <T : LightPhaseContext> PhaseEngine<T>.runFirSerializer(
        firOutput: FirOutput
): SerializerOutput<KonanLibrary>? {
    return this.runPhase(FirSerializerPhase, firOutput)
}

internal fun <T : LightPhaseContext> PhaseEngine<T>.runFir2IrSerializer(
        firSerializerInput: FirSerializerInput
): SerializerOutput<KonanLibrary> {
    return this.runPhase(Fir2IrSerializerPhase, firSerializerInput)
}