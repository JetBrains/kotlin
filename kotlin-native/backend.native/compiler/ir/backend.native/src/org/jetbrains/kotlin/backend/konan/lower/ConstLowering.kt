/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.InlineConstTransformer
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class ConstLowering(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(NativeInlineConstTransformer())
    }
}

private class NativeInlineConstTransformer : InlineConstTransformer() {
    override val IrField.constantInitializer get() =
        (initializer?.expression as? IrConst<*>)
                ?.takeIf { correspondingPropertySymbol?.owner?.isConst == true }
                // NaN constants has inconsistencies between IR and metadata representation,
                // so inlining them can lead to incorrect behaviour. Check KT-53258 for details.
                ?.takeUnless { it.kind == IrConstKind.Double && IrConstKind.Double.valueOf(it).isNaN() }
                ?.takeUnless { it.kind == IrConstKind.Float && IrConstKind.Float.valueOf(it).isNaN() }

    override fun reportInlineConst(field: IrField, value: IrConst<*>) {}
}