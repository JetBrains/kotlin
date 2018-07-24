/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.copyTree
import org.jetbrains.kotlin.j2k.tree.JKKtAssignmentStatement
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.impl.JKBinaryExpressionImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKKtOperatorImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKKtWordOperatorImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKStubExpressionImpl
import org.jetbrains.kotlin.lexer.KtTokens

class AssignmentStatementOperatorConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKKtAssignmentStatement) return recurse(element)
        val newOperator = JKKtOperatorImpl.javaToKotlinOperator[element.operator] ?: return recurse(element)
        if (newOperator is JKKtWordOperatorImpl) {
            element.operator = JKKtOperatorImpl.tokenToOperator[KtTokens.EQ] ?: return recurse(element)
            val expr = element.expression
            element.expression = JKStubExpressionImpl()
            element.expression = JKBinaryExpressionImpl(element.field.copyTree(), expr, newOperator)
        } else {
            element.operator = newOperator
        }
        return recurse(element)
    }
}