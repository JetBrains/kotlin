/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.equalsExpression
import org.jetbrains.kotlin.nj2k.symbols.deepestFqName
import org.jetbrains.kotlin.nj2k.tree.*


class EqualsOperatorConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKQualifiedExpression) return recurse(element)
        if (element.receiver is JKSuperExpression) return recurse(element)
        val selector = element.selector as? JKCallExpression ?: return (element)
        val argument = selector.arguments.arguments.singleOrNull() ?: return recurse(element)
        if (selector.identifier.deepestFqName() == "java.lang.Object.equals") {
            return recurse(
                JKParenthesizedExpression(
                    equalsExpression(
                        element::receiver.detached(),
                        argument::value.detached(),
                        typeFactory
                    )
                )
            )
        }
        return recurse(element)
    }
}