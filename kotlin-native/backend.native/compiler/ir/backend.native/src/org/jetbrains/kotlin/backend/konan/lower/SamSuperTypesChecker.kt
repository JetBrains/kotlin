/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.Variance

internal class SamSuperTypesChecker(private val context: Context,
                                    private val irFile: IrFile,
                                    private val mode: Mode,
                                    private val recurse: Boolean) {
    enum class Mode {
        ERASE,
        THROW
    }

    private fun IrType.eraseProjections(owner: IrElement): IrType {
        if (this !is IrSimpleType) return this
        return buildSimpleType {
            this.classifier = this@eraseProjections.classifier
            this.nullability = this@eraseProjections.nullability
            this.annotations = this@eraseProjections.annotations
            this.arguments = this@eraseProjections.arguments.mapIndexed { index, argument ->
                when (argument) {
                    is IrStarProjection -> argument
                    is IrTypeProjection -> {
                        if (mode == Mode.THROW && argument.variance != Variance.INVARIANT) {
                            context.reportCompilationError(
                                    "Unexpected variance in super type argument: ${argument.variance} @$index", irFile, owner)
                        }
                        val newArgumentType = if (recurse) {
                            argument.type.eraseProjections(owner)
                        } else {
                            // See the explanation at the SamSuperTypesChecker constructor call sites.
                            argument.type
                        }
                        makeTypeProjection(newArgumentType, Variance.INVARIANT)
                    }
                }
            }
        }
    }

    fun run() {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                expression.transformChildrenVoid(this)

                if (expression.operator == IrTypeOperator.SAM_CONVERSION)
                    expression.typeOperand = expression.typeOperand.eraseProjections(expression)
                return expression
            }
        })
    }
}
