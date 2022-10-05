/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.specialization.ir

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.specialization.common.shallowCopy

class MonomorphicCallsReplacer {
    private val replacement = mutableMapOf<IrCall, IrSimpleFunctionSymbol>()

    fun addReplacement(from: IrCall, to: IrSimpleFunctionSymbol) {
        replacement[from] = to
    }

    fun replaceInPlace(element: IrElement) {
        val replacer = object: IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val newCallSymbol = replacement[expression]
                val call = if (newCallSymbol != null) {
                    expression.shallowCopy(newSymbol = newCallSymbol)
                } else {
                    expression
                }
                call.transformChildrenVoid()
                return call
            }
        }
        element.transform(replacer, null)
    }
}