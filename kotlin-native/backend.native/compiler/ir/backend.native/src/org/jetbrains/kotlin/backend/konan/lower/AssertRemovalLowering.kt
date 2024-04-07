/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.util.inlineFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class AssertRemovalLowering(val context: Context) : BodyLoweringPass {
    private val asserts = context.ir.symbols.asserts.toSet()

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitReturnableBlock(expression: IrReturnableBlock): IrExpression {
                val inlinedFunction = expression.inlineFunction ?: return super.visitReturnableBlock(expression)
                if (inlinedFunction.symbol in asserts) {
                    return IrCompositeImpl(expression.startOffset, expression.endOffset, expression.type)
                }
                return super.visitReturnableBlock(expression)
            }
        })
    }
}