/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.cli.native.Fir2IrOutput
import org.jetbrains.kotlin.cli.native.FirOutput
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.cli.native.fir2Ir

internal val Fir2IrPhase = createSimpleNamedCompilerPhase(
        "Fir2Ir",
        outputIfNotEnabled = { _, _, _, _ -> error("Fir2Ir phase cannot be disabled") }
) { context: PhaseContext, input: FirOutput.Full ->
    context.fir2Ir(input)
}

internal fun <T : PhaseContext> PhaseEngine<T>.runFir2Ir(input: FirOutput.Full): Fir2IrOutput {
    return this.runPhase(Fir2IrPhase, input)
}
