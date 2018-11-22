/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKExpressionStatementImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKJavaOperatorImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKLambdaExpressionImpl
import org.jetbrains.kotlin.j2k.tree.impl.toKtToken


class AssertStatementConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaAssertStatement) return recurse(element)
        val messageExpression =
            if (element.description is JKStubExpression) null
            else JKLambdaExpressionImpl(
                emptyList(),
                JKExpressionStatementImpl(element::description.detached())
            )
        return recurse(
            JKExpressionStatementImpl(
                kotlinAssert(
                    element::condition.detached(),
                    messageExpression,
                    context.symbolProvider
                )
            )
        )
    }
}