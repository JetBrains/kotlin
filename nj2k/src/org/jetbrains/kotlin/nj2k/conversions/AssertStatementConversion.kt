/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.kotlinAssert
import org.jetbrains.kotlin.nj2k.tree.JKJavaAssertStatement
import org.jetbrains.kotlin.nj2k.tree.JKStubExpression
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement
import org.jetbrains.kotlin.nj2k.tree.detached
import org.jetbrains.kotlin.nj2k.tree.impl.JKExpressionStatementImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKLambdaExpressionImpl


class AssertStatementConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
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