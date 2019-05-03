/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.plugins.kotlin

import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.extensions.IrLoweringExtension
import org.jetbrains.kotlin.ir.declarations.IrFile
import androidx.compose.plugins.kotlin.compiler.lower.ComposeFcsPatcher
import androidx.compose.plugins.kotlin.compiler.lower.ComposeObservePatcher
import androidx.compose.plugins.kotlin.frames.FrameIrTransformer

val ComposeObservePhase = makeIrFilePhase(
    ::ComposeObservePatcher,
    name = "ComposeObservePhase",
    description = "Observe @Model"
)

val FrameClassGenPhase = makeIrFilePhase(
    ::FrameIrTransformer,
    name = "ComposeFrameTransformPhase",
    description = "Transform @Model classes into framed classes"
)

val ComposeFcsPhase = makeIrFilePhase(
    ::ComposeFcsPatcher,
    name = "ComposeFcsPhase",
    description = "Rewrite FCS descriptors to IR bytecode"
)

class ComposeIrLoweringExtension : IrLoweringExtension {
    override fun interceptLoweringPhases(
        phases: CompilerPhase<JvmBackendContext, IrFile, IrFile>
    ): CompilerPhase<JvmBackendContext, IrFile, IrFile> {
        return FrameClassGenPhase then ComposeObservePhase then ComposeFcsPhase then phases
    }
}
