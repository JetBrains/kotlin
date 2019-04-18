/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKBinaryExpressionImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKParenthesizedExpressionImpl


class AddParenthesisForLineBreaksInBinaryExpression : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKBinaryExpression) return recurse(element)
        if (element.parent is JKBinaryExpression) return recurse(element)
        if (element.left.rightNonCodeElements.any {
                it is JKSpaceElement && '\n' in it.text
            }) {
            return JKParenthesizedExpressionImpl(
                JKBinaryExpressionImpl(
                    element::left.detached(),
                    element::right.detached(),
                    element.operator
                ).withNonCodeElementsFrom(element)
            )
        }
        return recurse(element)
    }

}