/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.types.isNullableNothing
import org.jetbrains.kotlin.ir.util.isSimpleTypeWithQuestionMark
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal class ReturnsInsertionLowering(val context: Context) : FileLoweringPass {
    private val symbols = context.ir.symbols

    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFunction(declaration: IrFunction) {
                declaration.acceptChildrenVoid(this)

                val body = declaration.body ?: return
                body as IrBlockBody
                context.createIrBuilder(declaration.symbol, declaration.endOffset, declaration.endOffset).run {
                    if (declaration is IrConstructor || declaration.returnType == context.irBuiltIns.unitType) {
                        body.statements += irReturn(irGetObject(symbols.unit))
                    } else if (declaration.returnType.isSimpleTypeWithQuestionMark) {
                        // this is a workaround for KT-42832
                        val typeOperatorCall = body.statements.lastOrNull() as? IrTypeOperatorCall
                        if (typeOperatorCall?.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
                                && typeOperatorCall.argument.type.isNullableNothing()) {
                            body.statements[body.statements.lastIndex] = irReturn(typeOperatorCall.argument)
                        }
                    }
                }
            }

            override fun visitBlock(expression: IrBlock) {
                expression.acceptChildrenVoid(this)
                if (expression !is IrReturnableBlock) return
                if (expression.inlineFunctionSymbol?.owner?.returnType == context.irBuiltIns.unitType) {
                    val irBuilder = context.createIrBuilder(expression.symbol, expression.endOffset, expression.endOffset)
                    irBuilder.run {
                        expression.statements += irReturn(irGetObject(symbols.unit))
                    }
                }
            }
        })
    }
}