/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.konan.DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.utils.atMostOne

internal fun IrFunction.isBoxOrUnbox(): Boolean =
        origin == DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION
                && name.asString().let { it.endsWith("-box>") || it.endsWith("-unbox>") }

internal fun IrFunction.isUnbox(): Boolean =
        origin == DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION
                && name.asString().endsWith("-unbox>")

internal fun IrFunction.isBox(): Boolean =
        origin == DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION
                && name.asString().endsWith("-box>")

internal fun IrExpression.isBoxOrUnboxCall() = this is IrCall && symbol.owner.isBoxOrUnbox()

internal val IrCall.actualCallee: IrSimpleFunction
    get() {
        val callee = symbol.owner
        return (this.superQualifierSymbol?.owner?.getOverridingOf(callee) ?: callee).target
    }

private fun IrClass.getOverridingOf(function: IrFunction) = (function as? IrSimpleFunction)?.let {
    it.allOverriddenFunctions.atMostOne { it.parent == this }
}
