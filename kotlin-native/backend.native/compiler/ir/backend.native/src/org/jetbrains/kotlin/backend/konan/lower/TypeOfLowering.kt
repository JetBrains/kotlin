/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

internal class TypeOfLowering(val context: Context) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) = lower(irBody, container, container.file)

    fun lower(irBody: IrBody, container: IrDeclaration, irFile: IrFile) {
        irBody.transformChildren(object : IrElementTransformer<IrBuilderWithScope> {
            override fun visitDeclaration(declaration: IrDeclarationBase, data: IrBuilderWithScope) =
                    super.visitDeclaration(declaration,
                            data = (declaration as? IrSymbolOwner)?.let { context.createIrBuilder(it.symbol, it.startOffset, it.endOffset) }
                                    ?: data
                    )

            override fun visitCall(expression: IrCall, data: IrBuilderWithScope): IrExpression {
                expression.transformChildren(this, data)

                return when {
                    Symbols.isTypeOfIntrinsic(expression.symbol) -> {
                        with (KTypeGenerator(context, irFile, expression, needExactTypeParameters = true)) {
                            data.at(expression).irKType(expression.getTypeArgument(0)!!, leaveReifiedForLater = true)
                        }
                    }
                    else -> expression
                }
            }
        }, data = context.createIrBuilder((container as IrSymbolOwner).symbol, irBody.startOffset, irBody.endOffset))
    }
}