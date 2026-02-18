/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class RemoveCastsFromNothingLowering(val context: Context) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        container.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                expression.transformChildrenVoid(this)
                if (expression.argument.type.isNothing()) {
                    return expression.argument
                }

                return expression
            }
        })
    }
}