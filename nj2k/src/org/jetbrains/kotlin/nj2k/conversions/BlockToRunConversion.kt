/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*

class BlockToRunConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKBlockStatement) return recurse(element)

        if (element.parent !is JKBlock) return recurse(element)

        val parentDeclaration = element.parentOfType<JKDeclaration>() ?: return recurse(element)
        val psiContext = parentDeclaration.psi ?: return recurse(element)


        element.invalidate()
        val lambda = JKLambdaExpressionImpl(
            JKBlockStatementImpl(element.block),
            emptyList()
        )
        val call = JKKtCallExpressionImpl(context.symbolProvider.provideByFqName("kotlin.run", true), JKArgumentListImpl(lambda))
        return recurse(JKExpressionStatementImpl(call).withNonCodeElementsFrom(element))
    }

}