/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.lower.FunctionReferenceLowering.Companion.isLoweredFunctionReference
import org.jetbrains.kotlin.ir.builders.irConstantObject
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Function references are lowered into instantion of on object of some "callable" type.
 * e.g. `call(::foo)` will be lowered to something like `call(foo$FUNCTION_REFERENCE$0())`.
 *
 * In some cases the object instantiated doesn't capture any context and in fact can be replaced with a constant object.
 * That's what this optimization pass does.
 */
internal class StaticFunctionReferenceOptimization(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transform(object : IrElementTransformerVoidWithContext() {
            override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                val constructor = expression.symbol.owner
                val constructedClass = constructor.constructedClass

                if (isLoweredFunctionReference(constructedClass) && expression.valueArgumentsCount == 0) {
                    val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol,
                            expression.startOffset, expression.endOffset)

                    return irBuilder.irConstantObject(constructedClass, emptyMap(), expression.getClassTypeArguments().map { it!! })
                } else {
                    return expression
                }
            }
        }, data = null)
    }
}