/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ir.inline
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.inline.InlineMode
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.util.isVirtualCall
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class NativePrivateFunctionInlining(generationState: NativeGenerationState) : FunctionInlining(
        context = generationState.context,
        NativeInlineFunctionResolver(generationState, inlineMode = InlineMode.PRIVATE_INLINE_FUNCTIONS),
)

internal class NativeAllFunctionInlining(generationState: NativeGenerationState) : FunctionInlining(
        context = generationState.context,
        NativeInlineFunctionResolver(generationState, inlineMode = InlineMode.ALL_INLINE_FUNCTIONS),
)

internal class PreCodegenFunctionInlining(val context: Context, val functionsToInline: Set<IrFunction>) {
    fun run(irFunction: IrSimpleFunction) {
        val irBuilder = context.createIrBuilder(irFunction.symbol)
        irFunction.body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid()

                val callee = expression.symbol.owner
                if (callee !in functionsToInline || expression.isVirtualCall) return expression
                val result = irBuilder.at(expression).irBlock {
                    val arguments = callee.parameters.mapIndexed { index, param ->
                        val arg = expression.arguments[index] ?: error("No argument at $index for ${param.render()}")
                        (arg as? IrGetValue)?.symbol?.owner ?: irTemporary(
                                value = arg,
                                nameHint = param.name.asString(),
                                arg.type,
                        ).apply {
                            parent = irFunction
                        }
                    }

                    +callee.inline(irFunction, arguments, moveBody = false)
                }
                return result.statements.singleOrNull() as? IrExpression ?: result
            }
        })
    }
}