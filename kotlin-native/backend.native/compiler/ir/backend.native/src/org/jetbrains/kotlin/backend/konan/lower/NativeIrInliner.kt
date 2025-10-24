/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolver
import org.jetbrains.kotlin.ir.inline.InlineMode
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.isVirtualCall

internal class NativePrivateFunctionInlining(generationState: NativeGenerationState) : FunctionInlining(
        context = generationState.context,
        NativeInlineFunctionResolver(generationState, inlineMode = InlineMode.PRIVATE_INLINE_FUNCTIONS),
)

internal class NativeAllFunctionInlining(generationState: NativeGenerationState) : FunctionInlining(
        context = generationState.context,
        NativeInlineFunctionResolver(generationState, inlineMode = InlineMode.ALL_INLINE_FUNCTIONS),
)

internal class NativePreCodegenFunctionInlining(context: Context, functionsToInline: Set<IrFunction>) : FunctionInlining(
        context,
        inlineFunctionResolver = object : InlineFunctionResolver() {
            override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
                return symbol.owner.takeIf { it in functionsToInline }
            }

            override fun shouldSkipBecauseOfCallSite(expression: IrMemberAccessExpression<IrFunctionSymbol>): Boolean {
                return expression is IrCall && expression.isVirtualCall
            }
        },
)
