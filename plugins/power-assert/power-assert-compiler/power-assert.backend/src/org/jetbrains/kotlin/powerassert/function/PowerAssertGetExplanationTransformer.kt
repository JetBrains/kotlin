/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.callableId
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.powerassert.PowerAssertNames.POWER_ASSERT_EXPLANATION_GETTER_CALLABLE_ID

/**
 * Replaces all calls to `PowerAssert.explanation` with either `null` or a parameter access.
 */
class PowerAssertGetExplanationTransformer(
    private val builtIns: PowerAssertBuiltIns,
    private val parameter: IrValueParameter?,
) : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        return when (expression.symbol.owner.callableId) {
            POWER_ASSERT_EXPLANATION_GETTER_CALLABLE_ID -> when (parameter) {
                null -> IrConstImpl.constNull(expression.startOffset, expression.endOffset, builtIns.callExplanationType.makeNullable())
                else -> {
                    val callee = builtIns.function0invoke
                    IrCallImpl(
                        expression.startOffset, expression.endOffset,
                        type = builtIns.callExplanationType,
                        symbol = callee.symbol,
                        typeArgumentsCount = callee.typeParameters.size,
                        origin = null
                    ).apply {
                        dispatchReceiver = IrGetValueImpl(expression.startOffset, expression.endOffset, parameter.type, parameter.symbol)
                    }
                }
            }
            else -> super.visitCall(expression)
        }
    }
}
