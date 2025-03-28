/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irImplicitCoercionToUnit
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.eraseTypeParameters
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/*
 * FE substitutes types for generic calls, but by the end of the lowerings pipeline, the return types should be
 * replaced with their erasures to make some of the final passes (like Autoboxing and pre-codegen inlining) simpler.
 * The idea here is that by the end of the pipeline all the types are considered erased and a function call is just
 * a pure and simple function call with no conversions/coercions of the return type and the parameter types.
 */
internal class GenericCallsReturnTypeEraser(val context: Context) : BodyLoweringPass {
    private val reinterpret = context.ir.symbols.reinterpret.owner

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val irBuilder = context.createIrBuilder(container.symbol)
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                val callee = expression.target
                if (callee != reinterpret && callee.returnType.classifierOrNull is IrTypeParameterSymbol) {
                    val actualType = callee.returnType.eraseTypeParameters()
                    val expectedType = expression.type
                    if (actualType != expectedType) {
                        expression.type = actualType
                        return when {
                            expectedType.isUnit() -> irBuilder.at(expression).irImplicitCoercionToUnit(expression)
                            expectedType.isNothing() -> expression
                            else -> irBuilder.at(expression).irImplicitCast(expression, expectedType)
                        }
                    }
                }

                return expression
            }
        })
    }
}