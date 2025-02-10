/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.util.irCall

/**
 * Replaces all calls within a function to itself or super to include explanation parameter,
 * so explanation is propagated to super function calls and recursive calls.
 */
class ExplainedSelfCallTransformer(
    private val originalFunction: IrSimpleFunction,
    private val explanation: IrValueParameter,
) : IrElementTransformerVoidWithContext() {
    override fun visitCall(expression: IrCall): IrExpression {
        val call = if (expression.symbol == originalFunction.symbol || expression.symbol in originalFunction.overriddenSymbols) {
            val explainedDispatchSymbol = expression.symbol.owner.explainedDispatchSymbol!!
            irCall(expression, explainedDispatchSymbol, newSuperQualifierSymbol = expression.superQualifierSymbol).apply {
                arguments[explainedDispatchSymbol.owner.parameters.last()] =
                    IrGetValueImpl(expression.startOffset, expression.endOffset, explanation.type, explanation.symbol)
            }
        } else {
            expression
        }

        call.transformChildren(this, null)
        return call
    }
}
