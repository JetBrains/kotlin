/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.extensions.IrLoweringExtension
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.r4a.compiler.lower.R4aFcsPatcher
import org.jetbrains.kotlin.r4a.compiler.lower.R4aObservePatcher
import org.jetbrains.kotlin.r4a.frames.FrameIrTransformer

val R4aObservePhase = makeIrFilePhase(
    ::R4aObservePatcher,
    name = "R4aObservePhase",
    description = "Observe @Model"
)

val FrameClassGenPhase = makeIrFilePhase(
    ::FrameIrTransformer,
    name = "R4aFrameTransformPhase",
    description = "Transform @Model classes into framed classes"
)

val R4aFcsPhase = makeIrFilePhase(
    ::R4aFcsPatcher,
    name = "R4aFcsPhase",
    description = "Rewrite FCS descriptors to IR bytecode"
)

class R4aIrLoweringExtension : IrLoweringExtension {
    override fun interceptLoweringPhases(
        phases: CompilerPhase<JvmBackendContext, IrFile, IrFile>
    ): CompilerPhase<JvmBackendContext, IrFile, IrFile> {
        return FrameClassGenPhase then R4aObservePhase then R4aFcsPhase then phases
    }
}
