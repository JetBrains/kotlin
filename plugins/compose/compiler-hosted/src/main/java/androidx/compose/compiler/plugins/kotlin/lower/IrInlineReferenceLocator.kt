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

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isLambda
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

internal open class IrInlineReferenceLocator(private val context: IrPluginContext) :
    IrElementVisitor<Unit, IrDeclaration?> {
    override fun visitElement(element: IrElement, data: IrDeclaration?) {
        element.acceptChildren(this, data)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration?) {
        val scope = if (declaration is IrVariable) data else declaration
        declaration.acceptChildren(this, scope)
    }

    override fun visitValueParameter(declaration: IrValueParameter, data: IrDeclaration?) {
        val parent = declaration.parent as? IrFunction
        if (parent?.isInline == true && declaration.isInlineParameter()) {
            val defaultValue = declaration.defaultValue
            if (defaultValue != null) {
                if (defaultValue.expression.isInlineIrExpression()) {
                    visitInlineable(
                        defaultValue.expression,
                        declaration.parent as IrFunction,
                        declaration,
                        data!!
                    )
                }
            }
        }
        super.visitValueParameter(declaration, data)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrDeclaration?) {
        val function = expression.symbol.owner
        if (function.isInlineFunctionCall(context)) {
            for (parameter in function.valueParameters) {
                if (!parameter.isInlineParameter())
                    continue

                val valueArgument = expression.getValueArgument(parameter.index) ?: continue
                if (!valueArgument.isInlineIrExpression())
                    continue

                visitInlineable(valueArgument, function, parameter, data!!)
            }
        }
        return super.visitFunctionAccess(expression, data)
    }

    private fun visitInlineable(
        value: IrExpression,
        callee: IrFunction,
        parameter: IrValueParameter,
        scope: IrDeclaration
    ) {
        if (value is IrBlock) {
            visitInlineLambda(
                value.statements.last() as IrFunctionReference,
                callee,
                parameter,
                scope
            )
        } else if (value is IrFunctionExpression) {
            visitInlineLambda(value, callee, parameter, scope)
        } else if (value is IrCallableReference<*>) {
            visitInlineReference(value)
        }
    }

    open fun visitInlineReference(argument: IrCallableReference<*>) {}

    open fun visitInlineLambda(
        argument: IrFunctionReference,
        callee: IrFunction,
        parameter: IrValueParameter,
        scope: IrDeclaration
    ) = visitInlineReference(argument)

    open fun visitInlineLambda(
        argument: IrFunctionExpression,
        callee: IrFunction,
        parameter: IrValueParameter,
        scope: IrDeclaration
    ) {}

    companion object {
        fun scan(context: IrPluginContext, element: IrElement): Set<InlineLambdaInfo> =
            mutableSetOf<InlineLambdaInfo>().apply {
                element.accept(
                    object : IrInlineReferenceLocator(context) {
                        override fun visitInlineLambda(
                            argument: IrFunctionExpression,
                            callee: IrFunction,
                            parameter: IrValueParameter,
                            scope: IrDeclaration
                        ) {
                            add(InlineLambdaInfo(argument, callee, parameter, scope))
                        }
                    },
                    null
                )
            }
    }
}

data class InlineLambdaInfo(
    val argument: IrFunctionExpression,
    val callee: IrFunction,
    val parameter: IrValueParameter,
    val scope: IrDeclaration
)

@Suppress("UNUSED_PARAMETER")
fun IrFunction.isInlineFunctionCall(context: IrPluginContext) =
    (/*!context.state.isInlineDisabled */ true || typeParameters.any { it.isReified }) && isInline

fun IrExpression.isInlineIrExpression() =
    when (this) {
        is IrBlock -> origin.isInlineIrExpression()
        is IrCallableReference<*> -> true.also {
            assert((0 until valueArgumentsCount).count { getValueArgument(it) != null } == 0) {
                "Expecting 0 value arguments for bounded callable reference: ${dump()}"
            }
        }
        is IrFunctionExpression -> origin.isInlineIrExpression()
        else -> false
    }

fun IrStatementOrigin?.isInlineIrExpression(): Boolean {
    if (isLambda) return true
    if (this == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE) return true
    if (this == IrStatementOrigin.SUSPEND_CONVERSION) return true
    return false
}
