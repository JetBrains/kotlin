/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.firSerializer

internal val FirSerializerPhase = createSimpleNamedCompilerPhase<PhaseContext, Fir2IrOutput, SerializerOutput>(
        "FirSerializer", "Fir serializer",
        outputIfNotEnabled = { _, _, _, _ -> SerializerOutput(null, null, null, listOf()) }
) { context: PhaseContext, input: Fir2IrOutput ->
    context.firSerializer(input)
}

internal fun <T : PhaseContext> PhaseEngine<T>.runFirSerializer(
        fir2irOutput: Fir2IrOutput
): SerializerOutput {
    return this.runPhase(FirSerializerPhase, fir2irOutput)
}