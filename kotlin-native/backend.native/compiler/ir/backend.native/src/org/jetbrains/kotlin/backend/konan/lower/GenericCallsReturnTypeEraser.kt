/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.target
import org.jetbrains.kotlin.ir.util.eraseTypeParameters
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/*
 * FE substitutes types for generic calls, but by the end of the lowerings pipeline, the return types should be
 * replaced with their erasures to make some of the final passes (like Autoboxing and pre-codegen inlining) simpler.
 * The idea here is that by the end of the pipeline all the types are considered erased and a function call is just
 * a pure and simple function call with no conversions/coercions of the return type and the parameter types.
 */
internal class GenericCallsReturnTypeEraser(val context: Context) : BodyLoweringPass {
    private val reinterpret = context.ir.symbols.reinterpret.owner

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                expression.acceptChildrenVoid(this)

                val callee = expression.target
                if (callee != reinterpret && callee.returnType.classifierOrNull is IrTypeParameterSymbol) {
                    expression.type = callee.returnType.eraseTypeParameters()
                }
            }
        })
    }

}