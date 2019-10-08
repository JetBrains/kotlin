/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.kotlinAssert
import org.jetbrains.kotlin.nj2k.tree.*


class AssertStatementConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaAssertStatement) return recurse(element)
        val messageExpression =
            if (element.description is JKStubExpression) null
            else JKLambdaExpression(
                JKExpressionStatement(element::description.detached()),
                emptyList()
            )
        return recurse(
            JKExpressionStatement(
                kotlinAssert(
                    element::condition.detached(),
                    messageExpression,
                    typeFactory
                )
            )
        )
    }
}