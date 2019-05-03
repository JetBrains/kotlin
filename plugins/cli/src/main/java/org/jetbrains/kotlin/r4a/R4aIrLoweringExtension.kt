/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
