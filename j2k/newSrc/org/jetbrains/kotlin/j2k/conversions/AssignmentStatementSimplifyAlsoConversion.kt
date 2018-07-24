/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKExpressionStatementImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKStubExpressionImpl

class AssignmentStatementSimplifyAlsoConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKExpressionStatement) return recurse(element)
        val qualifiedExpression = element.expression as? JKQualifiedExpression ?: return recurse(element)
        val alsoCall = qualifiedExpression.selector as? JKKtAlsoCallExpression ?: return recurse(element)
        return recurse(if (alsoCall.statement !is JKBlockStatement) alsoCall.statement.also {
            alsoCall.statement = JKExpressionStatementImpl(JKStubExpressionImpl())
            inlineVal(it, qualifiedExpression.receiver.also { qualifiedExpression.receiver = JKStubExpressionImpl() })
        } else element)
    }

    private fun inlineVal(statement: JKStatement, expression: JKExpression) {
        if (statement is JKKtAssignmentStatement) {
            (statement.expression as? JKBinaryExpression)?.right = expression
            if (statement.expression !is JKBinaryExpression) statement.expression = expression
        }
    }
}