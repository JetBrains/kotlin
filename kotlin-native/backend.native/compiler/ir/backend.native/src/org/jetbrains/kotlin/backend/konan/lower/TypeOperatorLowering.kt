/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.ir.containsNull
import org.jetbrains.kotlin.backend.konan.ir.isSubtypeOf
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.isSimpleTypeWithQuestionMark
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal fun IrType.erasureForTypeOperation(): IrType {
    if (this !is IrSimpleType) return this

    return when (val classifier = classifier) {
        is IrClassSymbol -> this
        is IrTypeParameterSymbol -> {
            val upperBound = classifier.owner.superTypes.firstOrNull()
                    ?: TODO("${classifier.descriptor} : ${classifier.descriptor.upperBounds}")

            if (this.hasQuestionMark) {
                // `T?`
                upperBound.erasureForTypeOperation().makeNullable()
            } else {
                upperBound.erasureForTypeOperation()
            }
        }
        else -> TODO(classifier.toString())
    }
}

internal class TypeOperatorLowering(val context: CommonBackendContext) : FileLoweringPass, IrBuildingTransformer(context) {

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(this, null)
    }

    private fun IrType.erasure(): IrType = this.erasureForTypeOperation()

    private fun lowerCast(expression: IrTypeOperatorCall): IrExpression {
        builder.at(expression)
        val typeOperand = expression.typeOperand.erasure()

//        assert (!TypeUtils.hasNullableSuperType(typeOperand)) // So that `isNullable()` <=> `isMarkedNullable`.

        // TODO: consider the case when expression type is wrong e.g. due to generics-related unchecked casts.

        return when {
            expression.argument.type.isSubtypeOf(typeOperand) -> expression.argument

            expression.argument.type.containsNull() -> {
                with(builder) {
                    irLetS(expression.argument) { argument ->
                        irIfThenElse(
                                type = expression.type,
                                condition = irEqeqeq(irGet(argument.owner), irNull()),

                                thenPart = if (typeOperand.isSimpleTypeWithQuestionMark)
                                    irNull()
                                else
                                    irCall(this@TypeOperatorLowering.context.ir.symbols.throwNullPointerException.owner),

                                elsePart = irAs(irGet(argument.owner), typeOperand.makeNotNull())
                        )
                    }
                }
            }

            typeOperand.isSimpleTypeWithQuestionMark -> builder.irAs(expression.argument, typeOperand.makeNotNull())

            typeOperand == expression.typeOperand -> expression

            else -> builder.irAs(expression.argument, typeOperand)
        }
    }

    private fun lowerSafeCast(expression: IrTypeOperatorCall): IrExpression {
        val typeOperand = expression.typeOperand.erasure()

        return builder.irBlock(expression) {
            +irLetS(expression.argument) { variable ->
                irIfThenElse(expression.type,
                        condition = irIs(irGet(variable.owner), typeOperand),
                        thenPart = irImplicitCast(irGet(variable.owner), typeOperand),
                        elsePart = irNull())
            }
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        expression.transformChildrenVoid(this)

        return when (expression.operator) {
            IrTypeOperator.SAFE_CAST -> lowerSafeCast(expression)
            IrTypeOperator.CAST -> lowerCast(expression)
            else -> expression
        }
    }
}
