/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.objcinterop.isObjCObjectType
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal fun IrType.isSuperClassCastTo(dstClass: IrClass): Boolean =
        dstClass.isAny() || (this.classifierOrNull !is IrTypeParameterSymbol // Due to unsafe casts, see unchecked_cast8.kt as an example.
                && this.isSubtypeOfClass(dstClass.symbol))

internal class CastsLowering(val context: Context) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val irBuilder = context.createIrBuilder(container.symbol)
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                expression.transformChildrenVoid(this)

                if (expression.operator != IrTypeOperator.CAST) return expression
                val dstClass = expression.typeOperand.getClass() ?: return expression
                if (dstClass.defaultType.isObjCObjectType()) return expression

                val srcType = expression.argument.type
                val isSuperClassCast = srcType.isSuperClassCastTo(dstClass)
                val isNullable = expression.typeOperand.isNullable()
                return when {
                    isSuperClassCast && isNullable -> expression.argument
                    isSuperClassCast && !isNullable ->
                        irBuilder.at(expression)
                                .irCall(context.symbols.checkNotNull!!, expression.typeOperand, listOf(expression.typeOperand))
                                .apply { arguments[0] = expression.argument }
                    else -> irBuilder.at(expression)
                            .irCall(context.symbols.downcast!!, expression.typeOperand, listOf(expression.typeOperand))
                            .apply {
                                arguments[0] = expression.argument
                                arguments[1] = IrClassReferenceImpl(
                                        startOffset, endOffset,
                                        context.symbols.nativePtrType,
                                        dstClass.symbol,
                                        expression.typeOperand.makeNotNull()
                                )
                                arguments[2] = irBuilder.irBoolean(isNullable)
                            }
                }
            }
        })
    }
}