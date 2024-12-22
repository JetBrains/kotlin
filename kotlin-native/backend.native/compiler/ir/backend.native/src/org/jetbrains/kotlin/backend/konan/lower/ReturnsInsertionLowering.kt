/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.isNullableNothing
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.innerInlinedBlockOrThis
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Generates [IrReturn]s for `Unit`-returning functions.
 */
internal class ReturnsInsertionLowering(val context: Context) : FileLoweringPass {
    private val symbols = context.ir.symbols

    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                declaration.acceptChildrenVoid(this)

                val body = declaration.body ?: return
                body as IrBlockBody
                context.createIrBuilder(declaration.symbol, declaration.endOffset, declaration.endOffset).run {
                    if (declaration.returnType == context.irBuiltIns.unitType) {
                        body.statements += irReturn(irCall(symbols.theUnitInstance, context.irBuiltIns.unitType))
                    } else if (declaration.returnType.isNullable()) {
                        // this is a workaround for KT-42832
                        val typeOperatorCall = body.statements.lastOrNull() as? IrTypeOperatorCall
                        if (typeOperatorCall?.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
                                && typeOperatorCall.argument.type.isNullableNothing()) {
                            body.statements[body.statements.lastIndex] = irReturn(typeOperatorCall.argument)
                        }
                    }
                }
            }
        })
    }
}
