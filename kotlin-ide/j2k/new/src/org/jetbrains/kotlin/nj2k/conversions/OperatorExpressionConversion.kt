/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.callOn
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.types.isStringType


class AnyWithStringConcatenationConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKBinaryExpression) return recurse(element)
        if (element.operator.token == JKOperatorToken.PLUS
            && element.right.calculateType(typeFactory)?.isStringType() == true
            && element.left.calculateType(typeFactory)?.isStringType() == false
        ) {
            return recurse(
                JKBinaryExpression(
                    element::left.detached().callOn(symbolProvider.provideMethodSymbol("kotlin.Any.toString")),
                    element::right.detached(),
                    element.operator
                )
            )
        }
        return recurse(element)
    }
}