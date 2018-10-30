/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import com.intellij.psi.JavaTokenType
import org.jetbrains.kotlin.j2k.copyTreeAndDetach
import org.jetbrains.kotlin.j2k.tree.JKKtAssignmentStatement
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.detached
import org.jetbrains.kotlin.j2k.tree.impl.*

class AssignmentStatementOperatorConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKKtAssignmentStatement) return recurse(element)
        val operator = element.operator as? JKJavaOperatorImpl ?: return recurse(element)
        operator.token.correnspondingBinaryOperation()
            ?.apply {
                val expression = element.expression.copyTreeAndDetach()
                element.expression =
                        JKBinaryExpressionImpl(
                            element.field.copyTreeAndDetach(),
                            expression,
                            this
                        )
                element.operator = JKJavaOperatorImpl.tokenToOperator[JavaTokenType.EQ]!!
            }
        return recurse(element)
    }

    private fun JKJavaOperatorToken.correnspondingBinaryOperation() =
        when (psiToken) {
            JavaTokenType.OREQ -> JavaTokenType.OR
            JavaTokenType.ANDEQ -> JavaTokenType.AND
            JavaTokenType.LTLTEQ -> JavaTokenType.LTLT
            JavaTokenType.GTGTEQ -> JavaTokenType.GTGT
            JavaTokenType.GTGTGTEQ -> JavaTokenType.GTGTGT
            JavaTokenType.XOREQ -> JavaTokenType.XOR
            else -> null
        }?.let { JKJavaOperatorImpl.tokenToOperator[it] }
}