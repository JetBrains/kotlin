/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.tree.JKJavaSynchronizedStatement
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.nj2k.tree.withNonCodeElementsFrom


class SynchronizedStatementConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaSynchronizedStatement) return recurse(element)
        element.invalidate()
        val lambdaBody = JKLambdaExpressionImpl(
            JKBlockStatementImpl(element.body),
            emptyList()
        )
        val synchronizedCall =
            JKKtCallExpressionImpl(
                context.symbolProvider.provideByFqNameMulti("kotlin.synchronized"),
                JKArgumentListImpl(
                    element.lockExpression,
                    lambdaBody
                )
            ).withNonCodeElementsFrom(element)
        return recurse(JKExpressionStatementImpl(synchronizedCall))
    }

}