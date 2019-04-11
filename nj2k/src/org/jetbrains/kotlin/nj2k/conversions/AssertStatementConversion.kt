/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKExpressionStatementImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKJavaOperatorImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKLambdaExpressionImpl
import org.jetbrains.kotlin.nj2k.tree.impl.toKtToken


class AssertStatementConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaAssertStatement) return recurse(element)
        val messageExpression =
            if (element.description is JKStubExpression) null
            else JKLambdaExpressionImpl(
                JKExpressionStatementImpl(element::description.detached()),
                emptyList()
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