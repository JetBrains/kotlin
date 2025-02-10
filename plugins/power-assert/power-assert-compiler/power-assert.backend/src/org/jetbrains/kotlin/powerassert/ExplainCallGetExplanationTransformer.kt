/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.kotlinFqName

/**
 * Replaces all calls to `ExplainCall.explanation` with either `null` or a parameter access.
 */
class ExplainCallGetExplanationTransformer(
    private val builtIns: PowerAssertBuiltIns,
    private val parameter: IrValueParameter?,
) : IrElementTransformerVoidWithContext() {
    override fun visitExpression(expression: IrExpression): IrExpression {
        return when {
            isGetExplanation(expression) -> when (parameter) {
                null -> IrConstImpl.constNull(expression.startOffset, expression.endOffset, builtIns.callExplanationType.makeNullable())
                else -> IrGetValueImpl(expression.startOffset, expression.endOffset, parameter.type, parameter.symbol)
            }
            else -> super.visitExpression(expression)
        }
    }

    private fun isGetExplanation(expression: IrExpression): Boolean =
        (expression as? IrCall)?.symbol?.owner?.kotlinFqName == ExplainCallGetExplanation
}
