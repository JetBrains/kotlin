/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower


import org.jetbrains.kotlin.backend.common.FileLoweringPass

import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.*


/**
 * Boxes and unboxes values of value types when necessary.
 */
internal class InlineClassPropertyAccessorsLowering(val context: Context) : FileLoweringPass {

    private val transformer = InlineClassAccessorsTransformer(context)

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(transformer)
    }

}

private class InlineClassAccessorsTransformer(private val context: Context) : IrBuildingTransformer(context) {

    private val symbols = context.ir.symbols

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)

        val property = expression.symbol.owner.correspondingPropertySymbol?.owner ?: return expression

        property.parent.let {
            if (it is IrClass && it.isInline && property.backingField != null) {
                expression.dispatchReceiver?.let { receiver ->
                    return builder.at(expression)
                            .irCall(symbols.reinterpret, expression.type, listOf(receiver.type, expression.type))
                            .apply {
                                extensionReceiver = receiver
                            }
                }
            }
        }

        return expression
    }
}
