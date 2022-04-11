/*
 * Copyright 2021 The Android Open Source Project
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isLambda
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class ComposeInlineLambdaLocator(private val context: IrPluginContext) {
    private val inlineLambdaToParameter = mutableMapOf<IrFunctionSymbol, IrValueParameter>()

    fun isInlineLambda(irFunction: IrFunction): Boolean =
        irFunction.symbol in inlineLambdaToParameter.keys

    fun preservesComposableScope(irFunction: IrFunction): Boolean =
        inlineLambdaToParameter[irFunction.symbol]?.let {
            !it.isCrossinline && !it.type.hasAnnotation(ComposeFqNames.DisallowComposableCalls)
        } ?: false

    // Locate all inline lambdas in the scope of the given IrElement.
    fun scan(element: IrElement) {
        element.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitValueParameter(declaration: IrValueParameter) {
                declaration.acceptChildrenVoid(this)
                val parent = declaration.parent as? IrFunction
                if (parent?.isInlineFunctionCall(context) == true &&
                    declaration.isInlineParameter()) {
                    declaration.defaultValue?.expression?.unwrapInlineLambda()?.let {
                        inlineLambdaToParameter[it] = declaration
                    }
                }
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
                expression.acceptChildrenVoid(this)
                val function = expression.symbol.owner
                if (function.isInlineFunctionCall(context)) {
                    for (parameter in function.valueParameters) {
                        if (parameter.isInlineParameter()) {
                            expression.getValueArgument(parameter.index)
                                ?.unwrapInlineLambda()
                                ?.let { inlineLambdaToParameter[it] = parameter }
                        }
                    }
                }
            }
        })
    }
}

// TODO: There is a Kotlin command line option to disable inlining (-Xno-inline). The code
//       should check for this option.
private fun IrFunction.isInlineFunctionCall(context: IrPluginContext) =
    isInline || isInlineArrayConstructor(context)

// Constructors can't be marked as inline in metadata, hence this hack.
private fun IrFunction.isInlineArrayConstructor(context: IrPluginContext): Boolean =
    this is IrConstructor && valueParameters.size == 2 && constructedClass.symbol.let {
        it == context.irBuiltIns.arrayClass ||
            it in context.irBuiltIns.primitiveArraysToPrimitiveTypes
    }

private fun IrExpression.unwrapInlineLambda(): IrFunctionSymbol? = when {
    this is IrBlock && origin.isInlinable ->
        (statements.lastOrNull() as? IrFunctionReference)?.symbol

    this is IrFunctionExpression ->
        function.symbol

    else ->
        null
}

private val IrStatementOrigin?.isInlinable: Boolean
    get() = isLambda || this == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE ||
        this == IrStatementOrigin.SUSPEND_CONVERSION
