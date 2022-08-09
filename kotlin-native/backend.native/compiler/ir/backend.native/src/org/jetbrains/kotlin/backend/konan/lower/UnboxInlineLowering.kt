/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION
import org.jetbrains.kotlin.backend.konan.getUnboxFunction
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * In case the body of <T-unbox> is exactly RETURN(GET_FIELD(IrExpression, backing_field)), it is inlined.
 * So, the snippets `CALL 'public final fun <T-unbox> (IrExpression)` are transformed to 'GET_FIELD(IrExpression, backing_field)'.
 */
internal class UnboxInlineLowering(
        private val context: CommonBackendContext,
) : BodyLoweringPass {

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(AccessorInliner(context))
    }
}

private class AccessorInliner(commonBackendContext: CommonBackendContext) : IrElementTransformerVoid() {

    private val context = commonBackendContext as Context
    private val anyType = context.irBuiltIns.anyType

    private fun IrFunction.isEasyInlineableUnbox(): Boolean =
            origin == DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION &&
                    name.asString().endsWith("-unbox>") &&
                    !returnType.isNullable()

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)

        return if (expression.symbol.owner.isEasyInlineableUnbox())
            tryInlineUnbox(expression) ?: expression
        else expression
    }

    private fun tryInlineUnbox(call: IrCall): IrExpression? {
        val returnClass = call.type.getClass()!!
        val singleStatement = context.getUnboxFunction(returnClass).body?.statements?.singleOrNull()
        return if (singleStatement is IrReturn) {
            val retVal = singleStatement.value
            if (retVal is IrGetField) {
                // Boxed primitive types (Int, Short,..) have `value` field
                // Inline unsigned classes (UInt, UShort,..) have `data` field
                val field = retVal.symbol.owner
                context.createIrBuilder(call.symbol, call.startOffset, call.endOffset).irGetField(call.getValueArgument(0), field)
            } else {
                context.log { "Cannot inline unbox function ${call.symbol} with body `IrReturn(expression)`, where `expression` is not IrGetField(...)" }
                null
            }
        } else {
            context.log { "Cannot inline unbox function ${call.symbol} with body which is not IrReturn(IrGetField(...))" }
            null
        }
    }
}
