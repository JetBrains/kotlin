/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.driver

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.Fir2IrInput
import org.jetbrains.kotlin.backend.Fir2IrOutput
import org.jetbrains.kotlin.backend.common.phaser.PhaseContext
import org.jetbrains.kotlin.backend.fir2Ir

internal val Fir2IrPhase = createSimpleNamedCompilerPhase(
        "Fir2Ir",
        outputIfNotEnabled = { _, _, _, _ -> error("Fir2Ir phase cannot be disabled") }
) { context: PhaseContext, input: Fir2IrInput ->
    context.fir2Ir(input)
}

fun <T : PhaseContext> PhaseEngine<T>.runFir2Ir(input: Fir2IrInput): Fir2IrOutput {
    return this.runPhase(Fir2IrPhase, input)
}
