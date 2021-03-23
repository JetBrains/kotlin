/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.lower

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.DescriptorsRemapper
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * This symbol remapper is aware of possible wrapped descriptor ownership change to align
 * function signature and descriptor signature in cases of composable value parameters.
 * As wrapped descriptors are bound to IR functions inside, we need to create a new one to change
 * the function this descriptor represents as well.
 *
 * E.g. when function has a signature of:
 * ```
 * fun A(@Composable f: () -> Unit)
 * ```
 * it is going to be converted to incompatible signature of:
 * ```
 * fun A(f: (Composer<*>, Int) -> Unit)
 * ```
 * Same applies for receiver and return types.
 *
 * After remapping them, the newly created descriptors are bound back using
 * [WrappedComposableDescriptorPatcher] right after IR counterparts are created
 * (see usages in [ComposerTypeRemapper])
 *
 * This conversion is only required with decoys, but can be applied to the JVM as well for
 * consistency.
 */
class ComposableSymbolRemapper : DeepCopySymbolRemapper(
    object : DescriptorsRemapper {
    }
)

@OptIn(ObsoleteDescriptorBasedAPI::class)
object WrappedComposableDescriptorPatcher : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitConstructor(declaration: IrConstructor) {
        super.visitConstructor(declaration)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        super.visitSimpleFunction(declaration)
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        super.visitValueParameter(declaration)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter) {
        super.visitTypeParameter(declaration)
    }
}
