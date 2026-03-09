/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * Replaces all calls within a function to itself or super to include explanation parameter,
 * so explanation is propagated to super function calls and recursive calls.
 */
class PowerAssertSelfCallTransformer(
    private val originalFunction: IrSimpleFunction,
    private val generatedFunction: IrSimpleFunction,
    private val explanation: IrValueParameter,
) : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        val call = when (expression.symbol) {
            generatedFunction.symbol -> {
                // Recursive calls will automatically be converted by 'deepCopyWithSymbols', but will not include the extra argument.
                expression.arguments.add(IrGetValueImpl(expression.startOffset, expression.endOffset, explanation.type, explanation.symbol))
                expression
            }
            in originalFunction.overriddenSymbols if expression.superQualifierSymbol != null -> {
                // Calls to super will *not* be converted by 'deepCopyWithSymbols', so those must be transformed manually.
                val powerAssertDispatchSymbol = expression.symbol.owner.powerAssertDispatchSymbol!!
                irCall(expression, powerAssertDispatchSymbol, newSuperQualifierSymbol = expression.superQualifierSymbol).apply {
                    arguments.add(IrGetValueImpl(expression.startOffset, expression.endOffset, explanation.type, explanation.symbol))
                }
            }
            else -> {
                expression
            }
        }

        call.transformChildren(this, null)
        return call
    }
}
