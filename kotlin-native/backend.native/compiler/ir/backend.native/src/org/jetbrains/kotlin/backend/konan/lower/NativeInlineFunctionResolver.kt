/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.getCompilerMessageLocation
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.inline.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.library.isHeader

private var IrFunction.wasLowered: Boolean? by irAttribute(followAttributeOwner = true)

internal class NativeInlineFunctionResolver(
        context: Context,
        inlineMode: InlineMode,
) : InlineFunctionResolverReplacingCoroutineIntrinsics<Context>(context, inlineMode) {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val function = super.getFunctionDeclaration(symbol) ?: return null

        if (function.body != null) {
            // TODO this `if` check can be dropped after KT-72441
            if (function.getPackageFragment().konanLibrary?.isHeader == true && function.wasLowered != true) {
                lower(function)
                function.wasLowered = true
            }
            return function
        }

        context.getInlineFunctionDeserializer(function).deserializeInlineFunction(function)
        lower(function)

        return function
    }

    private fun lower(function: IrFunction) {
        val body = function.body ?: return

        UpgradeCallableReferences(context).lower(function)

        NativeAssertionWrapperLowering(context).lower(function)

        LateinitLowering(context).lower(body)

        SharedVariablesLowering(context).lower(body, function)

        LocalClassesInInlineLambdasLowering(context).lower(body, function)

        ArrayConstructorLowering(context).lower(body, function)

        NativeIrInliner(context, inlineMode = InlineMode.PRIVATE_INLINE_FUNCTIONS).lower(function)
        OuterThisInInlineFunctionsSpecialAccessorLowering(context).lowerWithoutAddingAccessorsToParents(function)
        SyntheticAccessorLowering(context).lowerWithoutAddingAccessorsToParents(function)
    }

    override val callInlinerStrategy: CallInlinerStrategy = NativeCallInlinerStrategy()

    inner class NativeCallInlinerStrategy : CallInlinerStrategy {
        private lateinit var builder: NativeRuntimeReflectionIrBuilder
        override fun at(scope: Scope, expression: IrExpression) {
            val symbols = this@NativeInlineFunctionResolver.context.ir.symbols
            builder = context.createIrBuilder(scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
                    .toNativeRuntimeReflectionBuilder(symbols) { message ->
                        this@NativeInlineFunctionResolver.context.reportCompilationError(message, getCompilerMessageLocation())
                    }
        }
        override fun postProcessTypeOf(expression: IrCall, nonSubstitutedTypeArgument: IrType): IrExpression {
            return builder.irKType(nonSubstitutedTypeArgument)
        }
    }
}
