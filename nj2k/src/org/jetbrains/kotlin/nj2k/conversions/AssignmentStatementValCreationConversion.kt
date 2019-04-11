/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.ConversionContext
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*

class AssignmentStatementValCreationConversion(val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKKtAlsoCallExpression) return recurse(element)
        val codeBlock = (element.statement as? JKBlockStatement)?.block ?: return recurse(element)
        val assignableExpr = (codeBlock.statements.first() as? JKKtAssignmentStatement)?.field ?: return recurse(element)
        when (assignableExpr) {
            is JKArrayAccessExpression -> {
                val ex1 = assignableExpr.expression
                val ex2 = assignableExpr.indexExpression
                assignableExpr.expression = JKStubExpressionImpl()
                assignableExpr.indexExpression = JKStubExpressionImpl()
                codeBlock.statements = listOf(
                    JKDeclarationStatementImpl(
                        listOf(
                            JKLocalVariableImpl(
                                JKTypeElementImpl(JKJavaVoidType/*TODO*/),
                                JKNameIdentifierImpl("arr"),
                                ex1,
                                JKMutabilityModifierElementImpl(Mutability.IMMUTABLE)
                            ).also {
                                assignableExpr.expression = JKFieldAccessExpressionImpl(context.symbolProvider.provideUniverseSymbol(it))
                            }, JKLocalVariableImpl(
                                JKTypeElementImpl(JKJavaVoidType/*TODO*/),
                                JKNameIdentifierImpl("i"),
                                ex2,
                                JKMutabilityModifierElementImpl(Mutability.UNKNOWN)
                            ).also {
                                assignableExpr.indexExpression =
                                        JKFieldAccessExpressionImpl(context.symbolProvider.provideUniverseSymbol(it))
                            }
                        )
                    )
                ) + codeBlock.statements
            }
            is JKQualifiedExpression -> {
                val ex = assignableExpr.receiver
                assignableExpr.receiver = JKStubExpressionImpl()
                codeBlock.statements = listOf(
                    JKDeclarationStatementImpl(
                        listOf(
                            JKLocalVariableImpl(
                                JKTypeElementImpl(JKJavaVoidType/*TODO*/),
                                JKNameIdentifierImpl("arg"),
                                ex,
                                JKMutabilityModifierElementImpl(Mutability.UNKNOWN)
                            ).also {
                                assignableExpr.receiver = JKFieldAccessExpressionImpl(context.symbolProvider.provideUniverseSymbol(it))
                            }
                        )
                    )
                ) + codeBlock.statements
            }
        }
        return recurse(element)
    }
}