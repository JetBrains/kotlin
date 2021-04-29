/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineFunctionCall
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineIrExpression
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

internal open class IrInlineReferenceLocator(private val context: BackendContext) : IrElementVisitor<Unit, IrDeclaration?> {
    override fun visitElement(element: IrElement, data: IrDeclaration?) {
        element.acceptChildren(this, data)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration?) {
        val scope = if (declaration is IrVariable) data else declaration
        declaration.acceptChildren(this, scope)
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

                if (valueArgument is IrBlock) {
                    visitInlineLambda(valueArgument.statements.last() as IrFunctionReference, function, parameter, data!!)
                } else if (valueArgument is IrCallableReference<*>) {
                    visitInlineReference(valueArgument)
                }
            }
        }
        return super.visitFunctionAccess(expression, data)
    }

    open fun visitInlineReference(argument: IrCallableReference<*>) {}

    open fun visitInlineLambda(argument: IrFunctionReference, callee: IrFunction, parameter: IrValueParameter, scope: IrDeclaration) =
        visitInlineReference(argument)

    companion object {
        fun scan(context: JvmBackendContext, element: IrElement): Set<IrCallableReference<*>> =
            mutableSetOf<IrCallableReference<*>>().apply {
                element.accept(object : IrInlineReferenceLocator(context) {
                    override fun visitInlineReference(argument: IrCallableReference<*>) {
                        add(argument)
                    }
                }, null)
            }
    }
}
