/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.utils.addToStdlib.cast

class AssignmentAsExpressionToAlsoConversion(val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaAssignmentExpression) return recurse(element)

        val alsoExpression = JKKtAlsoCallExpressionImpl(
            JKBlockStatementImpl(
                JKBlockImpl(listOf(JKKtAssignmentStatementImpl(element.field, JKStubExpressionImpl(), element.operator)))
            ), context.symbolProvider.provideByFqName("kotlin/also", element.parentOfType<JKClass>()!!.psi!!)
        ).also {
            it.statement.cast<JKBlockStatement>().block.statements.first().cast<JKKtAssignmentStatement>().expression =
                    JKFieldAccessExpressionImpl(context.symbolProvider.provideUniverseSymbol(it.parameter))
        }
        element.invalidate()

        return recurse(
            JKQualifiedExpressionImpl(
                element.expression,
                JKKtQualifierImpl.DOT,
                alsoExpression
            )
        )
    }
}