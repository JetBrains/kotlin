/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.common.serialization.SerializerOutput
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.native.writeKlib
import org.jetbrains.kotlin.native.KlibWriterInput

internal val WriteKlibPhase = createSimpleNamedCompilerPhase<PhaseContext, KlibWriterInput>(
        "WriteKlib",
) { context, input -> context.writeKlib(input) }

internal fun <T : PhaseContext> PhaseEngine<T>.writeKlib(
        serializationOutput: SerializerOutput,
        outputPath: String,
        produceHeaderKlib: Boolean = false,
) {
    this.runPhase(WriteKlibPhase, KlibWriterInput(serializationOutput, outputPath, produceHeaderKlib))
}
