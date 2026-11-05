/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.NativeLoweringContext
import org.jetbrains.kotlin.backend.konan.getUnboxFunction
import org.jetbrains.kotlin.backend.konan.ir.isUnbox
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * In case the body of <T-unbox> is exactly RETURN(GET_FIELD(IrExpression, backing_field)), it is inlined.
 * So, the snippets `CALL 'public final fun <T-unbox> (IrExpression)` are transformed to 'GET_FIELD(IrExpression, backing_field)'.
 */
@PhasePrerequisites(RedundantCoercionsCleaner::class)
internal class UnboxInlineLowering(
        private val context: NativeLoweringContext,
) : BodyLoweringPass {

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(AccessorInliner(context))
    }
}

private class AccessorInliner(private val context: NativeLoweringContext) : IrElementTransformerVoid() {
    private fun IrFunction.isEasyInlineableUnbox(): Boolean = !returnType.isNullable() && isUnbox()

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)

        return if (expression.symbol.owner.isEasyInlineableUnbox())
            tryInlineUnbox(expression) ?: expression
        else expression
    }

    private fun tryInlineUnbox(call: IrCall): IrExpression? {
        val returnClass = call.type.getClass()!!
        val unbox = context.getUnboxFunction(returnClass)
        if (unbox.parameters.single().type.isNullable()) {
            context.log { "Cannot inline unbox function ${call.symbol} with nullable parameter" }
            return null
        }
        val generationState = (context as NativeGenerationState)
        val field = generationState.context.getLayoutBuilder(returnClass).getFields(generationState.llvm).single().irField!!
        return context.createIrBuilder(call.symbol, call.startOffset, call.endOffset).irGetField(call.arguments[0], field)
    }
}
